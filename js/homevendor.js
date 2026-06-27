const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

let currentUnreadCount = 0;
let forceProfileSetup = false;
let popupMode = "info";
let popupConfirmCallback = null;

document.addEventListener("DOMContentLoaded", function() {
    loadDashboardData();

    document.getElementById("bellIconBtn").addEventListener("click", function() {
        if (forceProfileSetup) {
            openProfileSetupPopup();
            return;
        }

        if (currentUnreadCount > 0) {
            openPopup("Notifications", `You have ${currentUnreadCount} new unread notifications. Please check your notification log page.`, "info");
        } else {
            openPopup("Notifications", "You have no new notifications at this moment.", "info");
        }
    });

    document.getElementById("popupCloseBtn").addEventListener("click", function() {
        handlePopupPrimaryButton();
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

function loadDashboardData() {
    fetch(`${baseURL}/HomeVendorServlet?t=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin"
    })
    .then(function(res) {
        if (res.status === 401) {
            window.location.href = "login.html";
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
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data.status === "error") {
            window.location.href = "login.html";
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
                openProfileSetupPopup();
            } else {
                forceProfileSetup = false;
            }
        }

        currentUnreadCount = data.unreadNotificationsCount || 0;

        const badge = document.getElementById("notiBadge");

        if (currentUnreadCount > 0) {
            badge.innerText = currentUnreadCount;
            badge.style.display = "inline-block";
        } else {
            badge.style.display = "none";
        }

        renderUrgentJobs(data.urgentJobs || []);
        renderPendingJobs(data.pendingJobs || []);
        renderActiveJobs(data.activeJobs || []);
    })
    .catch(function() {
        console.log("Error synchronization with dashboard server.");
    });
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
        const canTrack = isBookingDateReached(scheduleText);

        let buttonHTML = "";

        if (canTrack) {
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

        const cardHTML = `
            <div class="job-card job-card--active">
                <div class="job-card__top">
                    <h4 class="job-card__title">${escapeHtml(job.title || "-")}</h4>
                    <small class="job-card__meta">📅 ${escapeHtml(scheduleText)}</small>
                </div>
                <p class="job-card__subservice">${escapeHtml(subserviceText)}</p>
                <div class="job-card__details">
                    <p><strong>Status:</strong> ${escapeHtml(job.status || "Accepted")}</p>
                    <p><strong>Customer:</strong> ${escapeHtml(custNameText)}</p>
                    <p><strong>Problem:</strong> ${escapeHtml(problemText)}</p>
                    <p><strong>Deposit Paid:</strong> RM ${formatMoney(job.deposit || 0)}</p>
                </div>
                ${buttonHTML}
            </div>
        `;

        activeContainer.innerHTML += cardHTML;
    });
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
            window.location.href = "login.html";
            return null;
        }

        return res.json();
    })
    .then(function(data) {
        if (!data) {
            return;
        }

        if (data.status === "session_expired") {
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data.status === "success") {
            openPopup("Success!", data.message || "Booking successfully accepted!", "success");
            loadDashboardData();
        } else {
            openPopup("Failed!", data.message || "Booking already taken or unavailable.", "error");
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
            window.location.href = "login.html";
            return null;
        }

        return res.json();
    })
    .then(function(data) {
        if (!data) {
            return;
        }

        if (data.status === "session_expired") {
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data.status === "success") {
            openPopup("Success!", data.message || "Booking successfully rejected.", "success");
            loadDashboardData();
        } else {
            openPopup("Failed!", data.message || "Booking already updated or unavailable.", "error");
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

    return `${baseURL}/profiles/${getOnlyFileName(cleanPath)}?t=${Date.now()}`;
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
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}