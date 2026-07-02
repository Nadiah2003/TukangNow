const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let activeEventsData = [];
let currentNotifications = [];
let customerNotificationTimer = null;
let lastSecondPaymentPushKey = localStorage.getItem("tukangnow_customer_second_payment_push_key") || "";

document.addEventListener("DOMContentLoaded", function() {
    bindFilterControls();
    requestBrowserNotificationPermission();
    loadHomeCustomerData();

    customerNotificationTimer = setInterval(function() {
        fetchNotifications(false);
    }, 8000);
});

function bindFilterControls() {
    const filterIds = [
        "electricianRange",
        "electricianSort",
        "plumberRange",
        "plumberSort",
        "lawnRange",
        "lawnSort"
    ];

    filterIds.forEach(function(id) {
        const element = document.getElementById(id);

        if (element) {
            element.addEventListener("change", loadHomeCustomerData);
        }
    });
}

function loadHomeCustomerData() {
    const params = new URLSearchParams();

    params.append("electricalRange", getSelectValue("electricianRange", "50"));
    params.append("electricalSort", getSelectValue("electricianSort", "asc"));
    params.append("plumberRange", getSelectValue("plumberRange", "50"));
    params.append("plumberSort", getSelectValue("plumberSort", "asc"));
    params.append("lawnRange", getSelectValue("lawnRange", "50"));
    params.append("lawnSort", getSelectValue("lawnSort", "asc"));

    fetch(`${baseURL}/api/home-cust?${params.toString()}&t=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "session_expired") {
            showModal("Session Debug", data.message || "Session expired. Redirect is disabled for debugging.");
            return;
        }

        if (data.status === "success") {
            updateProfileUI(data);

            activeEventsData = data.events || [];
            renderEventsList(activeEventsData);

            renderCategoryList(data.electricians, "electricianContainer", getSelectValue("electricianRange", "50"));
            renderCategoryList(data.plumbers, "plumberContainer", getSelectValue("plumberRange", "50"));
            renderCategoryList(data.lawns, "lawnContainer", getSelectValue("lawnRange", "50"));

            updateNotificationsFromData(data);
        } else {
            showModal("Failed!", data.message || "Unable to load home data.");
        }
    })
    .catch(err => {
        console.error("Fetch error:", err);
        showModal("Failed!", "Communication with server failed.");
    });
}

function getSelectValue(id, fallback) {
    const element = document.getElementById(id);

    if (!element || !element.value) {
        return fallback;
    }

    return element.value;
}

function fetchNotifications(showError = false) {
    fetch(`${baseURL}/api/home-cust?action=notifications&t=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "session_expired") {
            console.warn(data.message || "Session expired. Redirect is disabled for debugging.");
            if (showError) {
                showModal("Session Debug", data.message || "Session expired. Redirect is disabled for debugging.");
            }
            return;
        }

        if (data.status === "success") {
            updateNotificationsFromData(data);
        } else if (showError) {
            showModal("Failed!", data.message || "Unable to load notifications.");
        }
    })
    .catch(err => {
        console.error("Notification load error:", err);
        if (showError) {
            showModal("Failed!", "Unable to load notifications.");
        }
    });
}

function updateNotificationsFromData(data) {
    currentNotifications = data.notifications || [];

    const badge = document.getElementById("notifCountBadge");
    const unreadCount = Number(data.unreadNotificationsCount || 0);

    if (badge) {
        if (unreadCount > 0) {
            badge.innerText = unreadCount > 99 ? "99+" : unreadCount;
            badge.classList.remove("hidden");
        } else {
            badge.classList.add("hidden");
        }
    }

    renderNotificationList(currentNotifications);
    triggerSecondPaymentPush(currentNotifications);
}

