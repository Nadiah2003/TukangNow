const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let map;
let vendorMarker;
let customerMarker;
let routeLine;
let mapInitialized = false;
let userInteracted = false;
let latestData = null;
let selectedRating = 0;
let materialIndex = 0;
let receiptGroupIndex = 0;
let locationUpdateTimer = null;
let trackingRefreshTimer = null;
let lastLocationSendTime = 0;
let isFetchingTrackingStatus = false;

let activeRoadRoutePoints = [];
let routeFetchInProgress = false;
let routeRequestSerial = 0;
let lastRoadRouteVendorLoc = null;
let lastRoadRouteCustomerLoc = null;
let lastAutoFitAt = 0;
let followVendorMode = false;
let followButtonElement = null;
let systemMapMove = false;

const MALAYSIA_CENTER = [3.1390, 101.6869];
const MALAYSIA_BOUNDS = L.latLngBounds([0.5, 99.0], [7.8, 119.5]);
const ROUTE_REFETCH_DISTANCE_KM = 0.08;
const CUSTOMER_ROUTE_CHANGE_KM = 0.02;
const AUTO_FIT_INTERVAL_MS = 12000;
const MARKER_ANIMATION_MS = 900;
const FOLLOW_VENDOR_ZOOM = 17;

document.addEventListener("DOMContentLoaded", function() {
    const btnBack = document.getElementById("btnBack");
    const alertCloseBtn = document.getElementById("alertCloseBtn");

    if (btnBack) {
        btnBack.addEventListener("click", function() {
            goBackByRole();
        });
    }

    if (alertCloseBtn) {
        alertCloseBtn.addEventListener("click", function() {
            document.getElementById("customAlertModal").style.display = "none";
        });
    }

    window.addEventListener("beforeunload", function() {
        stopTrackingRefresh();
        stopVendorLocationUpdater();
    });

    document.addEventListener("visibilitychange", function() {
        if (document.hidden) {
            stopTrackingRefresh();
            stopVendorLocationUpdater();
            return;
        }

        fetchTrackingStatus(true);
    });

    ensureTrackingViewInUrl();
    fetchTrackingStatus(true);
});

function getBookingId() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get("id");
}

function ensureTrackingViewInUrl() {
    const view = getRequestedView();
    setTrackingViewInUrl(view);
}

function getRequestedView() {
    const urlParams = new URLSearchParams(window.location.search);
    const urlView = String(urlParams.get("view") || "").trim().toLowerCase();

    if (urlView === "vendor" || urlView === "customer") {
        sessionStorage.setItem("tukangnow_tracking_view", urlView);
        return urlView;
    }

    const referrer = String(document.referrer || "").toLowerCase();

    if (referrer.includes("job.html") || referrer.includes("homevendor.html") || referrer.includes("walletvendor.html") || referrer.includes("profilevendor.html")) {
        sessionStorage.setItem("tukangnow_tracking_view", "vendor");
        return "vendor";
    }

    if (referrer.includes("order.html") || referrer.includes("myorder.html") || referrer.includes("homecustomer.html") || referrer.includes("wallet.html") || referrer.includes("profilecustomer.html")) {
        sessionStorage.setItem("tukangnow_tracking_view", "customer");
        return "customer";
    }

    const savedView = String(sessionStorage.getItem("tukangnow_tracking_view") || "").trim().toLowerCase();

    if (savedView === "customer") {
        return "customer";
    }

    sessionStorage.setItem("tukangnow_tracking_view", "vendor");
    return "vendor";
}

function setTrackingViewInUrl(view) {
    const cleanView = String(view || "").trim().toLowerCase();

    if (cleanView !== "vendor" && cleanView !== "customer") {
        return;
    }

    const url = new URL(window.location.href);
    const currentView = String(url.searchParams.get("view") || "").trim().toLowerCase();

    if (currentView === cleanView) {
        return;
    }

    url.searchParams.set("view", cleanView);
    window.history.replaceState({}, "", url.toString());
}

function goBackByRole() {
    const role = latestData && latestData.viewer_role ? latestData.viewer_role : getRequestedView();
    const trackStatus = latestData ? normalizeTrackingStatus(latestData.current_status || latestData.tracking_status) : "";
    const hasRatedValue = latestData ? latestData.has_rated : false;

    const hasRated = trackStatus === "Rated"
        || hasRatedValue === true
        || hasRatedValue === 1
        || String(hasRatedValue).trim().toLowerCase() === "true";

    if (role === "customer" && trackStatus === "Completed" && !hasRated) {
        showCustomAlert(
            "Rating Required",
            "Please rate and comment for the vendor before going back.",
            "error"
        );
        return;
    }

    if (role === "vendor") {
        window.location.href = "job.html";
        return;
    }

    window.location.href = "myorder.html";
}

function isTypingInsideActionArea() {
    const active = document.activeElement;
    const area = document.getElementById("actionArea");

    if (!active || !area) {
        return false;
    }

    return area.contains(active) && ["INPUT", "TEXTAREA", "SELECT"].includes(active.tagName);
}

function hasEditingFormOnScreen() {
    const area = document.getElementById("actionArea");

    if (!area) {
        return false;
    }

    const hasFile = area.querySelector("input[type='file']");
    const hasText = area.querySelector("input[type='text'], input[type='number'], textarea, select");

    return !!hasFile || !!hasText;
}

function showCustomAlert(title, message, type = "success", callback = null) {
    const modal = document.getElementById("customAlertModal");
    const iconContainer = document.getElementById("alertIconContainer");
    const titleEl = document.getElementById("alertTitle");
    const msgEl = document.getElementById("alertMessage");
    const closeBtn = document.getElementById("alertCloseBtn");

    titleEl.innerText = title;
    msgEl.innerText = message;

    iconContainer.className = "alert-icon-circle " + type;

    if (type === "success") {
        iconContainer.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
        `;
    } else {
        iconContainer.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
        `;
    }

    modal.style.display = "flex";

    closeBtn.onclick = function() {
        modal.style.display = "none";

        if (callback && typeof callback === "function") {
            callback();
        }
    };
}

async function fetchTrackingStatus(forceRefresh = false) {
    const bookingId = getBookingId();

    if (!bookingId) {
        stopTrackingRefresh();
        stopVendorLocationUpdater();
        showCustomAlert("Failed!", "Booking ID is required.", "error", function() {
            window.location.href = "myorder.html";
        });
        return;
    }

    if (isFetchingTrackingStatus) {
        return;
    }

    if (!forceRefresh && isTypingInsideActionArea()) {
        return;
    }

    if (!forceRefresh && latestData) {
        const currentStatus = normalizeTrackingStatus(latestData.current_status || latestData.tracking_status);

        if (currentStatus !== "On The Way" && hasEditingFormOnScreen()) {
            return;
        }
    }

    isFetchingTrackingStatus = true;

    try {
        const view = getRequestedView();

        const res = await fetch(`${baseURL}/TrackingVendorServlet?id=${encodeURIComponent(bookingId)}&view=${encodeURIComponent(view)}`, {
            method: "GET",
            credentials: "same-origin"
        });

        const data = await res.json();

        if (data.status === "session_expired") {
            stopTrackingRefresh();
            stopVendorLocationUpdater();
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data.status !== "success") {
            stopTrackingRefresh();
            stopVendorLocationUpdater();
            showCustomAlert("Failed!", data.message || "Unable to load tracking details.", "error");
            return;
        }

        latestData = data;

        if (data.viewer_role === "vendor" || data.viewer_role === "customer") {
            sessionStorage.setItem("tukangnow_tracking_view", data.viewer_role);
            setTrackingViewInUrl(data.viewer_role);
        }

        renderTrackingData(data, bookingId, forceRefresh);

    } catch (err) {
        console.error("Tracking fetch error:", err);
    } finally {
        isFetchingTrackingStatus = false;
    }
}

