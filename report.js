const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let activeReportId = 0;
let isAdminPage = false;
let pageBookingId = 0;

document.addEventListener("DOMContentLoaded", function() {
    const params = new URLSearchParams(window.location.search);

    pageBookingId = getBookingIdFromParams(params);

    setupEvents();
    fillFormFromUrl();

    checkReportAccess();
});

function getBookingIdFromParams(params) {
    const id = Number(params.get("id") || params.get("bookingId") || params.get("booking_id") || 0);

    if (isNaN(id)) {
        return 0;
    }

    return id;
}

function checkReportAccess() {
    fetch(`${baseURL}/ReportServlet?action=session&t=${Date.now()}`, {
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
            window.location.href = data.redirect || "login.html";
            return;
        }

        const role = String(data.role || data.userRole || data.accountType || data.userType || data.loginType || "").toLowerCase().trim();
        isAdminPage = (data && data.isAdmin === true) || role === "admin";

        setupPageMode(isAdminPage);

        if (isAdminPage) {
            loadReports();
            return;
        }

        if (pageBookingId <= 0) {
            disableReportForm("Booking ID not found. Please open report from tracking page.");
            return;
        }

        checkExistingBookingReport();
    })
    .catch(function() {
        isAdminPage = false;
        setupPageMode(false);

        if (pageBookingId <= 0) {
            disableReportForm("Booking ID not found. Please open report from tracking page.");
            return;
        }

        checkExistingBookingReport();
    });
}

function setupPageMode(adminMode) {
    const pageTitle = document.getElementById("pageTitle");
    const pageSubtitle = document.getElementById("pageSubtitle");
    const submitSection = document.getElementById("submitSection");
    const adminSection = document.getElementById("adminSection");
    const reportMain = document.getElementById("reportMain");

    if (adminMode) {
        document.body.classList.add("admin-mode");
        document.body.classList.remove("user-mode");

        if (submitSection) {
            submitSection.style.display = "none";
        }

        if (adminSection) {
            adminSection.style.display = "block";
        }

        if (reportMain) {
            reportMain.style.justifyItems = "stretch";
        }

        if (pageTitle) {
            pageTitle.innerText = "Admin Reports";
        }

        if (pageSubtitle) {
            pageSubtitle.innerText = "Review submitted reports and take action.";
        }
    } else {
        document.body.classList.add("user-mode");
        document.body.classList.remove("admin-mode");

        if (submitSection) {
            submitSection.style.display = "block";
        }

        if (adminSection) {
            adminSection.style.display = "none";
        }

        if (reportMain) {
            reportMain.style.justifyItems = "center";
        }

        if (pageTitle) {
            pageTitle.innerText = "Submit Report";
        }

        if (pageSubtitle) {
            pageSubtitle.innerText = "Tell admin what happened so we can review it.";
        }
    }
}

function setupEvents() {
    const btnBack = document.getElementById("btnBack");
    const reportForm = document.getElementById("reportForm");
    const explanation = document.getElementById("explanation");
    const statusOkBtn = document.getElementById("statusOkBtn");
    const btnRefreshReports = document.getElementById("btnRefreshReports");
    const detailCloseBtn = document.getElementById("detailCloseBtn");

    if (btnBack) {
        btnBack.addEventListener("click", function() {
            if (isAdminPage) {
                window.location.href = "homeadmin.html";
                return;
            }

            window.location.href = "myorder.html";
        });
    }

    if (explanation) {
        explanation.addEventListener("input", function() {
            document.getElementById("charCount").innerText = `${explanation.value.length} / 1000`;
        });
    }

    if (reportForm) {
        reportForm.addEventListener("submit", function(event) {
            event.preventDefault();
            submitReport();
        });
    }

    if (statusOkBtn) {
        statusOkBtn.addEventListener("click", function() {
            document.getElementById("statusModal").style.display = "none";
        });
    }

    if (btnRefreshReports) {
        btnRefreshReports.addEventListener("click", function() {
            if (isAdminPage) {
                loadReports();
            }
        });
    }

    if (detailCloseBtn) {
        detailCloseBtn.addEventListener("click", function() {
            closeDetailModal();
        });
    }
}