function renderNotificationList(notifications) {
    const container = document.getElementById("notifListContainer");

    if (!container) {
        return;
    }

    const visibleNotifications = (notifications || []).filter(notif => Number(notif.count || 0) > 0);

    if (visibleNotifications.length === 0) {
        container.innerHTML = '<div class="notif-empty-state">No new notifications. Your activities are up to date!</div>';
        return;
    }

    container.innerHTML = visibleNotifications.map(notif => {
        const type = String(notif.type || "").toLowerCase();
        let cardClass = "notif-item-card";

        if (type === "wallet") {
            cardClass += " balance-type";
        } else if (type === "second_payment") {
            cardClass += " second-payment-type";
        } else if (type === "report_status") {
            cardClass += " report-type";
        } else if (type === "admin_chat" || type === "vendor_chat") {
            cardClass += " chat-type";
        }

        return `
            <div class="${cardClass}" onclick="handleNotificationClick('${escapeJs(notif.link || "")}', '${escapeJs(type)}')">
                <div class="notif-icon-box">${escapeHtml(notif.icon || "🔔")}</div>
                <div class="notif-content">
                    <div class="notif-title-row">
                        <p class="notif-item-title">${escapeHtml(notif.title || "Notification")}</p>
                        <span class="notif-count-pill">${Number(notif.count || 0)}</span>
                    </div>
                    <p class="notif-item-msg">${escapeHtml(notif.message || "-")}</p>
                    <div class="notif-item-date">${escapeHtml(notif.date || "")}</div>
                </div>
            </div>
        `;
    }).join("");
}

function handleNotificationClick(link, type) {
    const cleanLink = String(link || "").trim();

    if (cleanLink !== "") {
        window.location.href = cleanLink;
    }
}

function openNotificationModal() {
    const modal = document.getElementById("notificationModal");

    if (modal) {
        modal.classList.remove("hidden");
        fetchNotifications(true);
    }
}

function closeNotificationModal() {
    const modal = document.getElementById("notificationModal");

    if (modal) {
        modal.classList.add("hidden");
    }
}

function requestBrowserNotificationPermission() {
    if (!("Notification" in window)) {
        return;
    }

    if (Notification.permission === "default") {
        Notification.requestPermission().catch(function() {});
    }
}

function triggerSecondPaymentPush(notifications) {
    const secondPaymentNotification = (notifications || []).find(notif => String(notif.type || "").toLowerCase() === "second_payment" && Number(notif.count || 0) > 0);

    if (!secondPaymentNotification) {
        return;
    }

    const pushKey = `${Number(secondPaymentNotification.count || 0)}_${String(secondPaymentNotification.date || "")}_${String(secondPaymentNotification.message || "")}`;

    if (pushKey === lastSecondPaymentPushKey) {
        return;
    }

    lastSecondPaymentPushKey = pushKey;
    localStorage.setItem("tukangnow_customer_second_payment_push_key", pushKey);

    if ("vibrate" in navigator) {
        navigator.vibrate([500, 180, 500]);
    }

    if ("Notification" in window && Notification.permission === "granted") {
        const notification = new Notification(secondPaymentNotification.title || "Second Payment Required", {
            body: secondPaymentNotification.message || "Your booking has a second payment request.",
            icon: "image/logotukang.png"
        });

        notification.onclick = function() {
            window.focus();
            window.location.href = secondPaymentNotification.link || "myorder.html";
        };
    }
}

function updateProfileUI(data) {
    document.getElementById("customerName").innerText = data.customerName || "Guest";

    const walletLabel = document.getElementById("walletBalanceLabel");

    if (walletLabel) {
        if (data.walletBalance !== undefined && data.walletBalance !== null) {
            walletLabel.innerText = `RM ${data.walletBalance}`;
        } else {
            walletLabel.innerText = "RM 0.00";
        }
    }

    const rewardsDisplay = document.querySelector(".rewards-icon")?.previousElementSibling?.querySelector(".card-value");

    if (rewardsDisplay) {
        if (data.rewardsPoints !== undefined && data.rewardsPoints !== null) {
            rewardsDisplay.innerText = `${Number(data.rewardsPoints).toLocaleString()} pts`;
        } else {
            rewardsDisplay.innerText = "0 pts";
        }
    }

    const customerImg = document.getElementById("customerImg");

    if (customerImg) {
        customerImg.src = resolveProfilePath(data.customerImg);

        customerImg.onerror = function() {
            customerImg.src = "image/profile.png";
        };
    }
}