function startTrackingRefresh() {
    if (trackingRefreshTimer) {
        return;
    }

    trackingRefreshTimer = setInterval(function() {
        if (document.hidden) {
            return;
        }

        if (isTypingInsideActionArea()) {
            return;
        }

        fetchTrackingStatus(false);
    }, 30000);
}

function stopTrackingRefresh() {
    if (trackingRefreshTimer) {
        clearInterval(trackingRefreshTimer);
        trackingRefreshTimer = null;
    }
}

function renderTrackingData(data, bookingId, forceRefresh = false) {
    renderBottomNav(data.viewer_role);
    renderProfileSection(data);
    renderBookingInfo(data);

    const trackStatus = normalizeTrackingStatus(data.current_status || data.tracking_status);
    const mapSection = document.getElementById("map");
    const showMap = trackStatus === "On The Way";

    if (showMap) {
        startTrackingRefresh();
    } else {
        stopTrackingRefresh();
    }

    if (data.viewer_role === "vendor" && trackStatus === "On The Way") {
        startVendorLocationUpdater(bookingId);
    } else {
        stopVendorLocationUpdater();
    }

    if (showMap) {
        document.body.classList.add("map-open");
        mapSection.style.display = "block";
        renderMap(data);
    } else {
        document.body.classList.remove("map-open");
        mapSection.style.display = "none";
        followVendorMode = false;
        updateFollowButtonState();
    }

    renderTimeline(trackStatus);
    renderStatusText(data, trackStatus);
    renderActions(data, bookingId, trackStatus, forceRefresh);
}

function startVendorLocationUpdater(bookingId) {
    if (locationUpdateTimer) {
        return;
    }

    sendVendorLocation(bookingId);

    locationUpdateTimer = setInterval(function() {
        if (document.hidden) {
            return;
        }

        if (latestData) {
            const trackStatus = normalizeTrackingStatus(latestData.current_status || latestData.tracking_status);

            if (trackStatus !== "On The Way") {
                stopVendorLocationUpdater();
                return;
            }
        }

        sendVendorLocation(bookingId);
    }, 30000);
}

function stopVendorLocationUpdater() {
    if (locationUpdateTimer) {
        clearInterval(locationUpdateTimer);
        locationUpdateTimer = null;
    }
}

function sendVendorLocation(bookingId) {
    if (!navigator.geolocation) {
        return;
    }

    if (latestData) {
        const trackStatus = normalizeTrackingStatus(latestData.current_status || latestData.tracking_status);

        if (latestData.viewer_role !== "vendor" || trackStatus !== "On The Way") {
            stopVendorLocationUpdater();
            return;
        }
    }

    const now = Date.now();

    if (now - lastLocationSendTime < 25000) {
        return;
    }

    navigator.geolocation.getCurrentPosition(
        function(position) {
            const lat = Number(position.coords.latitude || 0);
            const lng = Number(position.coords.longitude || 0);

            if (!isValidMalaysiaCoordinate(lat, lng)) {
                return;
            }

            lastLocationSendTime = Date.now();

            const formData = new URLSearchParams();
            formData.append("action", "update_location");
            formData.append("booking_id", bookingId);
            formData.append("latitude", lat.toString());
            formData.append("longitude", lng.toString());

            fetch(`${baseURL}/TrackingVendorServlet`, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData.toString()
            }).catch(function() {});
        },
        function() {},
        {
            enableHighAccuracy: true,
            timeout: 15000,
            maximumAge: 20000
        }
    );
}

function renderBottomNav(role) {
    const nav = document.getElementById("bottomNav");

    if (!nav) {
        return;
    }

    if (role === "vendor") {
        nav.innerHTML = `
            <a href="homevendor.html" class="nav-item">
                <svg class="nav-svg" viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"></path></svg>
                <span>Home</span>
            </a>
            <a href="job.html" class="nav-item active">
                <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path></svg>
                <span>My Job</span>
            </a>
            <a href="walletvendor.html" class="nav-item">
                <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 12V8H6a2 2 0 0 1-2-2c0-1.1.9-2 2-2h12v4"></path><path d="M4 6v12c0 1.1.9 2 2 2h14v-4"></path><path d="M18 12a2 2 0 0 0-2 2c0 1.1.9 2 2 2h4v-4h-4z"></path></svg>
                <span>Wallet</span>
            </a>
            <a href="profilevendor.html" class="nav-item">
                <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                <span>Profile</span>
            </a>
        `;
        return;
    }

    nav.innerHTML = `
        <a href="homecustomer.html" class="nav-item">
            <svg class="nav-svg" viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"></path></svg>
            <span>Home</span>
        </a>
        <a href="myorder.html" class="nav-item active">
            <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path><path d="M3 6h18"></path><path d="M16 10a4 4 0 0 1-8 0"></path></svg>
            <span>My Order</span>
        </a>
        <a href="wallet.html" class="nav-item">
            <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 12V8H6a2 2 0 0 1-2-2c0-1.1.9-2 2-2h12v4"></path><path d="M4 6v12c0 1.1.9 2 2 2h14v-4"></path><path d="M18 12a2 2 0 0 0-2 2c0 1.1.9 2 2 2h4v-4h-4z"></path></svg>
            <span>Wallet</span>
        </a>
        <a href="profilecustomer.html" class="nav-item">
            <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            <span>Profile</span>
        </a>
    `;
}

function renderProfileSection(data) {
    const img = document.getElementById("profileImg");
    const name = document.getElementById("profileName");
    const subInfo = document.getElementById("profileSubInfo");
    const phoneText = document.getElementById("profilePhoneText");
    const callBtn = document.getElementById("callBtn");
    const trackStatus = normalizeTrackingStatus(data.current_status || data.tracking_status);
    const canCall = trackStatus !== "Accepted" && trackStatus !== "Rated";

    if (data.viewer_role === "vendor") {
        img.src = resolveProfilePath(data.customer_profile_path);
        name.innerHTML = `${escapeHtml(data.customer_name || "Customer")}`;
        subInfo.innerText = data.booking_service || "-";
        phoneText.innerText = data.customer_phone || "-";
        callBtn.href = `tel:${data.customer_phone || ""}`;
    } else {
        img.src = resolveProfilePath(data.profile_path);
        name.innerHTML = `${escapeHtml(data.vendor_name || "Vendor")} <span class="rating" id="ratingBadge">${escapeHtml(data.vendor_rating || "0.0")} ⭐</span>`;
        subInfo.innerText = `${data.vehicle_model || "-"} (${data.plate_number || "-"})`;
        phoneText.innerText = data.vendor_phone || "-";
        callBtn.href = `tel:${data.vendor_phone || ""}`;
    }

    img.onerror = function() {
        this.src = "image/profile.png";
    };

    if (canCall) {
        callBtn.classList.add("is-visible");
    } else {
        callBtn.classList.remove("is-visible");
    }
}

