const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

document.addEventListener("DOMContentLoaded", function() {
    const btnBack = document.getElementById("btnBack");
    const reportForm = document.getElementById("reportForm");
    const explanation = document.getElementById("explanation");
    const statusOkBtn = document.getElementById("statusOkBtn");
    const btnRefreshReports = document.getElementById("btnRefreshReports");

    btnBack.addEventListener("click", function() {
        history.back();
    });

    explanation.addEventListener("input", function() {
        document.getElementById("charCount").innerText = `${explanation.value.length} / 1000`;
    });

    reportForm.addEventListener("submit", function(event) {
        event.preventDefault();
        submitReport();
    });

    statusOkBtn.addEventListener("click", function() {
        document.getElementById("statusModal").style.display = "none";
    });

    btnRefreshReports.addEventListener("click", function() {
        loadReports();
    });

    loadReports();
});

function submitReport() {
    const reporterType = document.getElementById("reporterType").value.trim();
    const reportedType = document.getElementById("reportedType").value.trim();
    const reportedId = Number(document.getElementById("reportedId").value || 0);
    const reportOption = document.getElementById("reportOption").value.trim();
    const explanation = document.getElementById("explanation").value.trim();
    const submitBtn = document.getElementById("btnSubmitReport");

    if (reporterType === "") {
        showStatus("Failed!", "Please choose your account type.", "error");
        return;
    }

    if (reportedType === "") {
        showStatus("Failed!", "Please choose reported account type.", "error");
        return;
    }

    if (reportedId <= 0) {
        showStatus("Failed!", "Please enter reported account ID.", "error");
        return;
    }

    if (reportOption === "") {
        showStatus("Failed!", "Please choose a report option.", "error");
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
            reporterType: reporterType,
            reportedType: reportedType,
            reportedId: reportedId,
            reportOption: reportOption,
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
            document.getElementById("reportForm").reset();
            document.getElementById("charCount").innerText = "0 / 1000";
            showStatus("Success!", data.message || "Your report has been submitted successfully.", "success");
            loadReports();
            return;
        }

        showStatus("Failed!", data.message || "Unable to submit report.", "error");
    })
    .catch(function() {
        showStatus("Failed!", "Network connection or server processing error.", "error");
    })
    .finally(function() {
        submitBtn.disabled = false;
        submitBtn.innerText = "Submit Report";
    });
}

function loadReports() {
    const container = document.getElementById("reportList");
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
                <p class="report-info"><strong>Reported Account Status:</strong> ${escapeHtml(report.reportedAccountStatus || "-")}</p>
                <p class="report-info"><strong>Action Taken:</strong> ${escapeHtml(report.actionTaken || "-")}</p>
                <p class="report-info"><strong>Action Date:</strong> ${escapeHtml(report.actionDate || "-")}</p>
                <p class="report-info"><strong>Admin Note:</strong> ${escapeHtml(report.adminNote || "-")}</p>
            </div>

            <p class="report-explanation">${escapeHtml(report.explanation || "-")}</p>

            <div class="report-actions">
                <button class="action-btn investigate" type="button" onclick="markInvestigating(${Number(report.id)})">Investigate</button>
                <button class="action-btn suspend" type="button" onclick="suspendReportedAccount(${Number(report.id)})">Suspend</button>
                <button class="action-btn ban" type="button" onclick="banReportedAccount(${Number(report.id)})">Ban</button>
                <button class="action-btn resolve" type="button" onclick="resolveReport(${Number(report.id)})">Resolve</button>
            </div>
        `;

        container.appendChild(item);
    });
}

function markInvestigating(reportId) {
    const note = prompt("Investigation note:");

    if (note === null) {
        return;
    }

    updateReportAction("investigate", reportId, 0, note);
}

function resolveReport(reportId) {
    const note = prompt("Resolution note:");

    if (note === null) {
        return;
    }

    updateReportAction("resolve", reportId, 0, note);
}

function suspendReportedAccount(reportId) {
    const daysText = prompt("Suspend for how many days?");

    if (daysText === null) {
        return;
    }

    const days = Number(daysText);

    if (isNaN(days) || days <= 0) {
        showStatus("Failed!", "Suspend days must be more than 0.", "error");
        return;
    }

    const note = prompt("Suspend reason:");

    if (note === null) {
        return;
    }

    updateReportAction("suspend", reportId, days, note);
}

function banReportedAccount(reportId) {
    const note = prompt("Ban reason:");

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
    fetch(`${baseURL}/ReportServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        body: JSON.stringify({
            action: action,
            reportId: reportId,
            suspendDays: suspendDays,
            adminNote: adminNote
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

function escapeHtml(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}