const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let currentBalance = 0;

document.addEventListener("DOMContentLoaded", function() {
    const popupOverlay = document.getElementById("popupOverlay");
    const popupContinueBtn = document.getElementById("popupContinueBtn");
    const withdrawForm = document.getElementById("withdrawForm");

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

    if (withdrawForm) {
        withdrawForm.addEventListener("submit", withdrawMoney);
    }

    fetchWalletData();
});

function fetchWalletData() {
    fetch(`${baseURL}/VendorWalletServlet`, {
        method: "GET",
        credentials: "same-origin",
        headers: {
            "Accept": "application/json"
        }
    })
    .then(handleJsonResponse)
    .then(function(data) {
        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || `${baseURL}/login.html`;
            return;
        }

        if (data && data.status === "error") {
            openPopup("Failed!", data.message || "Failed to load wallet.");
            renderEmptyTransactions();
            return;
        }

        renderWallet(data.wallet);
        renderTransactions(data.transactions);
    })
    .catch(function(err) {
        console.error("Error fetching wallet:", err);
        openPopup("Failed!", err.message || "Failed to load wallet. Please try again.");
        renderEmptyTransactions();
    });
}

function withdrawMoney(e) {
    e.preventDefault();

    const amountInput = document.getElementById("withdrawAmount");
    const withdrawBtn = document.querySelector(".withdraw-btn");

    if (!amountInput) {
        return;
    }

    const amount = Number(amountInput.value || 0);

    if (amount <= 0) {
        openPopup("Invalid Amount", "Please enter a valid withdraw amount.");
        return;
    }

    if (amount > currentBalance) {
        openPopup("Insufficient Balance", "Your withdraw amount is more than your available balance.");
        return;
    }

    if (withdrawBtn) {
        withdrawBtn.disabled = true;
        withdrawBtn.textContent = "Processing...";
    }

    const formData = new URLSearchParams();
    formData.append("action", "withdraw");
    formData.append("amount", amount.toFixed(2));

    fetch(`${baseURL}/VendorWalletServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "Accept": "application/json"
        },
        body: formData.toString()
    })
    .then(handleJsonResponse)
    .then(function(data) {
        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || `${baseURL}/login.html`;
            return;
        }

        if (data && data.status === "error") {
            openPopup("Failed!", data.message || "Withdraw failed.");
            return;
        }

        amountInput.value = "";
        openPopup("Success!", data.message || "Withdraw successful.", "✓", true);
        renderWallet(data.wallet);
        renderTransactions(data.transactions);
    })
    .catch(function(err) {
        console.error("Error withdraw:", err);
        openPopup("Failed!", err.message || "Withdraw failed. Please try again.");
    })
    .finally(function() {
        if (withdrawBtn) {
            withdrawBtn.disabled = false;
            withdrawBtn.textContent = "Withdraw";
        }
    });
}

function handleJsonResponse(res) {
    return res.text().then(function(text) {
        let data = null;

        try {
            data = text ? JSON.parse(text) : null;
        } catch (e) {
            throw new Error("Invalid server response.");
        }

        if (!res.ok) {
            throw new Error(data && data.message ? data.message : "HTTP " + res.status);
        }

        return data;
    });
}

function renderWallet(wallet) {
    const walletBalance = document.getElementById("walletBalance");
    const totalEarned = document.getElementById("totalEarned");
    const totalWithdrawn = document.getElementById("totalWithdrawn");

    currentBalance = Number(wallet && wallet.balance !== undefined ? wallet.balance : 0);

    if (walletBalance) {
        walletBalance.textContent = formatRM(currentBalance);
    }

    if (totalEarned) {
        totalEarned.textContent = formatRM(Number(wallet && wallet.totalEarned !== undefined ? wallet.totalEarned : 0));
    }

    if (totalWithdrawn) {
        totalWithdrawn.textContent = formatRM(Number(wallet && wallet.totalWithdrawn !== undefined ? wallet.totalWithdrawn : 0));
    }
}

function renderTransactions(transactions) {
    const tbody = document.getElementById("walletTableBody");
    const cards = document.getElementById("walletCards");

    if (!tbody || !cards) {
        return;
    }

    tbody.innerHTML = "";
    cards.innerHTML = "";

    if (!Array.isArray(transactions) || transactions.length === 0) {
        renderEmptyTransactions();
        return;
    }

    tbody.innerHTML = transactions.map(function(item) {
        const type = String(item.type || "").toUpperCase();
        const amount = Number(item.amount || 0);
        const isWithdraw = type === "WITHDRAW";
        const badgeClass = isWithdraw ? "type-withdraw" : "type-credit";
        const amountClass = isWithdraw ? "amount-withdraw" : "amount-credit";
        const amountText = isWithdraw ? "- " + formatRM(amount) : "+ " + formatRM(amount);

        return `
            <tr>
                <td>
                    <span class="transaction-date">${safe(item.createdAt)}</span>
                </td>
                <td>
                    <span class="type-badge ${badgeClass}">${safe(type)}</span>
                </td>
                <td>
                    <span class="transaction-desc">${safe(item.description)}</span>
                </td>
                <td>
                    <span class="${amountClass}">${amountText}</span>
                </td>
            </tr>
        `;
    }).join("");

    cards.innerHTML = transactions.map(function(item) {
        const type = String(item.type || "").toUpperCase();
        const amount = Number(item.amount || 0);
        const isWithdraw = type === "WITHDRAW";
        const badgeClass = isWithdraw ? "type-withdraw" : "type-credit";
        const amountClass = isWithdraw ? "amount-withdraw" : "amount-credit";
        const amountText = isWithdraw ? "- " + formatRM(amount) : "+ " + formatRM(amount);

        return `
            <div class="wallet-card">
                <div class="wallet-card__top">
                    <span class="type-badge ${badgeClass}">${safe(type)}</span>
                    <p class="wallet-card__date">${safe(item.createdAt)}</p>
                </div>
                <p class="wallet-card__desc">${safe(item.description)}</p>
                <p class="wallet-card__amount">
                    <span class="${amountClass}">${amountText}</span>
                </p>
            </div>
        `;
    }).join("");
}

function renderEmptyTransactions() {
    const tbody = document.getElementById("walletTableBody");
    const cards = document.getElementById("walletCards");

    if (tbody) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="4">
                    <div class="empty-icon">💳</div>
                    <p class="empty-title">No wallet transaction</p>
                    <p class="empty-subtitle">Your wallet transactions will appear here.</p>
                </td>
            </tr>
        `;
    }

    if (cards) {
        cards.innerHTML = `
            <div class="wallet-card">
                <p class="wallet-card__desc">💳 No wallet transaction</p>
                <p class="wallet-card__date">Your wallet transactions will appear here.</p>
            </div>
        `;
    }
}

function openPopup(title, message, icon = "✕", success = false) {
    const popupOverlay = document.getElementById("popupOverlay");
    const popupTitle = document.getElementById("popupTitle");
    const popupMessage = document.getElementById("popupMessage");
    const popupIconSymbol = document.getElementById("popupIconSymbol");
    const popupIcon = document.getElementById("popupIcon");

    if (!popupOverlay || !popupTitle || !popupMessage || !popupIconSymbol || !popupIcon) {
        alert(title + "\n" + message);
        return;
    }

    popupTitle.textContent = title;
    popupMessage.textContent = message;
    popupIconSymbol.textContent = icon;

    if (success) {
        popupIcon.classList.add("success");
    } else {
        popupIcon.classList.remove("success");
    }

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

function formatRM(value) {
    return "RM " + Number(value || 0).toFixed(2);
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