function renderBookingInfo(data) {
    const box = document.getElementById("bookingInfoBox");

    box.innerHTML = `
        <p><strong>Booking ID:</strong> #${escapeHtml(data.booking_id || "-")}</p>
        <p><strong>Service:</strong> ${escapeHtml(data.booking_service || "-")}</p>
        <p><strong>Subservice:</strong> ${escapeHtml(data.subservicebooked || "-")}</p>
        <p><strong>Problem:</strong> ${escapeHtml(data.problem || "-")}</p>
        <p><strong>Booking Date:</strong> ${escapeHtml(data.bookingdate || "-")}</p>
        <p><strong>Deposit Paid:</strong> RM ${formatMoney(data.deposit || 0)}</p>
        <p><strong>Status:</strong> ${escapeHtml(data.current_status || "-")}</p>
    `;
}

function renderStatusText(data, trackStatus) {
    const etaText = document.getElementById("etaText");
    const durDistText = document.getElementById("durationDistanceText");
    const statusText = document.getElementById("statusText");

    if (trackStatus === "Accepted") {
        etaText.innerText = "Booking Accepted";

        if (data.viewer_role === "customer") {
            if (data.is_emergency || data.booking_date_reached) {
                durDistText.innerText = "Your vendor has accepted the booking.";
                statusText.innerText = "Please wait for the vendor to start the journey to your location.";
            } else {
                durDistText.innerText = "Your booking has been accepted.";
                statusText.innerText = "Please wait until your booking date arrives before tracking begins.";
            }
        } else {
            durDistText.innerText = "Customer is waiting for your movement update.";
            statusText.innerText = "Enter your vehicle details, then press On The Way when you are ready to go.";
        }

        statusText.style.color = "#1848a0";
        return;
    }

    if (trackStatus === "On The Way") {
        etaText.innerText = `Estimated arrival by ${data.eta || "--:--"}`;
        durDistText.innerText = `${data.duration_mins || "0"} mins away (${data.distance_km || "0.0"} km remaining)`;
        statusText.innerText = data.viewer_role === "vendor" ? "Follow the road route to the customer location." : "Vendor is on the way to your location.";
        statusText.style.color = "#4caf50";
        return;
    }

    if (trackStatus === "Arrived") {
        etaText.innerText = "Vendor Has Arrived";
        durDistText.innerText = "The vendor has reached the customer location.";
        statusText.innerText = data.viewer_role === "vendor" ? "Upload arrival proof before starting the work." : "The vendor has arrived and will start the inspection soon.";
        statusText.style.color = "#2196f3";
        return;
    }

    if (trackStatus === "Started") {
        etaText.innerText = "Job In Progress";
        durDistText.innerText = "The service work has started.";
        statusText.innerText = data.viewer_role === "vendor" ? "Update material or receipt details before completing the service." : "The vendor is currently working on your service.";
        statusText.style.color = "#ff9800";
        return;
    }

    if (trackStatus === "Second Payment") {
        etaText.innerText = "Balance Payment Required";
        durDistText.innerText = data.viewer_role === "customer" ? "Please pay the remaining balance to complete this booking." : "Waiting for the customer to complete the balance payment.";
        statusText.innerText = "Material and receipt details have been submitted successfully.";
        statusText.style.color = "#6d28d9";
        return;
    }

    if (trackStatus === "Completed") {
        etaText.innerText = "Job Completed";
        durDistText.innerText = data.viewer_role === "customer" ? "Please rate and comment before leaving this page." : "Waiting for customer rating.";
        statusText.innerText = "The booking has been completed.";
        statusText.style.color = "#4caf50";
        return;
    }

    if (trackStatus === "Rated") {
        etaText.innerText = "Booking Rated";
        durDistText.innerText = "Thank you. This booking has been rated.";
        statusText.innerText = "Rating and comment have been submitted.";
        statusText.style.color = "#4caf50";
        return;
    }

    etaText.innerText = "Tracking";
    durDistText.innerText = "-";
    statusText.innerText = trackStatus;
}

function renderTimeline(trackStatus) {
    const pLine = document.getElementById("progressLine");
    const pin1 = document.getElementById("pin1");
    const pin2 = document.getElementById("pin2");
    const pin3 = document.getElementById("pin3");
    const pin4 = document.getElementById("pin4");

    pin1.className = "step-pin";
    pin2.className = "step-pin";
    pin3.className = "step-pin";
    pin4.className = "step-pin";

    pLine.style.maxWidth = "75%";

    if (trackStatus === "Accepted") {
        pLine.style.width = "0%";
        pin1.classList.add("active");
        return;
    }

    if (trackStatus === "On The Way") {
        pLine.style.width = "12.5%";
        pin1.classList.add("active");
        return;
    }

    if (trackStatus === "Arrived") {
        pLine.style.width = "37.5%";
        pin1.classList.add("completed");
        pin2.classList.add("active");
        return;
    }

    if (trackStatus === "Started") {
        pLine.style.width = "62.5%";
        pin1.classList.add("completed");
        pin2.classList.add("completed");
        pin3.classList.add("active");
        return;
    }

    if (trackStatus === "Second Payment") {
        pLine.style.width = "75%";
        pin1.classList.add("completed");
        pin2.classList.add("completed");
        pin3.classList.add("completed");
        pin4.classList.add("active");
        return;
    }

    if (trackStatus === "Completed" || trackStatus === "Rated") {
        pLine.style.width = "75%";
        pin1.classList.add("completed");
        pin2.classList.add("completed");
        pin3.classList.add("completed");
        pin4.classList.add("completed");
        return;
    }

    pLine.style.width = "0%";
    pin1.classList.add("active");
}

function renderActions(data, bookingId, trackStatus, forceRefresh = false) {
    const area = document.getElementById("actionArea");

    if (!forceRefresh && isTypingInsideActionArea() && trackStatus !== "On The Way") {
        return;
    }

    if (data.viewer_role === "vendor") {
        renderVendorActions(area, data, bookingId, trackStatus);
        return;
    }

    renderCustomerActions(area, data, bookingId, trackStatus);
}

