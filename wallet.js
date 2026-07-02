const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let currentMode = "topup";

document.addEventListener("DOMContentLoaded", function() {
    const btnBack = document.getElementById("btnBack");
    const btnTopUp = document.getElementById("btnTopUp");
    const btnWithdraw = document.getElementById("btnWithdraw");
    const btnCancelModal = document.getElementById("btnCancelModal");
    const walletForm = document.getElementById("walletForm");
    const btnCloseMessage = document.getElementById("btnCloseMessage");

    if (btnBack) {
        btnBack.addEventListener("click", function() {
            window.location.href = "homecustomer.html";
        });
    }

    if (btnTopUp) {
        btnTopUp.addEventListener("click", function() {
            openModal("topup");
        });
    }

    if (btnWithdraw) {
        btnWithdraw.addEventListener("click", function() {
            openModal("withdraw");
        });
    }

    if (btnCancelModal) {
        btnCancelModal.addEventListener("click", closeModal);
    }

    if (walletForm) {
        walletForm.addEventListener("submit", handleFormSubmit);
    }

    if (btnCloseMessage) {
        btnCloseMessage.addEventListener("click", closeMessageModal);
    }

    const params = new URLSearchParams(window.location.search);

    if (params.get("topup") === "success") {
        showMessage("Success", "Wallet top up processed successfully.");
    }

    if (params.get("topup") === "failed") {
        showMessage("Failed", "Wallet top up failed or cancelled.");
    }

    fetchWalletData();
});

function fetchWalletData() {
    fetch(`${baseURL}/WalletServlet?action=getWallet`, {
        method: "GET",
        credentials: "same-origin"
    })
    .then(function(response) {
        return response.json();
    })
    .then(function(data) {
        if (data.status === "success") {
            updateUI(data);
        } else {
            showMessage("Failed", data.message || "Failed to load wallet data.");
        }
    })
    .catch(function() {
        showMessage("Failed", "Server connection error.");
    });
}

function updateUI(data) {
    const balanceEl = document.getElementById("walletBalance");
    const balance = Number(data.balance || 0);

    if (balanceEl) {
        balanceEl.innerText = `RM ${balance.toFixed(2)}`;
    }

    const listContainer = document.getElementById("transactionList");

    if (!listContainer) {
        return;
    }

    listContainer.innerHTML = "";

    if (!data.transactions || data.transactions.length === 0) {
        listContainer.innerHTML = `<div class="empty-state">No transaction records found.</div>`;
        return;
    }

    data.transactions.forEach(function(tx) {
        const item = document.createElement("div");
        item.className = "transaction-item";

        const type = String(tx.type || "").toLowerCase();
        const isIncome = type === "topup" || type === "refund" || type === "earnings";
        const prefix = isIncome ? "+" : "-";
        const classType = isIncome ? "income" : "expense";
        const amount = Number(tx.amount || 0);
        const title = tx.description || formatTransactionTitle(type);
        const date = tx.date || tx.created_at || "-";

        item.innerHTML = `
            <div class="tx-details">
                <span class="tx-title">${escapeHtml(title)}</span>
                <span class="tx-date">${escapeHtml(date)}</span>
            </div>
            <div class="tx-amount ${classType}">${prefix} RM ${amount.toFixed(2)}</div>
        `;

        listContainer.appendChild(item);
    });
}

function formatTransactionTitle(type) {
    if (type === "topup") {
        return "Wallet Top Up";
    }

    if (type === "transfer") {
        return "Withdraw to Bank";
    }

    if (type === "payment") {
        return "Service Payment";
    }

    if (type === "refund") {
        return "Booking Refund";
    }

    if (type === "earnings") {
        return "Wallet Earnings";
    }

    return type || "Wallet Transaction";
}

function openModal(mode) {
    currentMode = mode;

    const modal = document.getElementById("walletModal");
    const title = document.getElementById("modalTitle");
    const input = document.getElementById("amountInput");
    const bankFields = document.getElementById("bankFields");
    const bankNameInput = document.getElementById("bankNameInput");
    const accountNoInput = document.getElementById("accountNoInput");

    if (input) {
        input.value = "";
        input.min = mode === "topup" ? "10" : "50";
        input.placeholder = mode === "topup" ? "Minimum RM 10.00" : "Minimum RM 50.00";
    }

    if (bankNameInput) {
        bankNameInput.value = "";
        bankNameInput.required = mode === "withdraw";
    }

    if (accountNoInput) {
        accountNoInput.value = "";
        accountNoInput.required = mode === "withdraw";
    }

    if (title) {
        title.innerText = mode === "topup" ? "Top Up Wallet" : "Withdraw Funds";
    }

    if (bankFields) {
        if (mode === "withdraw") {
            bankFields.classList.add("is-open");
        } else {
            bankFields.classList.remove("is-open");
        }
    }

    if (modal) {
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
    }
}

function closeModal() {
    const modal = document.getElementById("walletModal");

    if (modal) {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
    }
}

function handleFormSubmit(e) {
    e.preventDefault();

    const amountInput = document.getElementById("amountInput");
    const bankNameInput = document.getElementById("bankNameInput");
    const accountNoInput = document.getElementById("accountNoInput");

    const amount = parseFloat(amountInput ? amountInput.value : 0) || 0;

    if (currentMode === "topup" && amount < 10) {
        showMessage("Failed", "Minimum top up value is RM 10.00");
        return;
    }

    if (currentMode === "withdraw" && amount < 50) {
        showMessage("Failed", "Minimum transfer is RM 50.00");
        return;
    }

    const formData = new URLSearchParams();

    if (currentMode === "topup") {
        formData.append("action", "topup");
        formData.append("amount", amount.toFixed(2));
    } else {
        formData.append("action", "transfer_to_bank");
        formData.append("amount", amount.toFixed(2));
        formData.append("bank_name", bankNameInput ? bankNameInput.value.trim() : "");
        formData.append("account_no", accountNoInput ? accountNoInput.value.trim() : "");
    }

    fetch(`${baseURL}/WalletServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: formData.toString()
    })
    .then(function(response) {
        return response.json();
    })
    .then(function(data) {
        if (data.status === "success") {
            closeModal();

            if (data.redirect && data.url) {
                window.location.href = data.url;
                return;
            }

            showMessage("Success", data.message || "Transaction processed successfully.");
            fetchWalletData();
        } else {
            showMessage("Failed", data.message || "Transaction process failed.");
        }
    })
    .catch(function() {
        showMessage("Failed", "Server connection error.");
    });
}

function showMessage(title, message) {
    const modal = document.getElementById("messageModal");
    const messageTitle = document.getElementById("messageTitle");
    const messageText = document.getElementById("messageText");

    if (messageTitle) {
        messageTitle.innerText = title;
    }

    if (messageText) {
        messageText.innerText = message;
    }

    if (modal) {
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
    }
}

function closeMessageModal() {
    const modal = document.getElementById("messageModal");

    if (modal) {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
    }
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}