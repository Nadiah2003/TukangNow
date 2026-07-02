const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let currentUnreadCount = 0;
let forceProfileSetup = false;
let popupMode = "info";
let popupConfirmCallback = null;
let dashboardPollingTimer = null;
let emergencyVibrationTimer = null;
let emergencyVibrationActive = false;
let profileSetupPopupShown = false;
let allVendorNotifications = [];

document.addEventListener("DOMContentLoaded", function() {
    loadDashboardData();

    dashboardPollingTimer = setInterval(function() {
        loadDashboardData();
    }, 8000);

    document.getElementById("bellIconBtn").addEventListener("click", function() {
        if (forceProfileSetup) {
            openProfileSetupPopup();
            return;
        }

        openNotificationModal();
    });

    document.getElementById("popupCloseBtn").addEventListener("click", function() {
        handlePopupPrimaryButton();
    });

    document.getElementById("closeNotificationBtn").addEventListener("click", function() {
        closeNotificationModal();
    });
});

function openPopup(title, message, type = "info") {
    popupMode = type;
    popupConfirmCallback = null;

    const modal = document.getElementById("popupModal");
    const card = modal.querySelector(".popup-card");

    document.getElementById("popupTitle").innerText = title;
    document.getElementById("popupMessage").innerText = message;

    card.classList.remove("popup-card--success", "popup-card--error", "popup-card--confirm", "popup-card--info");
    card.classList.add(`popup-card--${type}`);

    setPopupIcon(type);
    preparePopupButtons(type);

    modal.style.display = "flex";
}

function openHtmlPopup(title, html, type = "info") {
    popupMode = type;
    popupConfirmCallback = null;

    const modal = document.getElementById("popupModal");
    const card = modal.querySelector(".popup-card");

    document.getElementById("popupTitle").innerText = title;
    document.getElementById("popupMessage").innerHTML = html;

    card.classList.remove("popup-card--success", "popup-card--error", "popup-card--confirm", "popup-card--info");
    card.classList.add(`popup-card--${type}`);

    setPopupIcon(type);
    preparePopupButtons(type);

    modal.style.display = "flex";
}

function openConfirmPopup(title, message, confirmCallback) {
    popupMode = "confirm";
    popupConfirmCallback = typeof confirmCallback === "function" ? confirmCallback : null;

    const modal = document.getElementById("popupModal");
    const card = modal.querySelector(".popup-card");

    document.getElementById("popupTitle").innerText = title;
    document.getElementById("popupMessage").innerText = message;

    card.classList.remove("popup-card--success", "popup-card--error", "popup-card--info");
    card.classList.add("popup-card--confirm");

    setPopupIcon("confirm");
    preparePopupButtons("confirm");

    modal.style.display = "flex";
}

function setPopupIcon(type) {
    const iconCircle = document.getElementById("popupIconCircle");

    iconCircle.className = "popup-icon-circle";
    iconCircle.innerHTML = "";

    if (type === "success") {
        iconCircle.classList.add("success");
        iconCircle.innerHTML = `
            <svg class="popup-svg-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
        `;
        return;
    }

    if (type === "error") {
        iconCircle.classList.add("error");
        iconCircle.innerHTML = `<span class="popup-x-icon">×</span>`;
        return;
    }

    iconCircle.classList.add("hidden");
}

function preparePopupButtons(type) {
    const popupCloseBtn = document.getElementById("popupCloseBtn");
    const oldCancelBtn = document.getElementById("popupCancelBtn");

    if (oldCancelBtn) {
        oldCancelBtn.remove();
    }

    popupCloseBtn.className = "popup-btn";
    popupCloseBtn.innerText = "Continue";

    if (type === "confirm") {
        popupCloseBtn.innerText = "OK";
        popupCloseBtn.classList.add("popup-btn--half");

        const cancelBtn = document.createElement("button");
        cancelBtn.id = "popupCancelBtn";
        cancelBtn.type = "button";
        cancelBtn.className = "popup-btn-cancel";
        cancelBtn.innerText = "Cancel";
        cancelBtn.addEventListener("click", function() {
            closePopupOnly();
        });

        popupCloseBtn.insertAdjacentElement("afterend", cancelBtn);
    }
}

