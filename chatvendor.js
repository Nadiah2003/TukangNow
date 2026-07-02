const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let bookingId = "";
let viewerRole = "";
let selectedMsgId = 0;
let selectedMsgText = "";
let isEditing = false;
let pressTimer = null;
let loadTimer = null;

document.addEventListener("DOMContentLoaded", function() {
    const urlParams = new URLSearchParams(window.location.search);
    bookingId = urlParams.get("id") || "";
    viewerRole = resolveInitialView();

    if (!bookingId) {
        window.location.href = "order.html";
        return;
    }

    const backToTracking = document.getElementById("backToTracking");
    const sendMsgBtn = document.getElementById("sendMsgBtn");
    const messageInput = document.getElementById("messageInput");
    const editMenuBtn = document.getElementById("editMenuBtn");
    const deleteMenuBtn = document.getElementById("deleteMenuBtn");
    const cancelMenuBtn = document.getElementById("cancelMenuBtn");
    const modalContinueBtn = document.getElementById("modalContinueBtn");

    if (backToTracking) {
        backToTracking.addEventListener("click", function() {
            goBackToTracking();
        });
    }

    if (sendMsgBtn) {
        sendMsgBtn.addEventListener("click", function() {
            sendMessage();
        });
    }

    if (messageInput) {
        messageInput.addEventListener("keypress", function(e) {
            if (e.key === "Enter") {
                sendMessage();
            }
        });
    }

    if (editMenuBtn) {
        editMenuBtn.addEventListener("click", function() {
            closeActionMenu();
            isEditing = true;
            messageInput.value = selectedMsgText;
            messageInput.classList.add("editing-mode");
            messageInput.placeholder = "Editing message...";
            messageInput.focus();
        });
    }

    if (deleteMenuBtn) {
        deleteMenuBtn.addEventListener("click", function() {
            deleteMessage();
        });
    }

    if (cancelMenuBtn) {
        cancelMenuBtn.addEventListener("click", function() {
            closeActionMenu();
        });
    }

    if (modalContinueBtn) {
        modalContinueBtn.addEventListener("click", function() {
            document.getElementById("messageModal").style.display = "none";
        });
    }

    loadChatInfo();
    loadMessages();

    loadTimer = setInterval(function() {
        if (!isTyping()) {
            loadMessages();
        }
    }, 3000);
});

function resolveInitialView() {
    const urlParams = new URLSearchParams(window.location.search);
    const urlView = String(urlParams.get("view") || "").trim().toLowerCase();

    if (urlView === "vendor" || urlView === "customer") {
        sessionStorage.setItem("tukangnow_chat_view", urlView);
        return urlView;
    }

    const savedView = String(sessionStorage.getItem("tukangnow_chat_view") || "").trim().toLowerCase();

    if (savedView === "vendor" || savedView === "customer") {
        return savedView;
    }

    const referrer = String(document.referrer || "").toLowerCase();

    if (referrer.includes("job.html") || referrer.includes("homevendor.html") || referrer.includes("profilevendor.html") || referrer.includes("walletvendor.html")) {
        sessionStorage.setItem("tukangnow_chat_view", "vendor");
        return "vendor";
    }

    if (referrer.includes("order.html") || referrer.includes("homecustomer.html") || referrer.includes("profilecustomer.html") || referrer.includes("wallet.html")) {
        sessionStorage.setItem("tukangnow_chat_view", "customer");
        return "customer";
    }

    return "";
}

function getRequestedView() {
    return viewerRole || resolveInitialView();
}

function goBackToTracking() {
    const view = getRequestedView();
    const viewQuery = view ? `&view=${encodeURIComponent(view)}` : "";
    window.location.href = `trackingvendor.html?id=${encodeURIComponent(bookingId)}${viewQuery}`;
}

function isTyping() {
    const active = document.activeElement;
    return active && active.id === "messageInput" && active.value.trim() !== "";
}