function renderEventsList(events) {
    const container = document.getElementById("eventsContainer");

    if (!container) {
        return;
    }

    container.innerHTML = "";

    if (events && events.length > 0) {
        events.forEach(ev => {
            const imgValue = ev.img || "";
            const fullImgPath = imgValue.includes("http") ? imgValue : `${baseURL}/${imgValue}`;
            const safeImgPath = escapeAttribute(fullImgPath);

            container.innerHTML += `
                <div class="promo-banner" onclick="openEventModal(${Number(ev.id)})">
                    <div class="promo-banner-bg" style="background-image:url('${safeImgPath}')"></div>
                    <div class="promo-banner-overlay"></div>
                    <img src="${safeImgPath}" alt="Event Banner" onerror="this.src='https://via.placeholder.com/400x150?text=TukangNow+Promo'">
                </div>`;
        });
    } else {
        container.innerHTML = '<p class="empty-msg">No active events today.</p>';
    }
}

function openEventModal(eventId) {
    const eventObj = activeEventsData.find(e => Number(e.id) === Number(eventId));

    if (!eventObj) {
        return;
    }

    const modal = document.getElementById("eventModal");
    const modalImg = document.getElementById("modalEventImg");
    const modalBadge = document.getElementById("modalEventBadge");
    const modalTitle = document.getElementById("modalEventTitle");
    const modalDuration = document.getElementById("modalEventDuration");
    const modalDesc = document.getElementById("modalEventDesc");
    const voucherSection = document.getElementById("modalVoucherSection");
    const voucherCode = document.getElementById("modalVoucherCode");
    const copyBtn = document.getElementById("copyVoucherBtn");

    if (!modal) {
        return;
    }

    const imgValue = eventObj.img || "";
    const fullImgPath = imgValue.includes("http") ? imgValue : `${baseURL}/${imgValue}`;

    modalImg.src = fullImgPath;

    if (modalImg.parentElement) {
        modalImg.parentElement.style.backgroundImage = `url("${fullImgPath.replaceAll('"', "%22")}")`;
    }

    modalImg.onerror = function() {
        this.src = "https://via.placeholder.com/400x180?text=TukangNow+Promo";
        if (this.parentElement) {
            this.parentElement.style.backgroundImage = `url("https://via.placeholder.com/400x180?text=TukangNow+Promo")`;
        }
    };

    modalTitle.innerText = eventObj.title;
    modalDesc.innerText = eventObj.description;
    modalDuration.innerText = `Valid from ${eventObj.startDate} until ${eventObj.endDate}`;

    if (eventObj.discountPercentage > 0) {
        modalBadge.innerText = `${eventObj.discountPercentage}% OFF`;
        modalBadge.classList.remove("hidden");
    } else {
        modalBadge.classList.add("hidden");
    }

    if (eventObj.isRedeemed && eventObj.discountCode && eventObj.discountCode.trim() !== "") {
        voucherCode.innerText = eventObj.discountCode;
        copyBtn.innerText = "Copy";
        copyBtn.disabled = false;
        copyBtn.onclick = copyVoucherCode;
        voucherSection.classList.remove("hidden");
    } else {
        voucherCode.innerText = "Locked (Redeem needed)";
        copyBtn.innerText = "Redeem";
        copyBtn.disabled = false;
        copyBtn.onclick = function() {
            redeemVoucher(eventId);
        };
        voucherSection.classList.remove("hidden");
    }

    modal.classList.remove("hidden");
}

function closeEventModal() {
    const modal = document.getElementById("eventModal");

    if (modal) {
        modal.classList.add("hidden");
    }
}

function copyVoucherCode() {
    const codeText = document.getElementById("modalVoucherCode").innerText;
    const copyBtn = document.getElementById("copyVoucherBtn");

    if (!codeText || codeText === "-" || codeText === "Locked (Redeem needed)") {
        showModal("Notice", "You need to redeem this voucher using rewards points first!");
        return;
    }

    navigator.clipboard.writeText(codeText).then(() => {
        copyBtn.innerText = "Copied!";

        setTimeout(() => {
            copyBtn.innerText = "Copy";
        }, 2000);
    }).catch(err => {
        console.error("Failed to copy voucher code:", err);
    });
}

function redeemVoucher(eventId) {
    const copyBtn = document.getElementById("copyVoucherBtn");

    copyBtn.innerText = "Processing...";
    copyBtn.disabled = true;

    fetch(`${baseURL}/api/redeem`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: `reward_id=${eventId}`
    })
    .then(response => response.json())
    .then(data => {
        copyBtn.disabled = false;

        if (data.status === "success") {
            showModal("Success!", data.message);
            closeEventModal();
            window.location.reload();
        } else {
            showModal("Failed!", data.message);
            copyBtn.innerText = "Redeem";
        }
    })
    .catch(err => {
        console.error("Redeem error:", err);
        copyBtn.disabled = false;
        copyBtn.innerText = "Redeem";
        showModal("Failed!", "Communication with server failed.");
    });
}