function handlePopupPrimaryButton() {
    if (popupMode === "confirm") {
        const callback = popupConfirmCallback;
        closePopupOnly();

        if (typeof callback === "function") {
            callback();
        }

        return;
    }

    closePopup();
}

function openProfileSetupPopup() {
    forceProfileSetup = true;

    popupMode = "info";
    popupConfirmCallback = null;

    const modal = document.getElementById("popupModal");
    const card = modal.querySelector(".popup-card");

    document.getElementById("popupTitle").innerText = "Welcome Vendor!";
    document.getElementById("popupMessage").innerText = "Please completely update your subservice, starting price, available date and available time in Profile & Service setup to make your business live to customers.";

    card.classList.remove("popup-card--success", "popup-card--error", "popup-card--confirm");
    card.classList.add("popup-card--info");

    setPopupIcon("info");
    preparePopupButtons("info");

    modal.style.display = "flex";
}

function closePopup() {
    if (forceProfileSetup) {
        window.location.href = "profilevendor.html";
        return;
    }

    closePopupOnly();
}

function closePopupOnly() {
    popupMode = "info";
    popupConfirmCallback = null;

    const oldCancelBtn = document.getElementById("popupCancelBtn");

    if (oldCancelBtn) {
        oldCancelBtn.remove();
    }

    document.getElementById("popupCloseBtn").className = "popup-btn";
    document.getElementById("popupCloseBtn").innerText = "Continue";
    document.getElementById("popupModal").style.display = "none";
}

function openNotificationModal() {
    document.getElementById("notificationModal").style.display = "flex";
    renderNotificationList(allVendorNotifications);
}

function closeNotificationModal() {
    document.getElementById("notificationModal").style.display = "none";
}