async function loadChatInfo() {
    try {
        const view = getRequestedView();

        const response = await fetch(`${baseURL}/ChatServlet?action=init&booking_id=${encodeURIComponent(bookingId)}&view=${encodeURIComponent(view)}`, {
            method: "GET",
            credentials: "same-origin"
        });

        const data = await response.json();

        if (handleSessionExpired(data)) {
            return;
        }

        if (data.status === "success") {
            viewerRole = data.viewer_role || view;
            sessionStorage.setItem("tukangnow_chat_view", viewerRole);
            document.getElementById("chatPartnerName").innerText = data.partner_name || "Chat";
            document.getElementById("chatRoleText").innerText = viewerRole === "vendor" ? "Customer Chat" : "Vendor Chat";
            return;
        }

        showMessageModal("Failed!", data.message || "Unable to load chat information.", "error");

    } catch (e) {
        showMessageModal("Failed!", "System connection error.", "error");
    }
}

async function loadMessages() {
    try {
        const view = getRequestedView();

        const response = await fetch(`${baseURL}/ChatServlet?action=fetch&booking_id=${encodeURIComponent(bookingId)}&view=${encodeURIComponent(view)}`, {
            method: "GET",
            credentials: "same-origin"
        });

        const data = await response.json();

        if (handleSessionExpired(data)) {
            return;
        }

        if (data.status !== "success") {
            return;
        }

        if (data.viewer_role) {
            viewerRole = data.viewer_role;
            sessionStorage.setItem("tukangnow_chat_view", viewerRole);
        }

        renderMessages(data.messages || []);

    } catch (e) {
        console.error("Error loading chat messages", e);
    }
}

function renderMessages(messages) {
    const chatBox = document.getElementById("chatBox");
    const isAtBottom = chatBox.scrollHeight - chatBox.clientHeight <= chatBox.scrollTop + 100;

    if (!messages || messages.length === 0) {
        chatBox.innerHTML = `<div class="empty-chat">No messages yet. Start your conversation.</div>`;
        return;
    }

    let chatContent = "";

    messages.forEach(function(msg) {
        const isMine = String(msg.sender_type || "").toLowerCase() === String(viewerRole || "").toLowerCase();
        const senderClass = isMine ? "mine" : "other";
        let displayMessage = "";
        let isDeletedClass = "";
        let activeDataText = "";

        if (Number(msg.is_deleted || 0) === 1) {
            displayMessage = "🚫 <i>This message was deleted</i>";
            isDeletedClass = "msg-deleted";
        } else if (Number(msg.is_edited || 0) === 1) {
            displayMessage = `${escapeHTML(msg.edited_message || "")}<span class="edited-text">(edited)</span>`;
            activeDataText = escapeAttr(msg.edited_message || "");
        } else {
            displayMessage = escapeHTML(msg.message || "");
            activeDataText = escapeAttr(msg.message || "");
        }

        const canActionAttr = msg.can_action ? `data-can-action="true"` : `data-can-action="false"`;

        chatContent += `
            <div class="msg ${senderClass} ${isDeletedClass}" data-msg-id="${Number(msg.id || 0)}" data-text="${activeDataText}" ${canActionAttr}>
                ${displayMessage}
                <span class="msg-time">${escapeHTML(msg.time_sent || "")}</span>
            </div>
        `;
    });

    chatBox.innerHTML = chatContent;

    if (isAtBottom || chatBox.scrollTop === 0) {
        chatBox.scrollTop = chatBox.scrollHeight;
    }

    bindLongPressEvents();
}

function bindLongPressEvents() {
    const activeMessages = document.querySelectorAll('.msg[data-can-action="true"]');

    activeMessages.forEach(function(msgEl) {
        const startPress = function() {
            pressTimer = setTimeout(function() {
                selectedMsgId = parseInt(msgEl.getAttribute("data-msg-id")) || 0;
                selectedMsgText = decodeHtml(msgEl.getAttribute("data-text") || "");
                openActionMenu();
            }, 600);
        };

        const cancelPress = function() {
            clearTimeout(pressTimer);
        };

        msgEl.addEventListener("mousedown", startPress);
        msgEl.addEventListener("touchstart", startPress);
        msgEl.addEventListener("mouseup", cancelPress);
        msgEl.addEventListener("mouseleave", cancelPress);
        msgEl.addEventListener("touchend", cancelPress);
    });
}

