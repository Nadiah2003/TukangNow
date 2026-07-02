const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let chatTimer = null;
let chatPollingTimer = null;
const TIME_LIMIT = 30 * 60 * 1000;
let isChatSessionActive = false;
let currentChatOrderId = null;

document.addEventListener("DOMContentLoaded", function() {
    fetchOrders();
});

function goHome() {
    window.location.href = "homecustomer.html";
}

async function fetchOrders() {
    const status = document.getElementById("filterStatus").value || "";
    const service = document.getElementById("filterService").value || "";
    const date = document.getElementById("filterDate").value || "";

    const urlParams = new URLSearchParams({
        status: status,
        service: service,
        date: date
    });

    const container = document.getElementById("orderList");
    container.innerHTML = `<div class="loading">Loading your orders...</div>`;

    try {
        const response = await fetch(`${baseURL}/GetOrderServlet?${urlParams.toString()}`, {
            method: "GET",
            credentials: "same-origin"
        });

        const data = await response.json();

        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || "index.html";
            return;
        }

        if (data && data.status === "error") {
            container.innerHTML = `<p style="text-align:center; padding:20px; color:red;">${escapeHtml(data.message || "Error loading data.")}</p>`;
            return;
        }

        if (!Array.isArray(data)) {
            container.innerHTML = `<p style="text-align:center; padding:20px; color:red;">Invalid server response.</p>`;
            return;
        }

        container.innerHTML = "";

        if (data.length === 0) {
            container.innerHTML = `<p style="text-align:center; padding:20px; color:#999;">No orders found matching filters.</p>`;
            return;
        }

        data.forEach(function(order) {
            const originalStatus = order.status || "Pending";
            const statusLower = originalStatus.toLowerCase().trim();
            const statusClass = "status-" + statusLower.replace(/\s+/g, "-");
            const statusStyle = getStatusBadgeStyle(originalStatus);
            const deposit = Number(order.deposit || 0);
            const orderId = Number(order.id || 0);

            container.innerHTML += `
                <div class="order-card">
                    <span class="status-badge ${statusClass}" style="${statusStyle}">${escapeHtml(originalStatus)}</span>
                    <div class="order-info">
                        <h4>${escapeHtml(order.serviceName || "Service")}</h4>
                        <p><strong>Problem:</strong> ${escapeHtml(order.problem || "-")}</p>
                        <p><small>📅 ${escapeHtml(order.date || order.bookingdate || "-")}</small></p>
                        ${statusLower === "report" ? `<p class="admin-note">This booking is under report review by admin.</p>` : `<p class="admin-note">If you are not satisfied with this order, you can chat with admin for support.</p>`}
                    </div>
                    <div class="order-footer">
                        <span>Deposit: <b>RM ${deposit.toFixed(2)}</b></span>
                        <div class="button-group">
                            <button class="btn-detail" onclick="viewDetail(${orderId}, '${escapeJs(originalStatus)}')">Details</button>
                        </div>
                    </div>
                </div>
            `;
        });

    } catch (e) {
        console.error("Fetch error:", e);
        container.innerHTML = `<p style="text-align:center; padding:20px; color:red;">Error loading data.</p>`;
    }
}