function loadDashboardData() {
    fetch(`${baseURL}/HomeVendorServlet?t=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(function(res) {
        if (res.status === 401) {
            stopEmergencyVibration();
            openPopup("Session Debug", "HomeVendorServlet returned 401 Unauthorized. Session/cookie is missing, but redirect is disabled for debugging.", "error");
            return null;
        }

        if (!res.ok) {
            throw new Error("Server returned error " + res.status);
        }

        return res.json();
    })
    .then(function(data) {
        if (!data) {
            return;
        }

        if (data.status === "session_expired") {
            stopEmergencyVibration();
            openPopup("Session Debug", data.message || "Session expired. Redirect is disabled for debugging.", "error");
            return;
        }

        if (data.status === "error") {
            stopEmergencyVibration();
            openPopup("Dashboard Error", data.message || "Dashboard returned error. Redirect is disabled for debugging.", "error");
            return;
        }

        if (data.vendor) {
            document.getElementById("vendorName").innerText = data.vendor.name || "Vendor";
            document.getElementById("vendorIncome").innerText = "RM " + formatMoney(data.vendor.walletBalance || "0.00");
            document.getElementById("vendorJobsCount").innerText = data.vendor.totalFinish || "0";

            const vendorPic = document.getElementById("vendorPic");

            if (data.vendor.profileImage && data.vendor.profileImage.trim() !== "") {
                vendorPic.src = resolveProfilePath(data.vendor.profileImage);
            } else {
                vendorPic.src = "image/profile.png";
            }

            vendorPic.onerror = function() {
                vendorPic.src = "image/profile.png";
            };

            const statusEl = document.getElementById("accountStatus");
            const statusStr = (data.vendor.accountStatus || "pending").toLowerCase();

            statusEl.innerText = statusStr;
            statusEl.className = "status-badge";

            if (statusStr === "active") {
                statusEl.classList.add("status-badge--active");
            } else if (statusStr === "expired") {
                statusEl.classList.add("status-badge--expired");
            } else {
                statusEl.classList.add("status-badge--pending");
            }

            if (data.vendor.expiryInfo) {
                document.getElementById("expiryInfo").innerText = data.vendor.expiryInfo;
            }

            if (Number(data.vendor.isFirstLogin) === 1) {
                if (!profileSetupPopupShown) {
                    profileSetupPopupShown = true;
                    openProfileSetupPopup();
                }
            } else {
                forceProfileSetup = false;
            }
        }

        currentUnreadCount = Number(data.unreadNotificationsCount || 0);
        allVendorNotifications = data.notifications || [];

        updateNotificationBadge(currentUnreadCount);
        renderNotificationList(allVendorNotifications);

        const urgentJobs = data.urgentJobs || [];
        const pendingJobs = data.pendingJobs || [];
        const activeJobs = data.activeJobs || [];

        renderUrgentJobs(urgentJobs);
        renderPendingJobs(pendingJobs);
        renderActiveJobs(activeJobs);
        syncEmergencyVibration(urgentJobs);
    })
    .catch(function(error) {
        stopEmergencyVibration();
        openPopup("Dashboard Error", error.message || "Error synchronization with dashboard server.", "error");
    });
}

function updateNotificationBadge(count) {
    const badge = document.getElementById("notiBadge");

    if (count > 0) {
        badge.innerText = count > 99 ? "99+" : count;
        badge.style.display = "flex";
    } else {
        badge.style.display = "none";
    }
}

function renderNotificationList(notifications) {
    const list = document.getElementById("notificationList");

    if (!list) {
        return;
    }

    if (!notifications || notifications.length === 0) {
        list.innerHTML = `<div class="notification-empty">No new notifications.</div>`;
        return;
    }

    const visibleNotifications = notifications.filter(function(item) {
        return Number(item.count || 0) > 0;
    });

    if (visibleNotifications.length === 0) {
        list.innerHTML = `<div class="notification-empty">No new notifications.</div>`;
        return;
    }

    list.innerHTML = visibleNotifications.map(function(item) {
        const type = String(item.type || "");
        const emergencyClass = type === "emergency_booking" ? "emergency" : "";

        return `
            <div class="notification-item ${emergencyClass}" onclick="handleNotificationClick('${escapeJs(item.link || "")}')">
                <div class="notification-icon">${escapeHtml(item.icon || "🔔")}</div>
                <div class="notification-content">
                    <div class="notification-title-row">
                        <p class="notification-title">${escapeHtml(item.title || "Notification")}</p>
                        <span class="notification-count">${Number(item.count || 0)}</span>
                    </div>
                    <p class="notification-message">${escapeHtml(item.message || "-")}</p>
                </div>
            </div>
        `;
    }).join("");
}

function handleNotificationClick(link) {
    const cleanLink = String(link || "").trim();

    if (cleanLink === "") {
        return;
    }

    window.location.href = cleanLink;
}

function syncEmergencyVibration(urgentJobs) {
    const hasEmergency = Array.isArray(urgentJobs) && urgentJobs.length > 0;

    if (hasEmergency) {
        startEmergencyVibration();
    } else {
        stopEmergencyVibration();
    }
}

function startEmergencyVibration() {
    if (emergencyVibrationActive) {
        return;
    }

    emergencyVibrationActive = true;

    if ("vibrate" in navigator) {
        navigator.vibrate([700, 250, 700]);
    }

    emergencyVibrationTimer = setInterval(function() {
        if ("vibrate" in navigator) {
            navigator.vibrate([700, 250, 700]);
        }
    }, 2500);
}

function stopEmergencyVibration() {
    emergencyVibrationActive = false;

    if (emergencyVibrationTimer) {
        clearInterval(emergencyVibrationTimer);
        emergencyVibrationTimer = null;
    }

    if ("vibrate" in navigator) {
        navigator.vibrate(0);
    }
}

function renderUrgentJobs(jobs) {
    const urgentContainer = document.getElementById("urgentJobsContainer");
    urgentContainer.innerHTML = "";

    if (!jobs || jobs.length === 0) {
        urgentContainer.innerHTML = '<div class="no-jobs">No urgent emergency requests.</div>';
        return;
    }

    jobs.forEach(function(job) {
        const problemText = job.problem && job.problem.trim() !== "" ? job.problem : "-";
        const subserviceText = job.subservice && job.subservice.trim() !== "" ? job.subservice : "-";
        const timeAgoText = job.timeAgo || "-";
        const custNameText = job.custName || "-";
        const distanceText = job.distanceKM || "-";

        const cardHTML = `
            <div class="job-card job-card--urgent">
                <div class="job-card__top">
                    <h4 class="job-card__title">${escapeHtml(job.title || "-")} (EMERGENCY)</h4>
                    <small class="job-card__meta">⏱️ ${escapeHtml(timeAgoText)}</small>
                </div>
                <p class="job-card__subservice">${escapeHtml(subserviceText)}</p>
                <div class="job-card__details">
                    <p><strong>Status:</strong> ${escapeHtml(job.status || "Emergency")}</p>
                    <p><strong>Customer:</strong> ${escapeHtml(custNameText)}</p>
                    <p><strong>Problem:</strong> ${escapeHtml(problemText)}</p>
                    <p><strong>Distance:</strong> ${escapeHtml(distanceText)} KM</p>
                    <p><strong>Deposit Paid:</strong> RM ${formatMoney(job.deposit || 0)}</p>
                </div>
                <div class="job-actions">
                    <button class="job-btn job-btn--respond" onclick="respondNow(${Number(job.bookingId)})">
                        Respond Now
                    </button>
                    <button class="job-btn job-btn--reject" onclick="rejectBooking(${Number(job.bookingId)}, 'emergency')">
                        Reject
                    </button>
                </div>
            </div>
        `;

        urgentContainer.innerHTML += cardHTML;
    });
}

function renderPendingJobs(jobs) {
    const pendingContainer = document.getElementById("pendingJobsContainer");
    pendingContainer.innerHTML = "";

    if (!jobs || jobs.length === 0) {
        pendingContainer.innerHTML = '<div class="no-jobs">No pending booking requests.</div>';
        return;
    }

    jobs.forEach(function(job) {
        const subserviceText = job.subservice && job.subservice.trim() !== "" ? job.subservice : "-";
        const problemText = job.problem && job.problem.trim() !== "" ? job.problem : "-";
        const scheduleText = job.fullSchedule || "-";
        const custNameText = job.custName || "-";

        const cardHTML = `
            <div class="job-card job-card--active">
                <div class="job-card__top">
                    <h4 class="job-card__title">${escapeHtml(job.title || "-")}</h4>
                    <small class="job-card__meta">📅 ${escapeHtml(scheduleText)}</small>
                </div>
                <p class="job-card__subservice">${escapeHtml(subserviceText)}</p>
                <div class="job-card__details">
                    <p><strong>Status:</strong> ${escapeHtml(job.status || "Pending")}</p>
                    <p><strong>Customer:</strong> ${escapeHtml(custNameText)}</p>
                    <p><strong>Problem:</strong> ${escapeHtml(problemText)}</p>
                    <p><strong>Deposit Paid:</strong> RM ${formatMoney(job.deposit || 0)}</p>
                </div>
                <div class="job-actions">
                    <button class="job-btn job-btn--respond" onclick="respondNow(${Number(job.bookingId)})">
                        Accept Booking
                    </button>
                    <button class="job-btn job-btn--reject" onclick="rejectBooking(${Number(job.bookingId)}, 'pending')">
                        Reject
                    </button>
                </div>
            </div>
        `;

        pendingContainer.innerHTML += cardHTML;
    });
}

function renderActiveJobs(jobs) {
    const activeContainer = document.getElementById("activeJobsContainer");
    activeContainer.innerHTML = "";

    if (!jobs || jobs.length === 0) {
        activeContainer.innerHTML = '<div class="no-jobs">No active accepted jobs.</div>';
        return;
    }

    jobs.forEach(function(job) {
        const subserviceText = job.subservice && job.subservice.trim() !== "" ? job.subservice : "-";
        const problemText = job.problem && job.problem.trim() !== "" ? job.problem : "-";
        const scheduleText = job.fullSchedule || "-";
        const custNameText = job.custName || "-";
        const statusText = job.status || "Accepted";
        const statusLower = String(statusText).toLowerCase().trim();
        const canTrack = isBookingDateReached(scheduleText);

        let buttonHTML = "";
        let cardClass = "job-card job-card--active";

        if (statusLower === "report") {
            cardClass = "job-card job-card--report";
            buttonHTML = `
                <button class="job-btn job-btn--report" onclick="openVendorReportStatus(${Number(job.bookingId)})">
                    View Report Status
                </button>
            `;
        } else if (canTrack) {
            buttonHTML = `
                <button class="job-btn job-btn--start" onclick="window.location.href='trackingvendor.html?id=${Number(job.bookingId)}'">
                    View Job Details & Map
                </button>
            `;
        } else {
            buttonHTML = `
                <button class="job-btn job-btn--start" type="button" disabled>
                    Tracking Available On Booking Date
                </button>
            `;
        }

        const reportNote = statusLower === "report"
            ? `<p><strong>Report:</strong> This booking is under admin review. Only the account that submitted the report can view report details.</p>`
            : "";

        const cardHTML = `
            <div class="${cardClass}">
                <div class="job-card__top">
                    <h4 class="job-card__title">${escapeHtml(job.title || "-")}</h4>
                    <small class="job-card__meta">📅 ${escapeHtml(scheduleText)}</small>
                </div>
                <p class="job-card__subservice">${escapeHtml(subserviceText)}</p>
                <div class="job-card__details">
                    <p><strong>Status:</strong> ${escapeHtml(statusText)}</p>
                    <p><strong>Customer:</strong> ${escapeHtml(custNameText)}</p>
                    <p><strong>Problem:</strong> ${escapeHtml(problemText)}</p>
                    <p><strong>Deposit Paid:</strong> RM ${formatMoney(job.deposit || 0)}</p>
                    ${reportNote}
                </div>
                ${buttonHTML}
            </div>
        `;

        activeContainer.innerHTML += cardHTML;
    });
}

function openVendorReportStatus(bookingId) {
    fetch(`${baseURL}/ReportServlet?action=bookingReport&bookingId=${encodeURIComponent(bookingId)}&t=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        headers: {
            "Accept": "application/json"
        }
    })
    .then(function(response) {
        return response.json();
    })
    .then(function(data) {
        if (data && data.status === "session_expired") {
            openPopup("Session Debug", data.message || "Session expired. Redirect is disabled for debugging.", "error");
            return;
        }

        if (data && data.status === "success" && data.report) {
            showVendorReportDetails(data.report);
            return;
        }

        if (data && data.status === "reported_account") {
            openPopup("Report Under Review", data.message || "This booking has been reported and is under admin review. You cannot take action on this booking.", "info");
            return;
        }

        openPopup("Report Under Review", data.message || "This booking is under admin review.", "info");
    })
    .catch(function() {
        openPopup("Failed!", "Network connection or server processing error.", "error");
    });
}

