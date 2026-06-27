const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;
let activeEventsData = [];

document.addEventListener("DOMContentLoaded", function() {
    bindFilterControls();
    loadHomeCustomerData();
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

    fetch(`${baseURL}/api/home-cust?${params.toString()}`, {
        method: "GET",
        credentials: "same-origin"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            updateProfileUI(data);

            activeEventsData = data.events || [];
            renderEventsList(activeEventsData);

            renderCategoryList(data.electricians, 'electricianContainer', getSelectValue("electricianRange", "50"));
            renderCategoryList(data.plumbers, 'plumberContainer', getSelectValue("plumberRange", "50"));
            renderCategoryList(data.lawns, 'lawnContainer', getSelectValue("lawnRange", "50"));

            fetchNotifications();
        } else {
            showModal("Failed!", data.message);
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

function fetchNotifications() {
    fetch(`${baseURL}/api/get-notifications`, {
        method: "GET",
        credentials: "same-origin"
    })
    .then(response => response.json())
    .then(data => {
        const badge = document.getElementById("notifCountBadge");
        const container = document.getElementById("notifListContainer");

        if (data.status === 'success' && data.notifications && data.notifications.length > 0) {
            badge.innerText = data.notifications.length;
            badge.classList.remove("hidden");

            container.innerHTML = "";

            data.notifications.forEach(notif => {
                const isBalance = notif.id.startsWith("balance_");
                const cardClass = isBalance ? "notif-item-card balance-type" : "notif-item-card";

                container.innerHTML += `
                    <div class="${cardClass}">
                        <div class="notif-item-title">${escapeHtml(notif.title)}</div>
                        <div class="notif-item-msg">${escapeHtml(notif.message)}</div>
                        <div class="notif-item-date">${escapeHtml(notif.date)}</div>
                    </div>
                `;
            });
        } else {
            badge.classList.add("hidden");
            container.innerHTML = '<div class="notif-empty-state">No new notifications. Your activities are up to date!</div>';
        }
    })
    .catch(err => {
        console.error("Notification load error:", err);
    });
}

function openNotificationModal() {
    const modal = document.getElementById("notificationModal");

    if (modal) {
        modal.classList.remove("hidden");
        fetchNotifications();
    }
}

function closeNotificationModal() {
    const modal = document.getElementById("notificationModal");

    if (modal) {
        modal.classList.add("hidden");
    }
}

function updateProfileUI(data) {
    document.getElementById('customerName').innerText = data.customerName || 'Guest';

    const walletLabel = document.getElementById('walletBalanceLabel');

    if (walletLabel) {
        if (data.walletBalance !== undefined && data.walletBalance !== null) {
            walletLabel.innerText = `RM ${data.walletBalance}`;
        } else {
            walletLabel.innerText = `RM 0.00`;
        }
    }

    const rewardsDisplay = document.querySelector('.rewards-icon').previousElementSibling.querySelector('.card-value');

    if (rewardsDisplay) {
        if (data.rewardsPoints !== undefined && data.rewardsPoints !== null) {
            rewardsDisplay.innerText = `${Number(data.rewardsPoints).toLocaleString()} pts`;
        } else {
            rewardsDisplay.innerText = `0 pts`;
        }
    }

    const customerImg = document.getElementById('customerImg');

    if (customerImg) {
        customerImg.src = resolveProfilePath(data.customerImg);

        customerImg.onerror = function() {
            customerImg.src = "image/profile.png";
        };
    }
}

function renderEventsList(events) {
    const container = document.getElementById("eventsContainer");

    if (!container) return;

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
        container.innerHTML = '<p class="empty-msg" style="font-size:11px; color:#888;">No active events today.</p>';
    }
}

function openEventModal(eventId) {
    const eventObj = activeEventsData.find(e => e.id === eventId);

    if (!eventObj) return;

    const modal = document.getElementById("eventModal");
    const modalImg = document.getElementById("modalEventImg");
    const modalBadge = document.getElementById("modalEventBadge");
    const modalTitle = document.getElementById("modalEventTitle");
    const modalDuration = document.getElementById("modalEventDuration");
    const modalDesc = document.getElementById("modalEventDesc");
    const voucherSection = document.getElementById("modalVoucherSection");
    const voucherCode = document.getElementById("modalVoucherCode");
    const copyBtn = document.getElementById("copyVoucherBtn");

    if (!modal) return;

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

        if (data.status === 'success') {
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

    if (!container) return;

    container.innerHTML = '';

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
    const modal = document.getElementById('statusModal');
    const titleEl = document.getElementById('modalTitle');
    const msgEl = document.getElementById('modalMessage');

    if (modal && titleEl && msgEl) {
        titleEl.innerText = title;
        msgEl.innerText = message;
        modal.classList.remove('hidden');
    }
}

function closeModal() {
    const modal = document.getElementById('statusModal');

    if (modal) {
        modal.classList.add('hidden');
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

function escapeAttribute(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
}

const searchBtn = document.querySelector('.search-btn');

if (searchBtn) {
    searchBtn.addEventListener('click', function() {
        const query = document.getElementById('searchInput').value.trim();

        if (query.length > 0) {
            window.location.href = `search_results.html?q=${encodeURIComponent(query)}`;
        } else {
            showModal("Wait!", "Please enter a service or vendor name to search.");
        }
    });
}

const searchInput = document.getElementById('searchInput');

if (searchInput) {
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            const btn = document.querySelector('.search-btn');

            if (btn) {
                btn.click();
            }
        }
    });
}