function getStatusBadgeStyle(status) {
    const clean = String(status || "").toLowerCase().trim();

    if (clean === "report") {
        return "background:#fef3c7;color:#92400e;border:1px solid #fde68a;";
    }

    if (clean === "pending") {
        return "background:#fff7d6;color:#9a6700;border:1px solid #ffe08a;";
    }

    if (clean === "payment failed" || clean === "failed" || clean === "rejected" || clean === "reject" || clean === "cancelled") {
        return "background:#ffe4e6;color:#be123c;border:1px solid #fecdd3;";
    }

    if (clean === "emergency") {
        return "background:#fee2e2;color:#dc2626;border:1px solid #fecaca;";
    }

    if (clean === "accepted") {
        return "background:#dbeafe;color:#1d4ed8;border:1px solid #bfdbfe;";
    }

    if (clean === "on the way" || clean === "on theway" || clean === "ontheway") {
        return "background:#e0f2fe;color:#0369a1;border:1px solid #bae6fd;";
    }

    if (clean === "arrived") {
        return "background:#ccfbf1;color:#0f766e;border:1px solid #99f6e4;";
    }

    if (clean === "started" || clean === "work started") {
        return "background:#ffedd5;color:#c2410c;border:1px solid #fed7aa;";
    }

    if (clean === "second payment" || clean === "second_payment") {
        return "background:#ede9fe;color:#6d28d9;border:1px solid #ddd6fe;";
    }

    if (clean === "completed") {
        return "background:#dcfce7;color:#15803d;border:1px solid #bbf7d0;";
    }

    if (clean === "rated") {
        return "background:#f3e8ff;color:#7e22ce;border:1px solid #e9d5ff;";
    }

    return "background:#e5e7eb;color:#374151;border:1px solid #d1d5db;";
}

function viewDetail(id, status) {
    const s = String(status || "").toLowerCase().trim();
    const trackingStatuses = ["accepted", "on the way", "arrived", "started", "second payment", "completed", "rated"];

    if (s === "report") {
        openBookingReportDetails(id);
        return;
    }

    if (trackingStatuses.includes(s)) {
        sessionStorage.setItem("tukangnow_chat_view", "customer");
        window.location.href = `trackingvendor.html?id=${encodeURIComponent(id)}&view=customer`;
        return;
    }

    let title = "";
    let message = "";
    let isSuccessType = false;
    let showCancelButton = false;
    let cancelButtonText = "";
    let confirmButtonText = "Continue";

    if (s === "rejected" || s === "reject" || s === "cancelled" || s === "payment failed") {
        title = "Failed!";
        message = `Your order #${id} was rejected, cancelled, or payment failed. If you are not satisfied, you can chat with admin for support.`;
        isSuccessType = false;
        showCancelButton = true;
        cancelButtonText = "Chat Admin";
        confirmButtonText = "Understood";
    } else if (s === "pending") {
        title = "Looking for Vendor";
        message = `Your order #${id} has been notified to vendors. Please wait for a vendor to accept your order. If you are not satisfied, you can chat with admin for support.`;
        isSuccessType = true;
        showCancelButton = true;
        cancelButtonText = "Chat Admin";
    } else if (s === "emergency") {
        title = "Emergency Request";
        message = `Your order #${id} is currently in broadcast. Please wait for an emergency vendor to accept your request. If you are not satisfied, you can chat with admin for support.`;
        isSuccessType = true;
        showCancelButton = true;
        cancelButtonText = "Chat Admin";
    } else {
        title = "Status";
        message = `Status for Order #${id} is ${status}. If you are not satisfied, you can chat with admin for support.`;
        isSuccessType = true;
        showCancelButton = true;
        cancelButtonText = "Chat Admin";
    }

    const themeClass = isSuccessType ? "success-theme" : "error-theme";

    Swal.fire({
        html: `
            <div class="swal-custom-icon-circle ${themeClass}"></div>
            <div class="swal-custom-title">${title}</div>
            <div class="swal-custom-html">${message}</div>
        `,
        showCancelButton: showCancelButton,
        showConfirmButton: true,
        confirmButtonText: confirmButtonText,
        cancelButtonText: cancelButtonText,
        customClass: {
            popup: "swal-custom-popup",
            actions: "swal-custom-actions-container",
            confirmButton: "swal-custom-btn-confirm",
            cancelButton: "swal-custom-btn-cancel"
        },
        buttonsStyling: false
    }).then(function(result) {
        if (result.dismiss === Swal.DismissReason.cancel) {
            startAdminChatSession(id);
        }
    });
}