function showVendorReportDetails(report) {
    const html = `
        <div class="report-popup-details">
            <p><strong>Booking ID:</strong> #${Number(report.bookingId || 0)}</p>
            <p><strong>Report Type:</strong> ${escapeHtml(report.reportOption || "-")}</p>
            <p><strong>Report Status:</strong> ${escapeHtml(report.status || "-")}</p>
            <p><strong>Action Taken:</strong> ${escapeHtml(report.actionTaken || "-")}</p>
            <p><strong>Action Date:</strong> ${escapeHtml(report.actionDate || "-")}</p>
            <p><strong>Admin Note:</strong> ${escapeHtml(report.adminNote || "-")}</p>
            <p><strong>Explanation:</strong> ${escapeHtml(report.explanation || "-")}</p>
        </div>
    `;

    openHtmlPopup("Report Details", html, "info");
}

function respondNow(bookingId) {
    if (forceProfileSetup) {
        openProfileSetupPopup();
        return;
    }

    openConfirmPopup("Confirm Booking", "Are you sure you want to accept this booking request?", function() {
        acceptBookingRequest(bookingId);
    });
}

function rejectBooking(bookingId, bookingType) {
    if (forceProfileSetup) {
        openProfileSetupPopup();
        return;
    }

    let message = "Are you sure you want to reject this booking request?";

    if (bookingType === "emergency") {
        message = "Are you sure you want to reject this emergency request? It will be hidden from your dashboard.";
    }

    openConfirmPopup("Reject Booking", message, function() {
        rejectBookingRequest(bookingId);
    });
}