function openActionMenu() {
    document.getElementById("actionMenu").style.display = "flex";
}

function closeActionMenu() {
    document.getElementById("actionMenu").style.display = "none";
}

async function sendMessage() {
    const messageInput = document.getElementById("messageInput");
    const text = messageInput.value.trim();

    if (text === "") {
        return;
    }

    if (isEditing) {
        await updateMessage(text);
        return;
    }

    messageInput.value = "";

    const formData = new URLSearchParams();
    formData.append("action", "send");
    formData.append("booking_id", bookingId);
    formData.append("view", getRequestedView());
    formData.append("message", text);

    try {
        const response = await fetch(`${baseURL}/ChatServlet`, {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: formData.toString()
        });

        const data = await response.json();

        if (handleSessionExpired(data)) {
            return;
        }

        if (data.status === "success") {
            await loadMessages();
            const chatBox = document.getElementById("chatBox");
            chatBox.scrollTop = chatBox.scrollHeight;
            return;
        }

        showMessageModal("Failed!", data.message || "Message failed to send.", "error");

    } catch (e) {
        showMessageModal("Failed!", "System connection error.", "error");
    }
}

async function updateMessage(text) {
    if (selectedMsgId <= 0) {
        return;
    }

    const formData = new URLSearchParams();
    formData.append("action", "update_single");
    formData.append("booking_id", bookingId);
    formData.append("view", getRequestedView());
    formData.append("msg_id", selectedMsgId);
    formData.append("message", text);

    try {
        const response = await fetch(`${baseURL}/ChatServlet`, {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: formData.toString()
        });

        const data = await response.json();

        if (handleSessionExpired(data)) {
            return;
        }

        if (data.status === "success") {
            isEditing = false;
            selectedMsgId = 0;
            selectedMsgText = "";
            const messageInput = document.getElementById("messageInput");
            messageInput.value = "";
            messageInput.classList.remove("editing-mode");
            messageInput.placeholder = "Type a message...";
            await loadMessages();
            return;
        }

        showMessageModal("Failed!", data.message || "Message update failed.", "error");

    } catch (e) {
        showMessageModal("Failed!", "System connection error.", "error");
    }
}

async function deleteMessage() {
    closeActionMenu();

    if (selectedMsgId <= 0) {
        return;
    }

    const formData = new URLSearchParams();
    formData.append("action", "delete_single");
    formData.append("booking_id", bookingId);
    formData.append("view", getRequestedView());
    formData.append("msg_id", selectedMsgId);

    try {
        const response = await fetch(`${baseURL}/ChatServlet`, {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: formData.toString()
        });

        const data = await response.json();

        if (handleSessionExpired(data)) {
            return;
        }

        if (data.status === "success") {
            selectedMsgId = 0;
            selectedMsgText = "";
            await loadMessages();
            return;
        }

        showMessageModal("Failed!", data.message || "Message delete failed.", "error");

    } catch (e) {
        showMessageModal("Failed!", "System connection error.", "error");
    }
}

function showMessageModal(title, message, type) {
    const modal = document.getElementById("messageModal");
    const icon = document.getElementById("modalIcon");

    document.getElementById("modalTitle").innerText = title;
    document.getElementById("modalMessage").innerText = message;

    icon.className = "modal-icon";

    if (type === "success") {
        icon.classList.add("success");
        icon.innerText = "✓";
    } else {
        icon.innerText = "✕";
    }

    modal.style.display = "flex";
}

function handleSessionExpired(data) {
    if (data && data.status === "session_expired") {
        window.location.href = data.redirect || "login.html";
        return true;
    }

    return false;
}

function escapeHTML(value) {
    return String(value || "").replace(/[&<>'"]/g, function(tag) {
        return {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            "'": "&#39;",
            '"': "&quot;"
        }[tag] || tag;
    });
}

function escapeAttr(value) {
    return escapeHTML(value).replaceAll("`", "&#096;");
}

function decodeHtml(value) {
    const textarea = document.createElement("textarea");
    textarea.innerHTML = value;
    return textarea.value;
}