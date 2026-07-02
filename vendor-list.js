const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;
let globalCategory = "";

document.addEventListener("DOMContentLoaded", function() {
    const grid = document.getElementById("vendorGrid");

    if (grid) {
        const rawCategory = grid.getAttribute("data-category") || "";
        globalCategory = normalizeCategory(rawCategory);
        applyFilters();
    }
});

function normalizeCategory(category) {
    const clean = String(category || "").trim().toLowerCase();

    if (clean.includes("elect")) {
        return "Electrical";
    }

    if (clean.includes("plumb")) {
        return "Plumber";
    }

    if (clean.includes("lawn")) {
        return "Lawn";
    }

    return clean.charAt(0).toUpperCase() + clean.slice(1);
}

function applyFilters() {
    const minRating = document.getElementById("filterRating").value || "0";
    const maxPrice = document.getElementById("filterPrice").value || "";
    const sortBy = document.getElementById("sortBy").value || "distance";
    const radius = document.getElementById("filterRadius").value || "50";

    const urlParams = new URLSearchParams({
        type: globalCategory,
        min_rating: minRating,
        max_price: maxPrice,
        sort_by: sortBy,
        radius: radius
    });

    fetchVendors(urlParams.toString());
}

function fetchVendors(queryString) {
    const grid = document.getElementById("vendorGrid");

    fetch(`${baseURL}/VendorListServlet?${queryString}`, {
        method: "GET",
        credentials: "same-origin"
    })
        .then(function(res) {
            if (!res.ok) {
                throw new Error("Server communication failed.");
            }

            return res.json();
        })
        .then(function(data) {
            grid.innerHTML = "";

            if (data && data.status === "session_expired") {
                window.location.href = data.redirect || "index.html";
                return;
            }

            if (data && data.status === "error") {
                grid.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-icon">🔍</div>
                        <h3>No Experts Available</h3>
                        <p>${escapeHtml(data.message || "Unable to load vendor list.")}</p>
                        <button onclick="resetFilters()" class="btn-empty">Reset Filters</button>
                    </div>
                `;
                return;
            }

            if (!Array.isArray(data) || data.length === 0) {
                grid.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-icon">🔍</div>
                        <h3>No Experts Match Filters</h3>
                        <p>Try adjusting your radius, rating, deposit range or distance preferences.</p>
                        <button onclick="resetFilters()" class="btn-empty">Reset Filters</button>
                    </div>
                `;
                return;
            }

            data.forEach(function(v) {
                const card = document.createElement("div");
                card.className = "card";
                card.onclick = function() {
                    window.location.href = `viewvendor.html?id=${encodeURIComponent(v.id)}`;
                };

                const imgSrc = getProfileImagePath(v.profile_path);
                const distance = Number(v.distance || 0);
                const distText = distance > 0 ? `${distance.toFixed(1)} km away` : "Distance unavailable";
                const depositPrice = Number(v.depositPrice || 0);
                const rating = Number(v.rating || 0);
                const reviewCount = Number(v.reviewCount || 0);

                card.innerHTML = `
                    <img src="${imgSrc}" class="profile-img" onerror="this.src='image/profile.png'">
                    <div class="card-info">
                        <div class="vendor-name">${escapeHtml(v.name || "Vendor")}</div>
                        <div class="distance-info">📍 ${escapeHtml(distText)}</div>
                        <div class="price-tag">Deposit: RM ${depositPrice.toFixed(2)}</div>
                        <div class="rating">⭐ ${rating.toFixed(1)} (${reviewCount} reviews)</div>
                    </div>
                `;

                grid.appendChild(card);
            });
        })
        .catch(function(err) {
            console.error("Fetch Error:", err);
            showModal("Failed!", "Unable to load vendor list. Please try again.", true);
        });
}

function getProfileImagePath(profilePath) {
    if (!profilePath || String(profilePath).trim() === "") {
        return "image/profile.png";
    }

    const cleanPath = String(profilePath).trim();

    if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
        return cleanPath;
    }

    if (cleanPath.startsWith("image/") || cleanPath.startsWith("profiles/")) {
        return `${baseURL}/${cleanPath}`;
    }

    return `${baseURL}/profiles/${cleanPath}`;
}

function resetFilters() {
    document.getElementById("filterRadius").value = "50";
    document.getElementById("filterRating").value = "0";
    document.getElementById("filterPrice").value = "";
    document.getElementById("sortBy").value = "distance";
    applyFilters();
}

function showModal(title, message, isError = true) {
    const modal = document.getElementById("statusModal");
    const icon = document.getElementById("modalIcon");

    document.getElementById("modalTitle").innerText = title;
    document.getElementById("modalMessage").innerText = message;

    if (isError) {
        icon.style.background = "#e53935";
        icon.innerText = "✕";
    } else {
        icon.style.background = "#4caf50";
        icon.innerText = "✓";
    }

    modal.classList.remove("hidden");
}

function closeModal() {
    document.getElementById("statusModal").classList.add("hidden");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}