function openBookingReportDetails(bookingId) {
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
            window.location.href = data.redirect || "index.html";
            return;
        }

        if (data && data.status === "success" && data.report) {
            showReportDetailsPopup(data.report);
            return;
        }

        if (data && data.status === "reported_account") {
            Swal.fire({
                html: `
                    <div class="swal-custom-icon-circle error-theme"></div>
                    <div class="swal-custom-title">Report Under Review</div>
                    <div class="swal-custom-html">${escapeHtml(data.message || "This booking has been reported and is under admin review.")}</div>
                `,
                confirmButtonText: "Continue",
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
            html: `
                <div class="swal-custom-icon-circle error-theme"></div>
                <div class="swal-custom-title">Report Details</div>
                <div class="swal-custom-html">${escapeHtml(data.message || "Report details are not available.")}</div>
            `,
            confirmButtonText: "Continue",
            customClass: {
                popup: "swal-custom-popup",
                actions: "swal-custom-actions-container",
                confirmButton: "swal-custom-btn-confirm"
            },
            buttonsStyling: false
        });
    })
    .catch(function() {
        Swal.fire({
            html: `
                <div class="swal-custom-icon-circle error-theme"></div>
                <div class="swal-custom-title">Failed</div>
                <div class="swal-custom-html">Network connection or server processing error.</div>
            `,
            confirmButtonText: "Continue",
            customClass: {
                popup: "swal-custom-popup",
                actions: "swal-custom-actions-container",
                confirmButton: "swal-custom-btn-confirm"
            },
            buttonsStyling: false
        });
    });
}

function showReportDetailsPopup(report) {
    Swal.fire({
        html: `
            <div class="swal-custom-icon-circle success-theme"></div>
            <div class="swal-custom-title">Report Details</div>
            <div class="swal-custom-html" style="text-align:left;">
                <p><strong>Booking ID:</strong> #${Number(report.bookingId || 0)}</p>
                <p><strong>Report Type:</strong> ${escapeHtml(report.reportOption || "-")}</p>
                <p><strong>Report Status:</strong> ${escapeHtml(report.status || "-")}</p>
                <p><strong>Action Taken:</strong> ${escapeHtml(report.actionTaken || "-")}</p>
                <p><strong>Action Date:</strong> ${escapeHtml(report.actionDate || "-")}</p>
                <p><strong>Admin Note:</strong> ${escapeHtml(report.adminNote || "-")}</p>
                <p><strong>Explanation:</strong> ${escapeHtml(report.explanation || "-")}</p>
            </div>
        `,
        confirmButtonText: "Continue",
        customClass: {
            popup: "swal-custom-popup",
            actions: "swal-custom-actions-container",
            confirmButton: "swal-custom-btn-confirm"
        },
        buttonsStyling: false
    });
}

function startAdminChatSession(orderId) {
    currentChatOrderId = Number(orderId);
    isChatSessionActive = true;

    const widget = document.getElementById("adminChatWidget");
    const msgArea = document.getElementById("chatMessages");

    msgArea.innerHTML = "";
    document.getElementById("chatTitle").innerText = `Order #${currentChatOrderId} - Admin Support`;
    document.getElementById("chatInput").disabled = false;
    widget.style.display = "flex";

    fetchChatMessages();
    refreshChatTimer();

    clearInterval(chatPollingTimer);
    chatPollingTimer = setInterval(function() {
        if (isChatSessionActive && currentChatOrderId) {
            fetchChatMessages();
        }
    }, 3000);
}

function fetchChatMessages() {
    if (!currentChatOrderId) {
        return;
    }

    fetch(`${baseURL}/AdminChatServlet?booking_id=${encodeURIComponent(currentChatOrderId)}&t=${Date.now()}`, {
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
            window.location.href = data.redirect || "index.html";
            return;
        }

        if (data && data.status === "error") {
            renderChatSystemMessage(data.message || "Unable to load chat.");
            return;
        }

        if (Array.isArray(data)) {
            renderChatMessages(data);
        }
    })
    .catch(function() {
        renderChatSystemMessage("Unable to load chat messages.");
    });
}