function fillFormFromUrl() {
    const bookingIdInput = document.getElementById("bookingId");
    const bookingContextText = document.getElementById("bookingContextText");

    if (bookingIdInput) {
        bookingIdInput.value = pageBookingId > 0 ? pageBookingId : "";
    }

    if (bookingContextText) {
        bookingContextText.innerText = pageBookingId > 0 ? `Booking ID: #${pageBookingId}` : "Booking ID: -";
    }
}

function checkExistingBookingReport() {
    fetch(`${baseURL}/ReportServlet?action=bookingReport&bookingId=${encodeURIComponent(pageBookingId)}&t=${Date.now()}`, {
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
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data && data.status === "success" && data.report) {
            renderSubmittedReportView(data.report);
            return;
        }

        if (data && data.status === "reported_account") {
            disableReportForm(data.message || "This booking has been reported and is under admin review.");
        }
    })
    .catch(function() {
    });
}

function renderSubmittedReportView(report) {
    const reportForm = document.getElementById("reportForm");

    if (!reportForm) {
        return;
    }

    reportForm.innerHTML = `
        <input id="bookingId" type="hidden" value="${Number(report.bookingId || pageBookingId || 0)}">

        <div class="auto-report-box">
            <p>Booking ID: #${Number(report.bookingId || pageBookingId || 0)}</p>
            <p>Your report has already been submitted. Booking status is now Report.</p>
        </div>

        <div class="submitted-report-view">
            <h4>Report Details</h4>
            <p><strong>Report Type:</strong> ${escapeHtml(report.reportOption || "-")}</p>
            <p><strong>Report Status:</strong> ${escapeHtml(report.status || "-")}</p>
            <p><strong>Action Taken:</strong> ${escapeHtml(report.actionTaken || "-")}</p>
            <p><strong>Action Date:</strong> ${escapeHtml(report.actionDate || "-")}</p>
            <p><strong>Admin Note:</strong> ${escapeHtml(report.adminNote || "-")}</p>
            <p><strong>Explanation:</strong> ${escapeHtml(report.explanation || "-")}</p>
        </div>
    `;
}

function disableReportForm(message) {
    const submitBtn = document.getElementById("btnSubmitReport");

    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerText = "Report Unavailable";
    }

    showStatus("Failed!", message, "error");
}

