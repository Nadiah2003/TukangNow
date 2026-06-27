const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

document.addEventListener("DOMContentLoaded", function() {
    const popupOverlay = document.getElementById("popupOverlay");
    const popupContinueBtn = document.getElementById("popupContinueBtn");

    if (popupContinueBtn) {
        popupContinueBtn.addEventListener("click", closePopup);
    }

    if (popupOverlay) {
        popupOverlay.addEventListener("click", function(e) {
            if (e.target === popupOverlay) {
                closePopup();
            }
        });
    }

    fetchJobHistory();
});

function openPopup(title, message, icon = "✕") {
    const popupOverlay = document.getElementById("popupOverlay");
    const popupTitle = document.getElementById("popupTitle");
    const popupMessage = document.getElementById("popupMessage");
    const popupIconSymbol = document.getElementById("popupIconSymbol");

    if (!popupOverlay || !popupTitle || !popupMessage || !popupIconSymbol) {
        alert(title + "\n" + message);
        return;
    }

    popupTitle.textContent = title;
    popupMessage.textContent = message;
    popupIconSymbol.textContent = icon;

    popupOverlay.classList.add("show");
    popupOverlay.setAttribute("aria-hidden", "false");
}

function closePopup() {
    const popupOverlay = document.getElementById("popupOverlay");

    if (!popupOverlay) {
        return;
    }

    popupOverlay.classList.remove("show");
    popupOverlay.setAttribute("aria-hidden", "true");
}

function fetchJobHistory() {
    fetch(`${baseURL}/JobHistoryServlet`, {
        method: "GET",
        credentials: "same-origin",
        headers: {
            "Accept": "application/json"
        }
    })
    .then(function(res) {
        if (!res.ok) {
            throw new Error("HTTP " + res.status);
        }

        return res.json();
    })
    .then(function(data) {
        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || `${baseURL}/login.html`;
            return;
        }

        if (data && data.status === "error") {
            openPopup("Failed!", data.message || "Failed to load job history.");
            renderEmptyState();
            return;
        }

        renderJobHistory(data);
    })
    .catch(function(err) {
        console.error("Error fetching jobs:", err);
        openPopup("Failed!", "Failed to load job history. Please try again.");
        renderEmptyState();
    });
}

function renderJobHistory(data) {
    const tbody = document.getElementById("jobTableBody");
    const cards = document.getElementById("jobCards");

    if (!tbody || !cards) {
        return;
    }

    tbody.innerHTML = "";
    cards.innerHTML = "";

    if (!Array.isArray(data) || data.length === 0) {
        renderEmptyState();
        return;
    }

    tbody.innerHTML = data.map(function(job) {
        const amount = Number(job.amount || 0).toFixed(2);
        const ratingVal = Number(job.ratingVal || 0);
        const ratingComment = job.ratingComment || "-";

        return `
            <tr>
                <td class="cust-info">
                    <strong>${safe(job.custName)}</strong>
                    <span>📞 ${safe(job.custPhone)}</span>
                    <span>✉️ ${safe(job.custEmail)}</span>
                    <span class="address-text">📍 ${safe(job.custAddress)}</span>
                    <p class="rating-text"><span class="rating-stars">${renderStars(ratingVal)}</span> ${ratingVal.toFixed(1)}/5.0</p>
                    <p class="comment-text"><strong>Comment:</strong> ${safe(ratingComment)}</p>
                </td>
                <td>
                    <div class="job-type">${safe(job.subservice)}</div>
                    <div class="job-date">📅 ${safe(job.bookingDate)}</div>
                </td>
                <td>
                    <div class="price-tag">RM ${amount}</div>
                </td>
            </tr>
        `;
    }).join("");

    cards.innerHTML = data.map(function(job) {
        const amount = Number(job.amount || 0).toFixed(2);
        const ratingVal = Number(job.ratingVal || 0);
        const ratingComment = job.ratingComment || "-";

        return `
            <div class="job-card">
                <h4 class="job-card__name">${safe(job.custName)}</h4>
                <p class="job-card__line">📞 ${safe(job.custPhone)}</p>
                <p class="job-card__line">✉️ ${safe(job.custEmail)}</p>
                <p class="job-card__line">📍 ${safe(job.custAddress)}</p>
                <p class="job-card__line"><strong>Job:</strong> ${safe(job.subservice)}</p>
                <p class="job-card__line">📅 ${safe(job.bookingDate)}</p>
                <p class="job-card__line"><span class="rating-stars">${renderStars(ratingVal)}</span> ${ratingVal.toFixed(1)}/5.0</p>
                <p class="job-card__line"><strong>Comment:</strong> ${safe(ratingComment)}</p>
                <div class="job-card__amount">
                    <span class="price-tag">RM ${amount}</span>
                </div>
            </div>
        `;
    }).join("");
}

function renderEmptyState() {
    const tbody = document.getElementById("jobTableBody");
    const cards = document.getElementById("jobCards");

    if (tbody) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="3">
                    <div class="empty-icon">📋</div>
                    <p class="empty-title">No history job</p>
                    <p class="empty-subtitle">All completed bookings will appear here.</p>
                </td>
            </tr>
        `;
    }

    if (cards) {
        cards.innerHTML = `
            <div class="job-card">
                <h4 class="job-card__name">📋 No history job</h4>
                <p class="job-card__line">All completed bookings will appear here.</p>
            </div>
        `;
    }
}

function renderStars(value) {
    const rating = Math.max(0, Math.min(5, Math.round(Number(value || 0))));
    let stars = "";

    for (let i = 1; i <= 5; i++) {
        stars += i <= rating ? "★" : "☆";
    }

    return stars;
}

function safe(value) {
    if (value === null || value === undefined) {
        return "-";
    }

    const clean = String(value).trim();

    if (clean === "") {
        return "-";
    }

    return clean
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}