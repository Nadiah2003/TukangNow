const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

document.addEventListener("DOMContentLoaded", function() {
    const btnBack = document.getElementById("btnBack");

    if (btnBack) {
        btnBack.addEventListener("click", function() {
            history.back();
        });
    }

    const urlParams = new URLSearchParams(window.location.search);
    const vendorId = urlParams.get("id");

    if (!vendorId) {
        window.location.href = "homecustomer.html";
        return;
    }

    const modalBtn = document.getElementById("modalBtn");

    if (modalBtn) {
        modalBtn.addEventListener("click", closeStatusModal);
    }

    fetchVendorDetail(vendorId);
});

function fetchVendorDetail(vendorId) {
    fetch(`${baseURL}/GetVendorDetailServlet?id=${encodeURIComponent(vendorId)}`, {
        method: "GET",
        credentials: "same-origin"
    })
    .then(function(res) {
        return res.json();
    })
    .then(function(data) {
        if (data.status === "success") {
            fillVendorUI(data, vendorId);
        } else {
            showStatusModal("error", "Failed!", data.message || "Vendor not found.");
        }
    })
    .catch(function() {
        showStatusModal("error", "Failed!", "Server connection error.");
    });
}

function fillVendorUI(data, vendorId) {
    document.getElementById("vName").innerText = data.name || "N/A";
    document.getElementById("vType").innerText = data.service_name || "Category";
    document.getElementById("vSubService").innerText = data.subservice || "-";

    const price = Number(data.startprice || 0);
    document.getElementById("vPrice").innerText = `RM ${price.toFixed(2)}`;

    document.getElementById("vDate").innerText = data.avail_date || "-";
    document.getElementById("vTime").innerText = data.avail_time || "-";
    document.getElementById("vRating").innerText = `⭐ ${data.rating || "0.0"}`;

    const img = document.getElementById("vImg");

    if (img) {
        img.src = resolveProfilePath(data.img);

        img.onerror = function() {
            img.src = "image/profile.png";
        };
    }

    const bookBtn = document.getElementById("bookBtn");

    if (bookBtn) {
        bookBtn.onclick = function() {
            window.location.href = `booking.html?id=${encodeURIComponent(vendorId)}`;
        };
    }
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

    return `${baseURL}/profiles/${encodeURIComponent(fileName)}?t=${Date.now()}`;
}

function getOnlyFileName(path) {
    return String(path)
        .replaceAll("\\", "/")
        .split("/")
        .pop();
}

function showStatusModal(type, title, message) {
    const overlay = document.getElementById("statusModal");
    const iconCircle = document.getElementById("modalIconCircle");
    const icon = document.getElementById("modalIcon");
    const titleEl = document.getElementById("modalTitle");
    const msgEl = document.getElementById("modalMessage");

    if (!overlay || !iconCircle || !icon || !titleEl || !msgEl) {
        return;
    }

    titleEl.textContent = title;
    msgEl.textContent = message;

    iconCircle.classList.remove("success", "error");

    if (type === "success") {
        iconCircle.classList.add("success");
        icon.textContent = "✓";
    } else {
        iconCircle.classList.add("error");
        icon.textContent = "✕";
    }

    overlay.classList.add("is-open");
    overlay.setAttribute("aria-hidden", "false");
}

function closeStatusModal() {
    const overlay = document.getElementById("statusModal");

    if (overlay) {
        overlay.classList.remove("is-open");
        overlay.setAttribute("aria-hidden", "true");
    }
}