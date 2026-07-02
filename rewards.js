const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let currentActiveTab = "catalog";
let cachedData = null;

document.addEventListener("DOMContentLoaded", function() {
    const btnBack = document.getElementById("btnBack");
    const tabButtons = document.querySelectorAll(".tab-btn");

    if (btnBack) {
        btnBack.addEventListener("click", function() {
            window.location.href = "homecustomer.html";
        });
    }

    tabButtons.forEach(function(button) {
        button.addEventListener("click", function() {
            switchTab(button.getAttribute("data-tab"));
        });
    });

    loadRewardsData();
});

function switchTab(tabName) {
    currentActiveTab = tabName;

    const tabs = document.querySelectorAll(".tab-btn");
    tabs.forEach(function(btn) {
        btn.classList.remove("active");
    });

    const activeButton = document.querySelector(`.tab-btn[data-tab="${tabName}"]`);

    if (activeButton) {
        activeButton.classList.add("active");
    }

    renderContent();
}

function loadRewardsData() {
    fetch(`${baseURL}/RewardsServlet`, {
        method: "GET",
        credentials: "same-origin"
    })
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            if (data.status === "session_expired") {
                window.location.href = data.redirect || "index.html";
                return;
            }

            if (data.status === "error") {
                showErrorAlert(data.message || "Unable to load rewards data.");
                return;
            }

            cachedData = data;

            const customer = data.customer || {};
            document.getElementById("userNameDisplay").innerText = customer.name || "Customer";
            document.getElementById("userPointsDisplay").innerHTML = `${Number(customer.rewards_points || 0).toLocaleString()} <small>Pts</small>`;

            renderContent();
        })
        .catch(function() {
            showErrorAlert("Something went wrong while loading rewards data.");
        });
}

function renderContent() {
    const container = document.getElementById("rewardsContainer");

    if (!cachedData || !container) {
        return;
    }

    container.innerHTML = "";

    if (currentActiveTab === "catalog") {
        renderCatalog(container);
        return;
    }

    if (currentActiveTab === "myvouchers") {
        renderMyVouchers(container);
        return;
    }

    if (currentActiveTab === "history") {
        renderHistory(container);
    }
}

function renderCatalog(container) {
    const rewards = cachedData.rewards || [];

    if (rewards.length === 0) {
        container.innerHTML = `<div class="empty-text">No rewards available at the moment.</div>`;
        return;
    }

    rewards.forEach(function(item) {
        const id = Number(item.id || 0);
        const title = item.voucher_name || "Reward Voucher";
        const points = Number(item.points_required || 0);
        const discount = Number(item.discount_amount || 0);

        const card = document.createElement("div");
        card.className = "reward-card";

        const icon = document.createElement("div");
        icon.className = "reward-icon-container";
        icon.innerText = "🎁";

        const details = document.createElement("div");
        details.className = "reward-details";

        const titleElement = document.createElement("h4");
        titleElement.className = "reward-title";
        titleElement.innerText = title;

        const pointsElement = document.createElement("p");
        pointsElement.className = "reward-points";
        pointsElement.innerText = `${points.toLocaleString()} Pts`;

        const discountElement = document.createElement("p");
        discountElement.className = "reward-expiry";
        discountElement.innerText = `Discount: RM ${discount.toFixed(2)}`;

        details.appendChild(titleElement);
        details.appendChild(pointsElement);
        details.appendChild(discountElement);

        const button = document.createElement("button");
        button.className = "redeem-btn";
        button.type = "button";
        button.innerText = "Redeem";
        button.addEventListener("click", function() {
            confirmRedeem(id, title, points);
        });

        card.appendChild(icon);
        card.appendChild(details);
        card.appendChild(button);
        container.appendChild(card);
    });
}

function renderMyVouchers(container) {
    const myVouchers = cachedData.my_vouchers || [];

    if (myVouchers.length === 0) {
        container.innerHTML = `<div class="empty-text">You don't have any active vouchers.</div>`;
        return;
    }

    myVouchers.forEach(function(item) {
        const card = document.createElement("div");
        card.className = "reward-card";

        const discount = Number(item.discount_amount || 0);

        card.innerHTML = `
            <div class="reward-icon-container" style="background:#e6f4ea; color:#137333;">🎟️</div>
            <div class="reward-details">
                <h4 class="reward-title">${escapeHtml(item.voucher_name || "Voucher")}</h4>
                <p class="reward-points" style="color:#137333;">Discount: RM ${discount.toFixed(2)}</p>
                <p class="reward-expiry">Redeemed on: ${escapeHtml(item.redeemed_at || "N/A")}</p>
            </div>
        `;

        container.appendChild(card);
    });
}

