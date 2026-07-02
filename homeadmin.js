const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let allVendors = [];
let allNotifications = [];

document.addEventListener("DOMContentLoaded", function() {
    const adminName = sessionStorage.getItem("userName");
    document.getElementById("adminName").innerText = adminName || "Admin";

    bindEvents();
    fetchDashboardData();
});

function bindEvents() {
    const vendorSearch = document.getElementById("vendorSearch");
    const filterSelect = document.getElementById("filterSelect");
    const modalCloseBtn = document.getElementById("modalCloseBtn");
    const notificationBtn = document.getElementById("notificationBtn");
    const closeNotificationBtn = document.getElementById("closeNotificationBtn");

    if (vendorSearch) {
        vendorSearch.addEventListener("input", function(e) {
            const term = e.target.value.toLowerCase();

            const filtered = allVendors.filter(v =>
                (v.name && v.name.toLowerCase().includes(term)) ||
                (v.phone && v.phone.includes(term)) ||
                (v.status && v.status.toLowerCase().includes(term))
            );

            renderVendorTable(filtered);
        });
    }

    if (filterSelect) {
        filterSelect.addEventListener("change", sortVendors);
    }

    if (modalCloseBtn) {
        modalCloseBtn.addEventListener("click", closeModal);
    }

    if (notificationBtn) {
        notificationBtn.addEventListener("click", function() {
            openNotificationModal();
        });
    }

    if (closeNotificationBtn) {
        closeNotificationBtn.addEventListener("click", function() {
            closeNotificationModal();
        });
    }
}

function showModal(title, message) {
    document.getElementById("modalTitle").innerText = title;
    document.getElementById("modalMessage").innerText = message;
    document.getElementById("customModal").style.display = "flex";
}

function closeModal() {
    document.getElementById("customModal").style.display = "none";
}

function openNotificationModal() {
    document.getElementById("notificationModal").style.display = "flex";
    renderNotifications(allNotifications);
}

function closeNotificationModal() {
    document.getElementById("notificationModal").style.display = "none";
}