function acceptBookingRequest(bookingId) {
    fetch(`${baseURL}/HomeVendorServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            action: "respondNow",
            bookingId: bookingId
        })
    })
    .then(function(res) {
        if (res.status === 401) {
            stopEmergencyVibration();
            openPopup("Session Debug", "HomeVendorServlet returned 401 Unauthorized while processing booking. Redirect is disabled for debugging.", "error");
            return null;
        }

        return res.json();
    })
    .then(function(data) {
        if (!data) {
            return;
        }

        if (data.status === "session_expired") {
            stopEmergencyVibration();
            openPopup("Session Debug", data.message || "Session expired. Redirect is disabled for debugging.", "error");
            return;
        }

        if (data.status === "success") {
            stopEmergencyVibration();
            openPopup("Success!", data.message || "Booking successfully accepted!", "success");
            loadDashboardData();
        } else {
            openPopup("Failed!", data.message || "Booking already taken or unavailable.", "error");
            loadDashboardData();
        }
    })
    .catch(function() {
        openPopup("Failed!", "Network connection or server processing error.", "error");
    });
}

function rejectBookingRequest(bookingId) {
    fetch(`${baseURL}/HomeVendorServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            action: "rejectBooking",
            bookingId: bookingId
        })
    })
    .then(function(res) {
        if (res.status === 401) {
            stopEmergencyVibration();
            openPopup("Session Debug", "HomeVendorServlet returned 401 Unauthorized while processing booking. Redirect is disabled for debugging.", "error");
            return null;
        }

        return res.json();
    })
    .then(function(data) {
        if (!data) {
            return;
        }

        if (data.status === "session_expired") {
            stopEmergencyVibration();
            openPopup("Session Debug", data.message || "Session expired. Redirect is disabled for debugging.", "error");
            return;
        }

        if (data.status === "success") {
            stopEmergencyVibration();
            openPopup("Success!", data.message || "Booking successfully rejected.", "success");
            loadDashboardData();
        } else {
            openPopup("Failed!", data.message || "Booking already updated or unavailable.", "error");
            loadDashboardData();
        }
    })
    .catch(function() {
        openPopup("Failed!", "Network connection or server processing error.", "error");
    });
}