function submitReport() {
    const bookingId = Number(getElementValue("bookingId") || pageBookingId || 0);
    const reportType = getElementValue("reportType");
    const explanation = getElementValue("explanation");
    const submitBtn = document.getElementById("btnSubmitReport");

    if (bookingId <= 0) {
        showStatus("Failed!", "Booking ID not found. Please open report from tracking page.", "error");
        return;
    }

    if (reportType === "") {
        showStatus("Failed!", "Please choose a report type.", "error");
        return;
    }

    if (explanation === "") {
        showStatus("Failed!", "Please write your explanation.", "error");
        return;
    }

    if (explanation.length < 10) {
        showStatus("Failed!", "Explanation must be at least 10 characters.", "error");
        return;
    }

    submitBtn.disabled = true;
    submitBtn.innerText = "Submitting...";

    fetch(`${baseURL}/ReportServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        body: JSON.stringify({
            action: "submitReport",
            bookingId: bookingId,
            reportType: reportType,
            reportOption: reportType,
            explanation: explanation
        })
    })
    .then(function(response) {
        return response.json();
    })
    .then(function(data) {
        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data && data.status === "success") {
            showStatus("Success!", data.message || "Your report has been submitted successfully.", "success");
            setTimeout(function() {
                checkExistingBookingReport();
            }, 500);
            return;
        }

        showStatus("Failed!", data.message || "Unable to submit report.", "error");
    })
    .catch(function() {
        showStatus("Failed!", "Network connection or server processing error.", "error");
    })
    .finally(function() {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerText = "Submit Report";
        }
    });
}

function loadReports() {
    if (!isAdminPage) {
        return;
    }

    const container = document.getElementById("reportList");

    if (!container) {
        return;
    }

    container.innerHTML = `<div class="empty-state">Loading reports...</div>`;

    fetch(`${baseURL}/ReportServlet?action=list&t=${Date.now()}`, {
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
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data && data.status === "error") {
            container.innerHTML = `<div class="empty-state">${escapeHtml(data.message || "Unable to load reports.")}</div>`;
            return;
        }

        renderReports(data.reports || []);
    })
    .catch(function() {
        container.innerHTML = `<div class="empty-state">Connection error while loading reports.</div>`;
    });
}

function renderReports(reports) {
    const container = document.getElementById("reportList");
    container.innerHTML = "";

    if (!reports || reports.length === 0) {
        container.innerHTML = `<div class="empty-state">No report submitted yet.</div>`;
        return;
    }

    reports.forEach(function(report) {
        const statusClass = getStatusClass(report.status || "Submitted");
        const item = document.createElement("div");
        item.className = "report-item";

        const actionDone = isReportActionDone(report);
        const detailButton = actionDone
            ? `<button class="detail-btn detail-btn-disabled" type="button" disabled>Action Taken</button>`
            : `<button class="detail-btn" type="button" onclick="openReportDetails(${Number(report.id)})">Details</button>`;

        item.innerHTML = `
            <div class="report-item-top">
                <div>
                    <h4 class="report-title">#${Number(report.id)} - ${escapeHtml(report.reportOption || "-")}</h4>
                    <span class="status-pill ${statusClass}">${escapeHtml(report.status || "Submitted")}</span>
                </div>
                <div class="report-date">${escapeHtml(report.createdAt || "-")}</div>
            </div>

            <div class="report-grid">
                <p class="report-info"><strong>Reporter:</strong> ${escapeHtml(report.reporterName || "Unknown")} (${escapeHtml(report.reporterType || "-")} ID: ${Number(report.reporterId || 0)})</p>
                <p class="report-info"><strong>Reported:</strong> ${escapeHtml(report.reportedName || "Unknown")} (${escapeHtml(report.reportedType || "-")} ID: ${Number(report.reportedId || 0)})</p>
                <p class="report-info"><strong>Booking ID:</strong> ${Number(report.bookingId || 0) > 0 ? Number(report.bookingId) : "-"}</p>
                <p class="report-info"><strong>Reported Status:</strong> ${escapeHtml(report.reportedAccountStatus || "-")}</p>
                <p class="report-info"><strong>Action Taken:</strong> ${escapeHtml(report.actionTaken || "-")}</p>
            </div>

            <p class="report-explanation">${escapeHtml(shortText(report.explanation || "-", 220))}</p>

            <div class="report-actions">
                ${detailButton}
            </div>
        `;

        container.appendChild(item);
    });
}

function isReportActionDone(report) {
    const status = String(report.status || "").trim().toLowerCase();
    const actionTaken = String(report.actionTaken || "").trim().toLowerCase();

    if (status === "action taken" || status === "resolved") {
        return true;
    }

    if (actionTaken === "") {
        return false;
    }

    if (actionTaken === "-" || actionTaken === "investigating") {
        return false;
    }

    return true;
}

function openReportDetails(reportId) {
    if (!isAdminPage) {
        return;
    }

    activeReportId = Number(reportId || 0);

    if (activeReportId <= 0) {
        showStatus("Failed!", "Invalid report ID.", "error");
        return;
    }

    document.getElementById("detailContent").innerHTML = `<div class="empty-state">Loading report details...</div>`;
    document.getElementById("detailModal").style.display = "flex";

    fetch(`${baseURL}/ReportServlet?action=detail&reportId=${activeReportId}&t=${Date.now()}`, {
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
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data && data.status === "success") {
            if (isReportActionDone(data.report)) {
                closeDetailModal();
                loadReports();
                showStatus("Failed!", "Action already taken. This report details cannot be opened again.", "error");
                return;
            }

            renderDetailModal(data.report);
            return;
        }

        document.getElementById("detailContent").innerHTML = `<div class="empty-state">${escapeHtml(data.message || "Unable to load report details.")}</div>`;
    })
    .catch(function() {
        document.getElementById("detailContent").innerHTML = `<div class="empty-state">Connection error while loading report details.</div>`;
    });
}

function renderDetailModal(report) {
    if (!report) {
        document.getElementById("detailContent").innerHTML = `<div class="empty-state">Report not found.</div>`;
        return;
    }

    if (isReportActionDone(report)) {
        closeDetailModal();
        loadReports();
        showStatus("Failed!", "Action already taken. This report details cannot be opened again.", "error");
        return;
    }

    const reportedSuspendCount = Number(report.reportedSuspendCount || 0);
    const bookingAvailable = Number(report.bookingId || 0) > 0;
    const reportedStatus = String(report.reportedAccountStatus || "").toLowerCase();
    const alreadyBanned = reportedStatus === "banned";

    document.getElementById("detailTitle").innerText = `Report #${Number(report.id || 0)}`;
    document.getElementById("detailSubtitle").innerText = `${report.reportOption || "-"} • ${report.createdAt || "-"}`;

    let punishmentButtons = "";

    if (alreadyBanned) {
        punishmentButtons = `<button class="punish-btn ban" type="button" disabled>Already Banned</button>`;
    } else if (reportedSuspendCount >= 3) {
        punishmentButtons = `
            <div class="warning-box">This account already reached ${reportedSuspendCount} suspension count. Permanent ban only.</div>
            <button class="punish-btn no-suspend" type="button" onclick="closeCaseNoSuspend(${Number(report.id)})">No Suspend / Close Case</button>
            <button class="punish-btn ban" type="button" onclick="banReportedAccount(${Number(report.id)})">Permanent Ban</button>
        `;
    } else {
        punishmentButtons = `
            <button class="punish-btn no-suspend" type="button" onclick="closeCaseNoSuspend(${Number(report.id)})">No Suspend / Close Case</button>
            <button class="punish-btn suspend" type="button" onclick="suspendReportedAccount(${Number(report.id)}, 7)">Suspend 1 Week</button>
            <button class="punish-btn suspend" type="button" onclick="suspendReportedAccount(${Number(report.id)}, 30)">Suspend 1 Month</button>
            <button class="punish-btn ban" type="button" onclick="banReportedAccount(${Number(report.id)})">Permanent Ban</button>
        `;
    }

    document.getElementById("detailContent").innerHTML = `
        <div class="detail-section">
            <h4>Report Information</h4>
            <div class="detail-info-grid">
                <p class="detail-info"><strong>Report ID:</strong> ${Number(report.id || 0)}</p>
                <p class="detail-info"><strong>Status:</strong> ${escapeHtml(report.status || "-")}</p>
                <p class="detail-info"><strong>Report Type:</strong> ${escapeHtml(report.reportOption || "-")}</p>
                <p class="detail-info"><strong>Created At:</strong> ${escapeHtml(report.createdAt || "-")}</p>
                <p class="detail-info"><strong>Action Taken:</strong> ${escapeHtml(report.actionTaken || "-")}</p>
                <p class="detail-info"><strong>Action Date:</strong> ${escapeHtml(report.actionDate || "-")}</p>
                <p class="detail-info"><strong>Admin Note:</strong> ${escapeHtml(report.adminNote || "-")}</p>
                <p class="detail-info"><strong>Action Admin ID:</strong> ${Number(report.actionAdminId || 0) > 0 ? Number(report.actionAdminId) : "-"}</p>
            </div>
            <p class="report-explanation">${escapeHtml(report.explanation || "-")}</p>

            <div class="detail-main-actions">
                <button class="action-btn investigate" type="button" onclick="markInvestigating(${Number(report.id)})">Mark Investigating</button>
                <button class="action-btn resolve" type="button" onclick="resolveReport(${Number(report.id)})">Close Case</button>
            </div>
        </div>

        <div class="detail-section">
            <h4>Booking Details</h4>
            ${bookingAvailable ? `
                <div class="detail-info-grid">
                    <p class="detail-info"><strong>Booking ID:</strong> ${Number(report.bookingId || 0)}</p>
                    <p class="detail-info"><strong>Booking Date:</strong> ${escapeHtml(report.bookingDate || "-")}</p>
                    <p class="detail-info"><strong>Booking Status:</strong> ${escapeHtml(report.bookingStatus || "-")}</p>
                    <p class="detail-info"><strong>Subservice:</strong> ${escapeHtml(report.subserviceBooked || "-")}</p>
                    <p class="detail-info"><strong>Problem:</strong> ${escapeHtml(report.problem || "-")}</p>
                    <p class="detail-info"><strong>Total Amount:</strong> RM ${formatMoney(report.totalAmount)}</p>
                    <p class="detail-info"><strong>Deposit:</strong> RM ${formatMoney(report.deposit)}</p>
                    <p class="detail-info"><strong>Total Balance:</strong> RM ${formatMoney(report.totalBalance)}</p>
                    <p class="detail-info"><strong>Travel Fee:</strong> RM ${formatMoney(report.travelFee)}</p>
                    <p class="detail-info"><strong>Material Cost:</strong> RM ${formatMoney(report.materialCost)}</p>
                    <p class="detail-info"><strong>Distance:</strong> ${formatMoney(report.distanceKm)} km</p>
                    <p class="detail-info"><strong>Evidence Path:</strong> ${escapeHtml(report.evidencePath || "-")}</p>
                </div>
            ` : `
                <div class="empty-state">No booking ID linked to this report.</div>
            `}
        </div>

        <div class="detail-section">
            <h4>People Involved</h4>
            <div class="detail-two-col">
                <div class="person-card">
                    <h5>Person Who Reported</h5>
                    <p class="detail-info"><strong>Name:</strong> ${escapeHtml(report.reporterName || "Unknown")}</p>
                    <p class="detail-info"><strong>Type:</strong> ${escapeHtml(report.reporterType || "-")}</p>
                    <p class="detail-info"><strong>ID:</strong> ${Number(report.reporterId || 0)}</p>
                    <p class="detail-info"><strong>Email:</strong> ${escapeHtml(report.reporterEmail || "-")}</p>
                    <p class="detail-info"><strong>Phone:</strong> ${escapeHtml(report.reporterPhone || "-")}</p>
                    <p class="detail-info"><strong>Status:</strong> ${escapeHtml(report.reporterAccountStatus || "-")}</p>
                    <p class="detail-info"><strong>Suspend Count:</strong> ${Number(report.reporterSuspendCount || 0)}</p>
                    <p class="detail-info"><strong>Address:</strong> ${escapeHtml(report.reporterAddress || "-")}</p>
                </div>

                <div class="person-card">
                    <h5>Person Being Reported</h5>
                    <p class="detail-info"><strong>Name:</strong> ${escapeHtml(report.reportedName || "Unknown")}</p>
                    <p class="detail-info"><strong>Type:</strong> ${escapeHtml(report.reportedType || "-")}</p>
                    <p class="detail-info"><strong>ID:</strong> ${Number(report.reportedId || 0)}</p>
                    <p class="detail-info"><strong>Email:</strong> ${escapeHtml(report.reportedEmail || "-")}</p>
                    <p class="detail-info"><strong>Phone:</strong> ${escapeHtml(report.reportedPhone || "-")}</p>
                    <p class="detail-info"><strong>Status:</strong> ${escapeHtml(report.reportedAccountStatus || "-")}</p>
                    <p class="detail-info"><strong>Suspend Count:</strong> ${reportedSuspendCount}</p>
                    <p class="detail-info"><strong>Suspend Start:</strong> ${escapeHtml(report.reportedSuspendStartDate || "-")}</p>
                    <p class="detail-info"><strong>Suspend End:</strong> ${escapeHtml(report.reportedSuspendEndDate || "-")}</p>
                    <p class="detail-info"><strong>Ban Reason:</strong> ${escapeHtml(report.reportedBanReason || "-")}</p>
                    <p class="detail-info"><strong>Address:</strong> ${escapeHtml(report.reportedAddress || "-")}</p>

                    <div class="punishment-actions">
                        ${punishmentButtons}
                    </div>
                </div>
            </div>
        </div>
    `;
}

function closeDetailModal() {
    document.getElementById("detailModal").style.display = "none";
    activeReportId = 0;
}

function markInvestigating(reportId) {
    if (!isAdminPage) {
        return;
    }

    const note = prompt("Investigation note:");

    if (note === null) {
        return;
    }

    updateReportAction("investigate", reportId, 0, note);
}

function resolveReport(reportId) {
    if (!isAdminPage) {
        return;
    }

    const note = prompt("Close case note:");

    if (note === null) {
        return;
    }

    updateReportAction("resolve", reportId, 0, note);
}

function closeCaseNoSuspend(reportId) {
    if (!isAdminPage) {
        return;
    }

    const note = prompt("No suspend / close case reason:");

    if (note === null) {
        return;
    }

    if (note.trim() === "") {
        showStatus("Failed!", "Close case reason cannot be empty.", "error");
        return;
    }

    updateReportAction("noSuspendClose", reportId, 0, note);
}

function suspendReportedAccount(reportId, suspendDays) {
    if (!isAdminPage) {
        return;
    }

    const note = prompt(`Suspend reason for ${suspendDays} day(s):`);

    if (note === null) {
        return;
    }

    if (note.trim() === "") {
        showStatus("Failed!", "Suspend reason cannot be empty.", "error");
        return;
    }

    updateReportAction("suspend", reportId, suspendDays, note);
}

function banReportedAccount(reportId) {
    if (!isAdminPage) {
        return;
    }

    const note = prompt("Permanent ban reason:");

    if (note === null) {
        return;
    }

    if (note.trim() === "") {
        showStatus("Failed!", "Ban reason cannot be empty.", "error");
        return;
    }

    updateReportAction("ban", reportId, 0, note);
}

function updateReportAction(action, reportId, suspendDays, adminNote) {
    if (!isAdminPage) {
        return;
    }

    fetch(`${baseURL}/ReportServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        body: JSON.stringify({
            action: action,
            reportId: Number(reportId || 0),
            suspendDays: Number(suspendDays || 0),
            adminNote: adminNote || ""
        })
    })
    .then(function(response) {
        return response.json();
    })
    .then(function(data) {
        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data && data.status === "success") {
            showStatus("Success!", data.message || "Report updated successfully.", "success");

            if (action === "investigate") {
                loadReports();

                if (activeReportId > 0) {
                    openReportDetails(activeReportId);
                }

                return;
            }

            closeDetailModal();
            loadReports();
            return;
        }

        showStatus("Failed!", data.message || "Unable to update report.", "error");
    })
    .catch(function() {
        showStatus("Failed!", "Network connection or server processing error.", "error");
    });
}

function getStatusClass(status) {
    const clean = String(status || "").toLowerCase().trim();

    if (clean === "submitted") {
        return "status-submitted";
    }

    if (clean === "investigating") {
        return "status-investigating";
    }

    if (clean === "action taken") {
        return "status-action-taken";
    }

    if (clean === "resolved") {
        return "status-resolved";
    }

    return "";
}

function showStatus(title, message, type) {
    const modal = document.getElementById("statusModal");
    const card = document.getElementById("statusCard");
    const icon = document.getElementById("statusIcon");

    card.classList.remove("success", "error");
    card.classList.add(type === "success" ? "success" : "error");

    icon.innerText = type === "success" ? "✓" : "×";
    document.getElementById("statusTitle").innerText = title;
    document.getElementById("statusMessage").innerText = message;

    modal.style.display = "flex";
}

function getElementValue(id) {
    const element = document.getElementById(id);
    return element ? String(element.value || "").trim() : "";
}

function escapeHtml(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function shortText(value, limit) {
    const text = String(value || "");

    if (text.length <= limit) {
        return text;
    }

    return text.substring(0, limit) + "...";
}

function formatMoney(value) {
    const number = Number(value || 0);

    if (isNaN(number)) {
        return "0.00";
    }

    return number.toFixed(2);
}