function renderHistory(container) {
    const history = cachedData.history_vouchers || [];

    if (history.length === 0) {
        container.innerHTML = `<div class="empty-text">No reward points history available.</div>`;
        return;
    }

    history.forEach(function(item) {
        const points = Number(item.points || 0);
        const isEarned = points >= 0;

        const card = document.createElement("div");
        card.className = "reward-card";

        card.innerHTML = `
            <div class="reward-icon-container" style="background:#f1f3f4; color:#5f6368;">📜</div>
            <div class="reward-details">
                <h4 class="reward-title" style="color:#5f6368;">${escapeHtml(item.activity || "Reward Activity")}</h4>
                <p class="reward-expiry">${escapeHtml(item.date_created || "N/A")}</p>
            </div>
            <span class="reward-status-tag ${isEarned ? "status-earned" : "status-redeemed"}">${isEarned ? "+" : ""}${points.toLocaleString()} Pts</span>
        `;

        container.appendChild(card);
    });
}

function confirmRedeem(rewardId, rewardTitle, points) {
    const userPoints = Number((cachedData.customer || {}).rewards_points || 0);

    if (userPoints < points) {
        Swal.fire({
            html: `<div class="swal-custom-icon-circle error-theme"></div>
                   <div class="swal-custom-title">Insufficient Points</div>
                   <div class="swal-custom-html">You need ${Number(points).toLocaleString()} points to redeem this item. You currently have ${userPoints.toLocaleString()} points.</div>`,
            showConfirmButton: true,
            confirmButtonText: "OK",
            customClass: {
                popup: "swal-custom-popup",
                actions: "swal-custom-actions-container",
                confirmButton: "swal-custom-btn-confirm"
            },
            buttonsStyling: false
        });
        return;
    }

    Swal.fire({
        html: `<div class="swal-custom-title" style="margin-top:15px;">Confirm Redemption</div>
               <div class="swal-custom-html">Are you sure you want to redeem <strong>${escapeHtml(rewardTitle)}</strong> for <strong>${Number(points).toLocaleString()} Pts</strong>?</div>`,
        showCancelButton: true,
        confirmButtonText: "Confirm",
        cancelButtonText: "Cancel",
        customClass: {
            popup: "swal-custom-popup",
            actions: "swal-custom-actions-container",
            confirmButton: "swal-custom-btn-confirm",
            cancelButton: "swal-custom-btn-cancel"
        },
        buttonsStyling: false
    }).then(function(result) {
        if (result.isConfirmed) {
            executeRedemption(rewardId);
        }
    });
}

function executeRedemption(rewardId) {
    const body = new URLSearchParams();
    body.append("action", "redeem");
    body.append("reward_id", rewardId);

    fetch(`${baseURL}/RewardsServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        },
        body: body.toString()
    })
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            if (data.status === "session_expired") {
                window.location.href = data.redirect || "index.html";
                return;
            }

            if (data.status === "success") {
                Swal.fire({
                    html: `<div class="swal-custom-icon-circle success-theme"></div>
                           <div class="swal-custom-title">Redeemed Successfully!</div>
                           <div class="swal-custom-html">${escapeHtml(data.message || "Voucher redeemed successfully.")}</div>`,
                    showConfirmButton: true,
                    confirmButtonText: "Great",
                    customClass: {
                        popup: "swal-custom-popup",
                        actions: "swal-custom-actions-container",
                        confirmButton: "swal-custom-btn-confirm"
                    },
                    buttonsStyling: false
                }).then(function() {
                    loadRewardsData();
                });
                return;
            }

            showErrorAlert(data.message || "Redemption failed.");
        })
        .catch(function() {
            showErrorAlert("Something went wrong while processing your request.");
        });
}

function showErrorAlert(message) {
    Swal.fire({
        html: `<div class="swal-custom-icon-circle error-theme"></div>
               <div class="swal-custom-title">Error</div>
               <div class="swal-custom-html">${escapeHtml(message)}</div>`,
        showConfirmButton: true,
        confirmButtonText: "OK",
        customClass: {
            popup: "swal-custom-popup",
            actions: "swal-custom-actions-container",
            confirmButton: "swal-custom-btn-confirm"
        },
        buttonsStyling: false
    });
}

function escapeHtml(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}