function renderCategoryList(vendors, containerId, rangeValue) {
    const container = document.getElementById(containerId);

    if (!container) {
        return;
    }

    container.innerHTML = "";

    if (vendors && vendors.length > 0) {
        vendors.forEach(v => {
            const imgPath = resolveProfilePath(v.img || v.profilePath || v.profile_path);
            const safeImgPath = escapeAttribute(imgPath);
            const distanceText = v.distanceKm !== undefined && v.distanceKm !== null ? `${Number(v.distanceKm).toFixed(2)} KM away` : "Distance unavailable";

            container.innerHTML += `
                <div class="vendor-card">
                    <div class="vendor-img-frame" style="width:100%;height:100px;border-radius:10px;overflow:hidden;position:relative;margin-bottom:10px;background:#eef5ff;">
                        <div class="vendor-img-blur" style="position:absolute;inset:0;background-image:url('${safeImgPath}');background-size:cover;background-position:center;background-repeat:no-repeat;filter:blur(14px);transform:scale(1.25);opacity:0.82;"></div>
                        <div style="position:absolute;inset:0;background:rgba(238,245,255,0.16);"></div>
                        <img src="${safeImgPath}" style="width:100%;height:100%;border-radius:10px;object-fit:contain;object-position:center;display:block;position:relative;z-index:2;" onerror="this.src='image/profile.png'; const blur=this.parentElement.querySelector('.vendor-img-blur'); if(blur){blur.style.backgroundImage='url(image/profile.png)';}">
                    </div>
                    <div class="v-name">${escapeHtml(v.name || "-")}</div>
                    <div class="distance-badge">${escapeHtml(distanceText)}</div>
                    <button type="button" onclick="openVendorProfile(${Number(v.id)})">View Profile</button>
                </div>`;
        });
    } else {
        container.innerHTML = `<p class="empty-msg">No providers found within ${Number(rangeValue)} KM.</p>`;
    }
}

function openVendorProfile(vendorId) {
    window.location.href = `viewvendor.html?id=${Number(vendorId)}`;
}

function resolveProfilePath(path) {
    if (!path || path === "null" || String(path).trim() === "") {
        return "image/profile.png";
    }

    const cleanPath = String(path).trim();

    if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
        return cleanPath;
    }

    if (cleanPath.startsWith("image/")) {
        return cleanPath;
    }

    const fileName = getOnlyFileName(cleanPath);

    if (!fileName) {
        return "image/profile.png";
    }

    return `${baseURL}/profiles/${encodeURIComponent(fileName)}`;
}

function getOnlyFileName(path) {
    return String(path)
        .replaceAll("\\", "/")
        .split("/")
        .pop();
}

function showModal(title, message) {
    const modal = document.getElementById("statusModal");
    const titleEl = document.getElementById("modalTitle");
    const msgEl = document.getElementById("modalMessage");

    if (modal && titleEl && msgEl) {
        titleEl.innerText = title;
        msgEl.innerText = message;
        modal.classList.remove("hidden");
    }
}

function closeModal() {
    const modal = document.getElementById("statusModal");

    if (modal) {
        modal.classList.add("hidden");
    }
}

function escapeHtml(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeAttribute(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
}

function escapeJs(value) {
    return String(value || "")
        .replaceAll("\\", "\\\\")
        .replaceAll("'", "\\'")
        .replaceAll('"', '\\"')
        .replaceAll("\n", " ")
        .replaceAll("\r", " ");
}

const searchBtn = document.querySelector(".search-btn");

if (searchBtn) {
    searchBtn.addEventListener("click", function() {
        const query = document.getElementById("searchInput").value.trim();

        if (query.length > 0) {
            window.location.href = `search_results.html?q=${encodeURIComponent(query)}`;
        } else {
            showModal("Wait!", "Please enter a service or vendor name to search.");
        }
    });
}

const searchInput = document.getElementById("searchInput");

if (searchInput) {
    searchInput.addEventListener("keypress", function(e) {
        if (e.key === "Enter") {
            const btn = document.querySelector(".search-btn");

            if (btn) {
                btn.click();
            }
        }
    });
}