function isBookingDateReached(scheduleValue) {
    const bookingTime = parseDatabaseDateTime(scheduleValue);

    if (bookingTime <= 0) {
        return false;
    }

    return Date.now() >= bookingTime;
}

function parseDatabaseDateTime(value) {
    const clean = String(value || "").trim();

    if (clean === "" || clean === "-") {
        return 0;
    }

    const match = clean.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?/);

    if (!match) {
        const fallback = new Date(clean.replace(" ", "T")).getTime();
        return isNaN(fallback) ? 0 : fallback;
    }

    const year = Number(match[1]);
    const month = Number(match[2]) - 1;
    const day = Number(match[3]);
    const hour = Number(match[4]);
    const minute = Number(match[5]);
    const second = Number(match[6] || 0);

    return new Date(year, month, day, hour, minute, second).getTime();
}

function resolveProfilePath(path) {
    if (!path || path === "null" || path.trim() === "") {
        return "image/profile.png";
    }

    const cleanPath = path.trim();

    if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
        return cleanPath;
    }

    if (cleanPath.startsWith("image/")) {
        return cleanPath;
    }

    return `${baseURL}/HomeVendorServlet?action=profileImage&file=${encodeURIComponent(getOnlyFileName(cleanPath))}&t=${Date.now()}`;
}

function getOnlyFileName(path) {
    return String(path)
        .replaceAll("\\", "/")
        .split("/")
        .pop();
}

function formatMoney(value) {
    const amount = parseFloat(value);

    if (isNaN(amount)) {
        return "0.00";
    }

    return amount.toFixed(2);
}

function escapeHtml(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeJs(value) {
    return String(value || "")
        .replaceAll("\\", "\\\\")
        .replaceAll("'", "\\'")
        .replaceAll('"', '\\"')
        .replaceAll("\n", " ")
        .replaceAll("\r", " ");
}