function renderCustomerActions(area, data, bookingId, trackStatus) {
    if (trackStatus === "Accepted") {
        area.innerHTML = `
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        return;
    }

    if (trackStatus === "On The Way" || trackStatus === "Arrived" || trackStatus === "Started") {
        area.innerHTML = `
            <button class="main-btn" type="button" onclick="goChat(${Number(bookingId)})">Open Chat Room</button>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        return;
    }

    if (trackStatus === "Second Payment") {
        area.innerHTML = `
            <button class="main-btn" type="button" onclick="goPayBalance(${Number(bookingId)})">Pay Balance</button>
            <button class="main-btn" type="button" onclick="goChat(${Number(bookingId)})">Open Chat Room</button>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        return;
    }

    if (trackStatus === "Completed") {
        area.innerHTML = `
            <div class="form-card">
                <h3>Rate This Service</h3>
                <p>Please rate and comment before leaving this page.</p>
                <div class="rating-options">
                    <button type="button" class="inactive-star" onclick="selectRating(1)" aria-label="1 star">★</button>
                    <button type="button" class="inactive-star" onclick="selectRating(2)" aria-label="2 stars">★</button>
                    <button type="button" class="inactive-star" onclick="selectRating(3)" aria-label="3 stars">★</button>
                    <button type="button" class="inactive-star" onclick="selectRating(4)" aria-label="4 stars">★</button>
                    <button type="button" class="inactive-star" onclick="selectRating(5)" aria-label="5 stars">★</button>
                </div>
                <div class="input-group">
                    <label>Comment</label>
                    <textarea id="ratingComment" class="textarea-field" placeholder="Write your comment here..."></textarea>
                </div>
                <button class="main-btn" type="button" onclick="submitRating(${Number(bookingId)})">Submit Rating</button>
            </div>
            <button class="main-btn" type="button" onclick="goChat(${Number(bookingId)})">Open Chat Room</button>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        selectedRating = 0;
        return;
    }

    if (trackStatus === "Rated") {
        area.innerHTML = `
            <div class="form-card">
                <h3>Your Rating</h3>
                <p><strong>Rating:</strong> ${renderStarText(data.rating_val || 0)}</p>
                <p><strong>Comment:</strong> ${escapeHtml(data.rating_comment || "-")}</p>
            </div>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        return;
    }

    area.innerHTML = `
        <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
    `;
}

function renderVendorActions(area, data, bookingId, trackStatus) {
    if (trackStatus === "Accepted") {
        area.innerHTML = `
            <div class="form-card">
                <h3>Vehicle Details</h3>
                <p>Enter your vehicle type and plate number before updating the job to On The Way.</p>
                <div class="input-group">
                    <label>Vehicle Type</label>
                    <p>Motorcycle / Car / Van</p>
                    <input type="text" id="vehicleModelInput" class="input-field" placeholder="Example: Toyota Hiace Van (White)" value="${escapeAttr(data.vehicle_model || "")}" oninput="validateVehicleForm()">
                </div>
                <div class="input-group">
                    <label>Plate Number</label>
                    <input type="text" id="plateNumberInput" class="input-field" placeholder="Example: ABC1234" value="${escapeAttr(data.plate_number || "")}" oninput="validateVehicleForm()">
                </div>
                <button class="main-btn" id="btnOnTheWay" type="button" onclick="updateOnTheWay(${Number(bookingId)})" disabled>On The Way</button>
            </div>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        validateVehicleForm();
        return;
    }

    if (trackStatus === "On The Way") {
        area.innerHTML = `
            <button class="main-btn" type="button" onclick="updateArrived(${Number(bookingId)})">Arrived</button>
            <button class="main-btn" type="button" onclick="goChat(${Number(bookingId)})">Open Chat Room</button>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        return;
    }

    if (trackStatus === "Arrived") {
        area.innerHTML = `
            <div class="form-card">
                <h3>Arrival Proof</h3>
                <p>Take a photo as proof that you have arrived at the customer location.</p>
                <p><strong>Reminder:</strong> Keep every purchase receipt or take a clear photo of any item used with the price visible. Admin will check it before approving claims.</p>
                <div class="input-group">
                    <label>Arrival Evidence Photo</label>
                    <input type="file" id="arrivalEvidenceInput" class="file-input" accept="image/*" onchange="validateArrivalEvidence()">
                </div>
                <button class="main-btn" id="btnStarted" type="button" onclick="startWork(${Number(bookingId)})" disabled>Started</button>
            </div>
            <button class="main-btn" type="button" onclick="goChat(${Number(bookingId)})">Open Chat Room</button>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        return;
    }

    if (trackStatus === "Started") {
        materialIndex = 0;
        receiptGroupIndex = 0;

        area.innerHTML = `
            <div class="form-card">
                <h3>Material / Receipt Update</h3>
                <p>Add your work labour charge. For materials, upload one receipt per shop, then add all items under that receipt. If you bought from another shop, add a new receipt/shop group.</p>

                <div class="input-group">
                    <label>Work Labour Charge (RM)</label>
                    <input type="number" id="laborChargeInput" class="input-field" min="0" step="0.01" placeholder="Example: 80.00">
                </div>

                <div id="receiptGroupList"></div>

                <button class="secondary-btn" type="button" onclick="addReceiptGroup()">Add Receipt / Shop</button>

                <div class="input-group" style="margin-top:12px;">
                    <label>Completed Work Photo</label>
                    <input type="file" id="completionEvidenceInput" class="file-input" accept="image/*">
                </div>

                <button class="main-btn" type="button" onclick="completeWork(${Number(bookingId)})">Completed</button>
            </div>
            <button class="main-btn" type="button" onclick="goChat(${Number(bookingId)})">Open Chat Room</button>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;

        addReceiptGroup();
        return;
    }

    if (trackStatus === "Second Payment") {
        area.innerHTML = `
            <div class="form-card">
                <h3>Waiting For Balance Payment</h3>
                <p>Material, receipt, labour charge, and completed work proof have been submitted successfully.</p>
                <p>Please wait for the customer to pay the remaining balance.</p>
                <p><strong>Balance:</strong> RM ${formatMoney(data.totalbalance || 0)}</p>
            </div>
            <button class="main-btn" type="button" onclick="goChat(${Number(bookingId)})">Open Chat Room</button>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        return;
    }

    if (trackStatus === "Completed") {
        area.innerHTML = `
            <div class="form-card">
                <h3>Completed</h3>
                <p>This booking has been completed. Waiting for customer rating and comment.</p>
            </div>
            <button class="main-btn" type="button" onclick="goChat(${Number(bookingId)})">Open Chat Room</button>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        return;
    }

    if (trackStatus === "Rated") {
        area.innerHTML = `
            <div class="form-card">
                <h3>Customer Rating</h3>
                <p><strong>Rating:</strong> ${renderStarText(data.rating_val || 0)}</p>
                <p><strong>Comment:</strong> ${escapeHtml(data.rating_comment || "-")}</p>
            </div>
            <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
        `;
        return;
    }

    area.innerHTML = `
        <button class="secondary-btn report-btn" type="button" onclick="goReport(${Number(bookingId)})">Report This Booking</button>
    `;
}

function validateVehicleForm() {
    const vehicle = document.getElementById("vehicleModelInput");
    const plate = document.getElementById("plateNumberInput");
    const btn = document.getElementById("btnOnTheWay");

    if (!vehicle || !plate || !btn) {
        return;
    }

    btn.disabled = vehicle.value.trim() === "" || plate.value.trim() === "";
}

function validateArrivalEvidence() {
    const input = document.getElementById("arrivalEvidenceInput");
    const btn = document.getElementById("btnStarted");

    if (!input || !btn) {
        return;
    }

    btn.disabled = !input.files || input.files.length === 0;
}

async function updateOnTheWay(bookingId) {
    const vehicle = document.getElementById("vehicleModelInput").value.trim();
    const plate = document.getElementById("plateNumberInput").value.trim();

    if (!vehicle || !plate) {
        showCustomAlert("Failed!", "Please enter vehicle type and plate number first.", "error");
        return;
    }

    const formData = new URLSearchParams();
    formData.append("action", "on_the_way");
    formData.append("booking_id", bookingId);
    formData.append("vehicle_model", vehicle);
    formData.append("plate_number", plate);

    await postAction(formData, false);
}

async function updateArrived(bookingId) {
    const formData = new URLSearchParams();
    formData.append("action", "arrived");
    formData.append("booking_id", bookingId);

    await postAction(formData, false);
}

async function startWork(bookingId) {
    const input = document.getElementById("arrivalEvidenceInput");

    if (!input || !input.files || input.files.length === 0) {
        showCustomAlert("Failed!", "Please attach arrival proof photo first.", "error");
        return;
    }

    const formData = new FormData();
    formData.append("action", "started");
    formData.append("booking_id", bookingId);
    formData.append("arrival_evidence", input.files[0]);

    await postAction(formData, true);
}

function addReceiptGroup() {
    const list = document.getElementById("receiptGroupList");
    const receiptIndex = receiptGroupIndex++;

    if (!list) {
        return;
    }

    const div = document.createElement("div");
    div.className = "receipt-group-card";
    div.dataset.receiptIndex = receiptIndex;

    div.innerHTML = `
        <div class="receipt-group-header">
            <h4>Receipt / Shop ${receiptIndex + 1}</h4>
            <button type="button" class="btn-remove-small" onclick="removeReceiptGroup(${receiptIndex})">Remove</button>
        </div>

        <div class="input-group">
            <label>Shop / Receipt Label</label>
            <input type="text" class="input-field receipt-label" data-receipt-index="${receiptIndex}" placeholder="Example: Kedai Elektrik Maju">
        </div>

        <div class="input-group">
            <label>Receipt / Price Photo</label>
            <input type="file" class="file-input receipt-file" data-receipt-index="${receiptIndex}" accept="image/*,.pdf">
        </div>

        <div class="receipt-items" id="receiptItems_${receiptIndex}"></div>

        <button class="secondary-btn btn-add-item-small" type="button" onclick="addMaterialRow(${receiptIndex})">Add Item Under This Receipt</button>
    `;

    list.appendChild(div);
    addMaterialRow(receiptIndex);
}

function removeReceiptGroup(receiptIndex) {
    const groups = document.querySelectorAll(".receipt-group-card");

    if (groups.length <= 1) {
        showCustomAlert("Failed!", "At least one receipt or shop group is required.", "error");
        return;
    }

    const group = document.querySelector(`.receipt-group-card[data-receipt-index="${receiptIndex}"]`);

    if (group) {
        group.remove();
    }
}

function addMaterialRow(receiptIndex) {
    const list = document.getElementById(`receiptItems_${receiptIndex}`);

    if (!list) {
        return;
    }

    const itemIndex = list.querySelectorAll(".material-row").length;

    const div = document.createElement("div");
    div.className = "material-row";
    div.dataset.receiptIndex = receiptIndex;
    div.dataset.itemIndex = itemIndex;

    div.innerHTML = `
        <div class="material-row-title">
            <span>Item ${itemIndex + 1}</span>
            <button type="button" class="btn-remove-small" onclick="removeMaterialRow(this)">Remove</button>
        </div>

        <div class="input-group">
            <label>Item Name</label>
            <input type="text" class="input-field material-name" placeholder="Example: LED bulb">
        </div>

        <div class="grid-2">
            <div class="input-group">
                <label>Quantity</label>
                <input type="number" class="input-field material-qty" min="1" value="1">
            </div>

            <div class="input-group">
                <label>Price (RM)</label>
                <input type="number" class="input-field material-price" min="0" step="0.01" placeholder="0.00">
            </div>
        </div>
    `;

    list.appendChild(div);
}

function removeMaterialRow(button) {
    const row = button.closest(".material-row");
    const receiptItems = button.closest(".receipt-items");

    if (!row || !receiptItems) {
        return;
    }

    if (receiptItems.querySelectorAll(".material-row").length <= 1) {
        showCustomAlert("Failed!", "Each receipt must have at least one item.", "error");
        return;
    }

    row.remove();
    renumberReceiptItems(receiptItems);
}

function renumberReceiptItems(receiptItems) {
    const rows = receiptItems.querySelectorAll(".material-row");

    rows.forEach(function(row, index) {
        row.dataset.itemIndex = index;
        const title = row.querySelector(".material-row-title span");

        if (title) {
            title.innerText = `Item ${index + 1}`;
        }
    });
}

async function completeWork(bookingId) {
    const completeInput = document.getElementById("completionEvidenceInput");
    const laborChargeInput = document.getElementById("laborChargeInput");
    const receiptGroups = Array.from(document.querySelectorAll(".receipt-group-card"));

    if (!laborChargeInput || laborChargeInput.value.trim() === "") {
        showCustomAlert("Failed!", "Please enter your work labour charge first.", "error");
        return;
    }

    const laborCharge = parseFloat(laborChargeInput.value.trim());

    if (isNaN(laborCharge) || laborCharge < 0) {
        showCustomAlert("Failed!", "Work labour charge is invalid.", "error");
        return;
    }

    if (receiptGroups.length < 1) {
        showCustomAlert("Failed!", "Please add at least one receipt or shop group.", "error");
        return;
    }

    if (!completeInput || !completeInput.files || completeInput.files.length === 0) {
        showCustomAlert("Failed!", "Please attach completed work photo first.", "error");
        return;
    }

    const formData = new FormData();
    formData.append("action", "completed");
    formData.append("booking_id", bookingId);
    formData.append("labor_charge", laborCharge.toFixed(2));
    formData.append("receipt_count", receiptGroups.length);
    formData.append("completion_evidence", completeInput.files[0]);

    for (let r = 0; r < receiptGroups.length; r++) {
        const group = receiptGroups[r];
        const labelInput = group.querySelector(".receipt-label");
        const receiptFileInput = group.querySelector(".receipt-file");
        const materialRows = Array.from(group.querySelectorAll(".material-row"));

        const receiptLabel = labelInput && labelInput.value.trim() !== "" ? labelInput.value.trim() : `Receipt ${r + 1}`;

        if (!receiptFileInput || !receiptFileInput.files || receiptFileInput.files.length === 0) {
            showCustomAlert("Failed!", `Please attach receipt photo for ${receiptLabel}.`, "error");
            return;
        }

        if (materialRows.length < 1) {
            showCustomAlert("Failed!", `Please add at least one item for ${receiptLabel}.`, "error");
            return;
        }

        formData.append(`receipt_label_${r}`, receiptLabel);
        formData.append(`receipt_file_${r}`, receiptFileInput.files[0]);
        formData.append(`item_count_${r}`, materialRows.length);

        for (let i = 0; i < materialRows.length; i++) {
            const nameInput = materialRows[i].querySelector(".material-name");
            const qtyInput = materialRows[i].querySelector(".material-qty");
            const priceInput = materialRows[i].querySelector(".material-price");

            const name = nameInput ? nameInput.value.trim() : "";
            const qty = qtyInput ? qtyInput.value.trim() : "";
            const price = priceInput ? priceInput.value.trim() : "";

            if (!name || !qty || !price) {
                showCustomAlert("Failed!", `Please complete all item details under ${receiptLabel}.`, "error");
                return;
            }

            if (parseInt(qty) <= 0) {
                showCustomAlert("Failed!", `Quantity must be at least 1 under ${receiptLabel}.`, "error");
                return;
            }

            if (parseFloat(price) < 0) {
                showCustomAlert("Failed!", `Price is invalid under ${receiptLabel}.`, "error");
                return;
            }

            formData.append(`item_name_${r}_${i}`, name);
            formData.append(`item_qty_${r}_${i}`, qty);
            formData.append(`item_price_${r}_${i}`, price);
        }
    }

    await postAction(formData, true);
}

async function submitRating(bookingId) {
    const comment = document.getElementById("ratingComment").value.trim();

    if (selectedRating <= 0) {
        showCustomAlert("Failed!", "Please select your rating first.", "error");
        return;
    }

    if (!comment) {
        showCustomAlert("Failed!", "Please write your comment before submitting.", "error");
        return;
    }

    const formData = new URLSearchParams();
    formData.append("action", "submit_rating");
    formData.append("booking_id", bookingId);
    formData.append("rating_val", selectedRating);
    formData.append("comment", comment);

    await postAction(formData, false);
}

async function postAction(bodyData, isMultipart) {
    stopTrackingRefresh();

    try {
        const fetchOptions = {
            method: "POST",
            credentials: "same-origin",
            body: bodyData
        };

        if (!isMultipart) {
            fetchOptions.headers = {
                "Content-Type": "application/x-www-form-urlencoded"
            };
            fetchOptions.body = bodyData.toString();
        }

        const response = await fetch(`${baseURL}/TrackingVendorServlet`, fetchOptions);
        const data = await response.json();

        if (data.status === "success") {
            const area = document.getElementById("actionArea");

            if (area) {
                area.innerHTML = "";
            }

            showCustomAlert("Success!", data.message || "Updated successfully.", "success", function() {
                fetchTrackingStatus(true);
            });

            fetchTrackingStatus(true);
        } else {
            showCustomAlert("Failed!", data.message || "Update failed.", "error");
        }

    } catch (error) {
        showCustomAlert("Failed!", "System connection error.", "error");
    }
}

function selectRating(value) {
    selectedRating = value;

    const buttons = document.querySelectorAll(".rating-options button");

    buttons.forEach(function(btn, index) {
        btn.classList.remove("active");
        btn.classList.remove("inactive-star");
        btn.innerHTML = "★";

        if (index < value) {
            btn.classList.add("active");
        } else {
            btn.classList.add("inactive-star");
        }
    });
}

function renderStarText(value) {
    const rating = Math.max(0, Math.min(5, parseInt(value) || 0));
    let stars = "";

    for (let i = 1; i <= 5; i++) {
        if (i <= rating) {
            stars += '<span style="color:#f5c542;font-weight:800;text-shadow:0 1px 1px rgba(0,0,0,0.14);">★</span>';
        } else {
            stars += '<span style="color:#f5c542;opacity:0.35;font-weight:800;text-shadow:0 1px 1px rgba(0,0,0,0.14);">★</span>';
        }
    }

    return stars;
}

function goChat(bookingId) {
    const role = latestData && latestData.viewer_role ? latestData.viewer_role : getRequestedView();

    if (role) {
        sessionStorage.setItem("tukangnow_chat_view", role);
        window.location.href = `chatvendor.html?id=${encodeURIComponent(bookingId)}&view=${encodeURIComponent(role)}`;
        return;
    }

    window.location.href = `chatvendor.html?id=${encodeURIComponent(bookingId)}`;
}

function goReport(bookingId) {
    window.location.href = `report.html?id=${encodeURIComponent(bookingId)}`;
}

function goPayBalance(bookingId) {
    const pendingBooking = {
        booking_id: bookingId,
        payment_type: "second_payment",
        amount: latestData ? String(latestData.totalbalance || "0.00") : "0.00",
        final_amount: latestData ? String(latestData.totalbalance || "0.00") : "0.00",
        service: latestData ? latestData.booking_service : "Service",
        category: latestData ? latestData.booking_service : "Service",
        service_id: latestData ? latestData.service_id : 0,
        problem: latestData ? latestData.problem : "",
        subservicebooked: latestData ? latestData.subservicebooked : "",
        userName: latestData ? latestData.customer_name : "Customer",
        userEmail: latestData ? latestData.customer_email : "customer@email.com",
        userPhone: latestData ? latestData.customer_phone : "0123456789",
        travelFee: "0",
        distanceKm: latestData ? String(latestData.distance_km || "0") : "0"
    };

    sessionStorage.setItem("pendingBooking", JSON.stringify(pendingBooking));
    window.location.href = `payment.html?bookingId=${encodeURIComponent(bookingId)}&type=second_payment`;
}

function renderMap(data) {
    let vLoc = normalizeMalaysiaLocation(parseFloat(data.vendor_lat) || 0, parseFloat(data.vendor_lng) || 0);
    let cLoc = normalizeMalaysiaLocation(parseFloat(data.cust_lat) || 0, parseFloat(data.cust_lng) || 0);

    if (!cLoc) {
        cLoc = MALAYSIA_CENTER;
    }

    if (!vLoc) {
        vLoc = [cLoc[0] + 0.003, cLoc[1] + 0.003];
    }

    initMap(vLoc[0], vLoc[1], cLoc[0], cLoc[1]);
    updateMapMarkersOnly(vLoc, cLoc);
    drawCachedRouteIfAvailable(vLoc, cLoc);
    requestRoadRouteIfNeeded(vLoc, cLoc, data);
    followVendorIfEnabled(vLoc);

    if (map) {
        setTimeout(function() {
            map.invalidateSize();
        }, 200);
    }
}

function initMap(vLat, vLng, cLat, cLng) {
    if (mapInitialized) {
        return;
    }

    map = L.map("map", {
        zoomControl: false,
        dragging: true,
        touchZoom: true,
        scrollWheelZoom: true,
        doubleClickZoom: true,
        maxBounds: MALAYSIA_BOUNDS.pad(0.2),
        maxBoundsViscosity: 1.0,
        minZoom: 7
    }).setView([cLat, cLng], 15);

    L.control.zoom({
        position: "topright"
    }).addTo(map);

    addVendorFollowControl();

    map.on("movestart", function() {
        if (!systemMapMove && !followVendorMode) {
            userInteracted = true;
        }
    });

    map.on("zoomstart", function() {
        if (!systemMapMove && !followVendorMode) {
            userInteracted = true;
        }
    });

    map.on("moveend", function() {
        systemMapMove = false;
    });

    map.on("zoomend", function() {
        systemMapMove = false;
    });

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        maxNativeZoom: 19,
        attribution: "© OpenStreetMap contributors"
    }).addTo(map);

    const vendorIcon = L.divIcon({
        html: "🚗",
        className: "map-icon",
        iconSize: [32, 32]
    });

    const customerIcon = L.divIcon({
        html: "🏠",
        className: "map-icon",
        iconSize: [32, 32]
    });

    vendorMarker = L.marker([vLat, vLng], { icon: vendorIcon }).addTo(map);
    customerMarker = L.marker([cLat, cLng], { icon: customerIcon }).addTo(map);

    routeLine = L.polyline([], {
        color: "red",
        weight: 5,
        opacity: 0.85,
        lineJoin: "round",
        lineCap: "round"
    }).addTo(map);

    try {
        systemMapMove = true;
        map.fitBounds(L.latLngBounds([[vLat, vLng], [cLat, cLng]]), { padding: [50, 50], maxZoom: 16 });
    } catch (e) {
        systemMapMove = true;
        map.setView(MALAYSIA_CENTER, 13);
    }

    mapInitialized = true;
}

function addVendorFollowControl() {
    const VendorFollowControl = L.Control.extend({
        options: {
            position: "topright"
        },
        onAdd: function() {
            const container = L.DomUtil.create("div", "leaflet-control vendor-follow-control");
            const button = L.DomUtil.create("button", "vendor-follow-btn", container);

            button.type = "button";
            button.innerHTML = "🎯";
            button.title = "Follow vendor vehicle";
            button.setAttribute("aria-label", "Follow vendor vehicle");

            L.DomEvent.disableClickPropagation(container);
            L.DomEvent.disableScrollPropagation(container);

            L.DomEvent.on(button, "click", function(e) {
                L.DomEvent.stopPropagation(e);
                L.DomEvent.preventDefault(e);
                toggleVendorFollowMode();
            });

            followButtonElement = button;
            updateFollowButtonState();

            return container;
        }
    });

    map.addControl(new VendorFollowControl());
}

function toggleVendorFollowMode() {
    followVendorMode = !followVendorMode;
    userInteracted = !followVendorMode;
    updateFollowButtonState();

    if (followVendorMode) {
        centerMapToVendor(true);
    }
}

function updateFollowButtonState() {
    if (!followButtonElement) {
        return;
    }

    if (followVendorMode) {
        followButtonElement.classList.add("active");
        followButtonElement.title = "Following vendor vehicle";
        followButtonElement.setAttribute("aria-label", "Following vendor vehicle");
    } else {
        followButtonElement.classList.remove("active");
        followButtonElement.title = "Follow vendor vehicle";
        followButtonElement.setAttribute("aria-label", "Follow vendor vehicle");
    }
}

function centerMapToVendor(forceZoom) {
    if (!map || !vendorMarker) {
        return;
    }

    const loc = vendorMarker.getLatLng();

    if (!loc || !isValidMalaysiaCoordinate(loc.lat, loc.lng)) {
        return;
    }

    systemMapMove = true;

    if (forceZoom) {
        map.setView([loc.lat, loc.lng], Math.max(map.getZoom(), FOLLOW_VENDOR_ZOOM), {
            animate: true,
            duration: 0.6
        });
    } else {
        map.panTo([loc.lat, loc.lng], {
            animate: true,
            duration: 0.6
        });
    }
}

function followVendorIfEnabled(vLoc) {
    if (!followVendorMode || !map || !vLoc || !isValidMalaysiaCoordinate(vLoc[0], vLoc[1])) {
        return;
    }

    systemMapMove = true;

    if (map.getZoom() < FOLLOW_VENDOR_ZOOM) {
        map.setView(vLoc, FOLLOW_VENDOR_ZOOM, {
            animate: true,
            duration: 0.6
        });
        return;
    }

    map.panTo(vLoc, {
        animate: true,
        duration: 0.6
    });
}

function updateMapMarkersOnly(vLoc, cLoc) {
    if (!mapInitialized || !map) {
        return;
    }

    const finalVendorLoc = normalizeMalaysiaLocation(vLoc[0], vLoc[1]);
    const finalCustomerLoc = normalizeMalaysiaLocation(cLoc[0], cLoc[1]);

    if (!finalVendorLoc || !finalCustomerLoc) {
        return;
    }

    if (vendorMarker) {
        animateMarkerTo(vendorMarker, finalVendorLoc, MARKER_ANIMATION_MS);
    }

    if (customerMarker) {
        customerMarker.setLatLng(finalCustomerLoc);
    }
}

function animateMarkerTo(marker, targetLoc, durationMs) {
    if (!marker || !targetLoc) {
        return;
    }

    const current = marker.getLatLng();

    if (!current) {
        marker.setLatLng(targetLoc);
        return;
    }

    const startLat = current.lat;
    const startLng = current.lng;
    const endLat = targetLoc[0];
    const endLng = targetLoc[1];

    if (!isValidMalaysiaCoordinate(endLat, endLng)) {
        return;
    }

    const distance = calculateDistanceKm(startLat, startLng, endLat, endLng);

    if (distance > 3) {
        marker.setLatLng(targetLoc);
        return;
    }

    if (distance < 0.005) {
        marker.setLatLng(targetLoc);
        return;
    }

    const startTime = performance.now();

    function step(now) {
        const progress = Math.min((now - startTime) / durationMs, 1);
        const eased = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2;
        const nextLat = startLat + (endLat - startLat) * eased;
        const nextLng = startLng + (endLng - startLng) * eased;

        marker.setLatLng([nextLat, nextLng]);

        if (progress < 1) {
            requestAnimationFrame(step);
        }
    }

    requestAnimationFrame(step);
}

function requestRoadRouteIfNeeded(vLoc, cLoc, data) {
    if (routeFetchInProgress) {
        return;
    }

    if (!isValidMalaysiaCoordinate(vLoc[0], vLoc[1]) || !isValidMalaysiaCoordinate(cLoc[0], cLoc[1])) {
        return;
    }

    if (lastRoadRouteVendorLoc && lastRoadRouteCustomerLoc && activeRoadRoutePoints.length > 1) {
        const vendorMove = calculateDistanceKm(lastRoadRouteVendorLoc[0], lastRoadRouteVendorLoc[1], vLoc[0], vLoc[1]);
        const customerMove = calculateDistanceKm(lastRoadRouteCustomerLoc[0], lastRoadRouteCustomerLoc[1], cLoc[0], cLoc[1]);

        if (vendorMove < ROUTE_REFETCH_DISTANCE_KM && customerMove < CUSTOMER_ROUTE_CHANGE_KM) {
            return;
        }
    }

    fetchRoadRoute(vLoc, cLoc, data);
}

async function fetchRoadRoute(vLoc, cLoc, data) {
    if (!isValidMalaysiaCoordinate(vLoc[0], vLoc[1]) || !isValidMalaysiaCoordinate(cLoc[0], cLoc[1])) {
        return;
    }

    const requestId = ++routeRequestSerial;
    routeFetchInProgress = true;

    const url = `https://router.project-osrm.org/route/v1/driving/${vLoc[1]},${vLoc[0]};${cLoc[1]},${cLoc[0]}?overview=full&geometries=geojson&steps=false`;

    try {
        const response = await fetch(url);
        const result = await response.json();

        if (requestId !== routeRequestSerial) {
            return;
        }

        if (!result || result.code !== "Ok" || !result.routes || result.routes.length === 0) {
            return;
        }

        const route = result.routes[0];

        if (!route.geometry || !route.geometry.coordinates) {
            return;
        }

        const routePoints = route.geometry.coordinates
            .map(function(coord) {
                return [coord[1], coord[0]];
            })
            .filter(function(point) {
                return isValidMalaysiaCoordinate(point[0], point[1]);
            });

        if (routePoints.length < 2) {
            return;
        }

        activeRoadRoutePoints = routePoints;
        lastRoadRouteVendorLoc = [vLoc[0], vLoc[1]];
        lastRoadRouteCustomerLoc = [cLoc[0], cLoc[1]];

        drawCachedRouteIfAvailable(vLoc, cLoc);

        const minutes = Math.max(1, Math.round((route.duration || 0) / 60));
        const km = ((route.distance || 0) / 1000).toFixed(1);

        const durDistText = document.getElementById("durationDistanceText");
        const etaText = document.getElementById("etaText");

        if (durDistText) {
            durDistText.innerText = `${minutes} mins away (${km} km remaining)`;
        }

        if (etaText) {
            etaText.innerText = `Estimated arrival by ${buildEtaFromSeconds(route.duration || 0, data.eta)}`;
        }

    } catch (error) {
        drawCachedRouteIfAvailable(vLoc, cLoc);
    } finally {
        if (requestId === routeRequestSerial) {
            routeFetchInProgress = false;
        }
    }
}

function drawCachedRouteIfAvailable(vLoc, cLoc) {
    if (!mapInitialized || !map || !routeLine) {
        return;
    }

    const finalVendorLoc = normalizeMalaysiaLocation(vLoc[0], vLoc[1]);
    const finalCustomerLoc = normalizeMalaysiaLocation(cLoc[0], cLoc[1]);

    if (!finalVendorLoc || !finalCustomerLoc) {
        return;
    }

    if (!activeRoadRoutePoints || activeRoadRoutePoints.length < 2) {
        routeLine.setLatLngs([]);
        fitMapToMarkers(finalVendorLoc, finalCustomerLoc);
        return;
    }

    const remainingRoute = filterRemainingRoute(finalVendorLoc, activeRoadRoutePoints);

    if (!remainingRoute || remainingRoute.length < 2) {
        routeLine.setLatLngs(activeRoadRoutePoints);
        fitMapToRoute(activeRoadRoutePoints, finalVendorLoc, finalCustomerLoc);
        return;
    }

    routeLine.setLatLngs(remainingRoute);
    fitMapToRoute(remainingRoute, finalVendorLoc, finalCustomerLoc);
}

function filterRemainingRoute(vendorLoc, fullRoute) {
    if (!fullRoute || fullRoute.length === 0) {
        return [];
    }

    let closestIndex = 0;
    let minDistance = Infinity;

    for (let i = 0; i < fullRoute.length; i++) {
        const dist = calculateDistanceKm(vendorLoc[0], vendorLoc[1], fullRoute[i][0], fullRoute[i][1]);

        if (dist < minDistance) {
            minDistance = dist;
            closestIndex = i;
        }
    }

    const remaining = fullRoute.slice(closestIndex);

    if (remaining.length < 2) {
        return fullRoute;
    }

    return remaining;
}

function fitMapToMarkers(vLoc, cLoc) {
    if (!map || userInteracted || followVendorMode) {
        return;
    }

    const now = Date.now();

    if (now - lastAutoFitAt < AUTO_FIT_INTERVAL_MS) {
        return;
    }

    lastAutoFitAt = now;

    try {
        systemMapMove = true;
        map.fitBounds(L.latLngBounds([vLoc, cLoc]), { padding: [50, 50], maxZoom: 16 });
    } catch (e) {
        systemMapMove = true;
        map.setView(MALAYSIA_CENTER, 13);
    }
}

function fitMapToRoute(points, vLoc, cLoc) {
    if (!map || userInteracted || followVendorMode) {
        return;
    }

    const now = Date.now();

    if (now - lastAutoFitAt < AUTO_FIT_INTERVAL_MS) {
        return;
    }

    lastAutoFitAt = now;

    try {
        const bounds = L.latLngBounds(points.concat([vLoc, cLoc]));
        systemMapMove = true;
        map.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
    } catch (e) {
        fitMapToMarkers(vLoc, cLoc);
    }
}

function isValidMalaysiaCoordinate(latitude, longitude) {
    return latitude >= 0.5 && latitude <= 7.8 && longitude >= 99.0 && longitude <= 119.5;
}

function normalizeMalaysiaLocation(latitude, longitude) {
    if (!isValidMalaysiaCoordinate(latitude, longitude)) {
        return null;
    }

    return [latitude, longitude];
}

function calculateDistanceKm(lat1, lon1, lat2, lon2) {
    if (!isValidMalaysiaCoordinate(lat1, lon1) || !isValidMalaysiaCoordinate(lat2, lon2)) {
        return 0;
    }

    const earthRadiusKm = 6371.0;
    const latDistance = toRadians(lat2 - lat1);
    const lonDistance = toRadians(lon2 - lon1);

    const a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return earthRadiusKm * c;
}

function toRadians(value) {
    return value * Math.PI / 180;
}

function buildEtaFromSeconds(seconds, fallback) {
    if (!seconds || seconds <= 0) {
        return fallback || "--:--";
    }

    const date = new Date(Date.now() + seconds * 1000);
    const hour = String(date.getHours()).padStart(2, "0");
    const minute = String(date.getMinutes()).padStart(2, "0");

    return `${hour}:${minute}`;
}

function normalizeTrackingStatus(status) {
    const clean = String(status || "").trim().toLowerCase();

    if (clean === "accepted") {
        return "Accepted";
    }

    if (clean === "on the way" || clean === "on theway" || clean === "ontheway") {
        return "On The Way";
    }

    if (clean === "arrived") {
        return "Arrived";
    }

    if (clean === "started" || clean === "work started") {
        return "Started";
    }

    if (clean === "second payment" || clean === "second_payment") {
        return "Second Payment";
    }

    if (clean === "completed") {
        return "Completed";
    }

    if (clean === "rated") {
        return "Rated";
    }

    return "Accepted";
}

function resolveProfilePath(profilePath) {
    if (!profilePath || String(profilePath).trim() === "") {
        return "image/profile.png";
    }

    const cleanPath = String(profilePath).trim();

    if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
        return cleanPath;
    }

    if (cleanPath.startsWith("image/")) {
        return cleanPath;
    }

    if (cleanPath.startsWith("profiles/")) {
        return `${baseURL}/${cleanPath}`;
    }

    return `${baseURL}/profiles/${cleanPath}`;
}

function formatMoney(value) {
    const amount = parseFloat(value);

    if (isNaN(amount)) {
        return "0.00";
    }

    return amount.toFixed(2);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
    return escapeHtml(value).replaceAll("`", "&#096;");
}