function fetchDashboardData() {
    fetch(`${baseURL}/HomeAdminServlet?t=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(res => {
        if (res.status === 401) {
            showModal("Session Debug", "HomeAdminServlet returned 401 Unauthorized. Session/cookie is missing, but redirect is disabled for debugging.");
            return null;
        }

        if (!res.ok) {
            throw new Error("Server returned error " + res.status);
        }

        return res.json();
    })
    .then(data => {
        if (!data) {
            return;
        }

        if (data.status === "session_expired") {
            showModal("Session Debug", data.message || "Session expired. Redirect is disabled for debugging.");
            return;
        }

        if (data.error || data.status === "error") {
            showModal("Failed!", data.error || data.message || "Unable to load dashboard.");
            return;
        }

        document.getElementById("newAppCount").innerText = data.newApps || 0;
        document.getElementById("activeVendorCount").innerText = data.activeVendors || 0;
        document.getElementById("newReportCount").innerText = data.newReports || 0;

        const adminProfileImg = document.getElementById("adminProfileImg");
        adminProfileImg.src = resolveProfilePath(data.adminProfile);
        adminProfileImg.onerror = function() {
            this.src = "image/profile.png";
        };

        allNotifications = data.notifications || [];
        updateNotificationBadge(allNotifications);
        renderNotifications(allNotifications);

        allVendors = data.vendors || [];
        renderVendorTable(allVendors);
    })
    .catch(err => {
        showModal("Failed!", err.message || "Server returned error. Please check database connection.");
    });
}

function updateNotificationBadge(notifications) {
    const badge = document.getElementById("notificationBadge");

    if (!badge) {
        return;
    }

    const total = (notifications || []).reduce(function(sum, item) {
        return sum + Number(item.count || 0);
    }, 0);

    if (total > 0) {
        badge.innerText = total > 99 ? "99+" : total;
        badge.style.display = "flex";
    } else {
        badge.style.display = "none";
    }
}

function renderNotifications(notifications) {
    const list = document.getElementById("notificationList");

    if (!list) {
        return;
    }

    if (!notifications || notifications.length === 0) {
        list.innerHTML = `<div class="notification-empty">No new notification.</div>`;
        return;
    }

    const visibleNotifications = notifications.filter(item => Number(item.count || 0) > 0);

    if (visibleNotifications.length === 0) {
        list.innerHTML = `<div class="notification-empty">No new notification.</div>`;
        return;
    }

    list.innerHTML = visibleNotifications.map(item => {
        return `
            <div class="notification-item" onclick="goNotificationLink('${escapeJs(item.link || "")}')">
                <div class="notification-item-icon">${escapeHtml(item.icon || "🔔")}</div>
                <div class="notification-item-content">
                    <p class="notification-item-title">${escapeHtml(item.title || "Notification")}</p>
                    <p class="notification-item-message">${escapeHtml(item.message || "-")}</p>
                </div>
                <span class="notification-count">${Number(item.count || 0)}</span>
            </div>
        `;
    }).join("");
}

function goNotificationLink(link) {
    const cleanLink = String(link || "").trim();

    if (cleanLink === "") {
        return;
    }

    window.location.href = cleanLink;
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

    return `${baseURL}/HomeAdminServlet?action=profileImage&file=${encodeURIComponent(fileName)}&t=${Date.now()}`;
}

function resolveDocPath(fileName) {
    if (!fileName || String(fileName).trim() === "") {
        return "#";
    }

    const cleanName = String(fileName).trim();

    if (cleanName.startsWith("http://") || cleanName.startsWith("https://")) {
        return cleanName;
    }

    const onlyName = getOnlyFileName(cleanName);

    if (!onlyName) {
        return "#";
    }

    return `${baseURL}/HomeAdminServlet?action=licenseFile&file=${encodeURIComponent(onlyName)}&t=${Date.now()}`;
}

function getOnlyFileName(path) {
    return String(path)
        .replaceAll("\\", "/")
        .split("/")
        .pop();
}

function renderVendorTable(vendors) {
    const tbody = document.getElementById("vendorTableBody");

    if (!vendors || vendors.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 20px;">No vendors found.</td></tr>`;
        return;
    }

    tbody.innerHTML = vendors.map(v => {
        const fileNames = v.docUrl ? String(v.docUrl).split(",") : [];

        const fileLinksHTML = fileNames.map(fileName => {
            const trimmedName = fileName.trim();

            if (!trimmedName) {
                return "";
            }

            const onlyName = getOnlyFileName(trimmedName);
            const parts = onlyName.split("_");
            const displayName = parts.length > 2 ? parts.slice(2).join("_") : onlyName;
            const docHref = resolveDocPath(trimmedName);

            return `
                <div style="margin-bottom: 4px;">
                    <a href="${escapeHtml(docHref)}" target="_blank" 
                       style="color:#1848a0; font-weight:600; text-decoration:none; font-size:0.7rem; display:flex; align-items:center; gap:4px;">
                        📄 ${escapeHtml(displayName)}
                    </a>
                </div>`;
        }).join("");

        const profileImgSrc = resolveProfilePath(v.profilePath);
        const statusText = v.status ? v.status : "inactive";
        const statusClass = statusText.toLowerCase() === "active" ? "active-pill" : "expired-pill";
        const ratingValue = Number(v.rating || 0);
        const workDone = Number(v.workDone || 0);

        return `
            <tr>
                <td>
                    <div style="display:flex; align-items:center; gap:10px;">
                        <img src="${escapeHtml(profileImgSrc)}"
                             style="width:38px; height:38px; border-radius:50%; object-fit:cover; border:1px solid #eee;"
                             onerror="this.src='image/profile.png'">
                        <div>
                            <strong style="font-size:0.85rem;">${escapeHtml(v.name || "No Name")}</strong><br>
                            <span style="font-size:0.75rem; color:#666;">${escapeHtml(v.phone || "No Phone")}</span>
                        </div>
                    </div>
                </td>
                <td>${fileLinksHTML || '<span style="color:#999; font-size:0.75rem;">No License</span>'}</td>
                <td>
                    <span style="color:#f39c12; font-weight:bold; font-size:0.8rem;">★ ${ratingValue.toFixed(1)}</span><br>
                    <span style="font-size:0.7rem; color:#888;">${workDone} jobs</span>
                </td>
                <td>
                    <span class="status-pill ${statusClass}">
                        ${escapeHtml(statusText.toUpperCase())}
                    </span>
                </td>
                <td>
                    <button onclick="viewVendorDetails('${escapeJs(v.phone || "")}')" class="view-btn">
                        VIEW
                    </button>
                </td>
            </tr>`;
    }).join("");
}

function sortVendors() {
    const sortType = document.getElementById("filterSelect").value;
    let sorted = [...allVendors];

    if (sortType === "az") {
        sorted.sort((a, b) => (a.name || "").localeCompare(b.name || ""));
    } else if (sortType === "za") {
        sorted.sort((a, b) => (b.name || "").localeCompare(a.name || ""));
    } else if (sortType === "rating") {
        sorted.sort((a, b) => Number(b.rating || 0) - Number(a.rating || 0));
    } else if (sortType === "jobs") {
        sorted.sort((a, b) => Number(b.workDone || 0) - Number(a.workDone || 0));
    } else if (sortType === "newest") {
        sorted.reverse();
    }

    renderVendorTable(sorted);
}

function viewVendorDetails(phone) {
    if (!phone || phone === "No Phone") {
        showModal("Failed!", "Phone number not found for this vendor.");
        return;
    }

    sessionStorage.setItem("viewVendorPhone", phone);
    showModal("Vendor Selected", "Vendor information has been saved. You can continue with your next action.");
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