function renderChatMessages(chats) {
    const msgArea = document.getElementById("chatMessages");
    msgArea.innerHTML = "";

    if (!chats || chats.length === 0) {
        addChatBubble("system", "Admin support chat is open. Send your message and wait for admin reply.", "");
        return;
    }

    chats.forEach(function(chat) {
        const sender = String(chat.sender || "").toLowerCase().trim();

        if (sender === "customer") {
            addChatBubble("customer", chat.message || "", chat.createdAt || "");
        } else if (sender === "admin") {
            addChatBubble("admin", chat.message || "", chat.createdAt || "");
        } else {
            addChatBubble("system", chat.message || "", chat.createdAt || "");
        }
    });
}

function renderChatSystemMessage(message) {
    const msgArea = document.getElementById("chatMessages");
    msgArea.innerHTML = "";
    addChatBubble("system", message, "");
}

function addChatBubble(sender, text, time) {
    const msgArea = document.getElementById("chatMessages");
    const bubble = document.createElement("div");

    bubble.classList.add("chat-bubble", sender);

    const safeText = escapeHtml(text);
    const safeTime = escapeHtml(time || "");

    if (safeTime !== "") {
        bubble.innerHTML = `
            <span class="chat-text">${safeText}</span>
            <span class="chat-time">${safeTime}</span>
        `;
    } else {
        bubble.innerHTML = `
            <span class="chat-text">${safeText}</span>
        `;
    }

    msgArea.appendChild(bubble);
    msgArea.scrollTop = msgArea.scrollHeight;
}

function sendChatMessage() {
    if (!isChatSessionActive || !currentChatOrderId) {
        return;
    }

    const input = document.getElementById("chatInput");
    const text = input.value.trim();

    if (text === "") {
        return;
    }

    input.disabled = true;

    fetch(`${baseURL}/AdminChatServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        body: JSON.stringify({
            booking_id: currentChatOrderId,
            message: text
        })
    })
    .then(function(response) {
        return response.json();
    })
    .then(function(data) {
        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || "index.html";
            return;
        }

        if (data && data.status === "error") {
            Swal.fire({
                html: `
                    <div class="swal-custom-icon-circle error-theme"></div>
                    <div class="swal-custom-title">Failed</div>
                    <div class="swal-custom-html">${escapeHtml(data.message || "Unable to send message.")}</div>
                `,
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

        input.value = "";
        fetchChatMessages();
        refreshChatTimer();
    })
    .catch(function() {
        Swal.fire({
            html: `
                <div class="swal-custom-icon-circle error-theme"></div>
                <div class="swal-custom-title">Failed</div>
                <div class="swal-custom-html">Network connection or server processing error.</div>
            `,
            confirmButtonText: "OK",
            customClass: {
                popup: "swal-custom-popup",
                actions: "swal-custom-actions-container",
                confirmButton: "swal-custom-btn-confirm"
            },
            buttonsStyling: false
        });
    })
    .finally(function() {
        input.disabled = false;
        input.focus();
    });
}

function handleChatKeyPress(event) {
    if (event.key === "Enter") {
        sendChatMessage();
    }
}

function refreshChatTimer() {
    clearTimeout(chatTimer);

    if (isChatSessionActive) {
        chatTimer = setTimeout(function() {
            closeChatByInactivity();
        }, TIME_LIMIT);
    }
}

function closeChatByInactivity() {
    if (!isChatSessionActive) {
        return;
    }

    addChatBubble("system", "Chat closed automatically due to 30 minutes of inactivity.", "");
    endChatSessionDisplay();
}

function manualCloseChat() {
    Swal.fire({
        html: `
            <div class="swal-custom-icon-circle error-theme"></div>
            <div class="swal-custom-title">Close Session?</div>
            <div class="swal-custom-html">Are you sure you want to exit and close this chat session?</div>
        `,
        showCancelButton: true,
        showConfirmButton: true,
        confirmButtonText: "Yes, Close",
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
            endChatSessionDisplay();
        }
    });
}

function endChatSessionDisplay() {
    isChatSessionActive = false;
    currentChatOrderId = null;

    clearTimeout(chatTimer);
    clearInterval(chatPollingTimer);

    document.getElementById("chatInput").disabled = true;
    document.getElementById("adminChatWidget").style.display = "none";
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