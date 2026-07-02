const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

document.addEventListener("DOMContentLoaded", function() {
    const dateInput = document.getElementById("searchDate");
    const minDate = getLocalDateString();

    if (dateInput) {
        dateInput.setAttribute("min", minDate);
        dateInput.value = minDate;
    }
});

function getLocalDateString() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function performSearch() {
    const serviceName = document.getElementById("serviceType").value;
    const radiusValue = document.getElementById("searchRadius").value;
    const dateValue = document.getElementById("searchDate").value;

    if (!serviceName || !radiusValue || !dateValue) {
        showModal("Selection Required", "Please select a service type, radius range and date.", false);
        return;
    }

    showModal("Location Access", "Requesting current location coordinates...", true);

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;

                showModal("Searching", "Searching for available nearby vendors...", true);

                fetch(`${baseURL}/SearchServlet?serviceName=${encodeURIComponent(serviceName)}&radius=${encodeURIComponent(radiusValue)}&date=${encodeURIComponent(dateValue)}&latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lng)}&t=${Date.now()}`, {
                    method: "GET",
                    credentials: "same-origin",
                    headers: {
                        "Accept": "application/json"
                    }
                })
                .then(function(res) {
                    return res.json();
                })
                .then(function(data) {
                    if (data && data.status === "error") {
                        showModal("Error", data.message || "Unable to search vendor.", false);
                        return;
                    }

                    if (data && data.error) {
                        showModal("Error", data.error || "Unable to search vendor.", false);
                        return;
                    }

                    const results = Array.isArray(data) ? data : [];

                    const modalTitle = document.getElementById("statusTitle");
                    const modalMessage = document.getElementById("statusMessage");
                    const iconCircle = document.getElementById("statusIconCircle");

                    if (results.length > 0) {
                        modalTitle.innerText = "Vendors Found";
                        iconCircle.style.display = "none";

                        let resultsHTML = `<div class="vendor-list-container">`;

                        results.forEach(function(v) {
                            resultsHTML += `
                                <div class="vendor-card-modal">
                                    <div class="vendor-info-modal">
                                        <h3>${escapeHtml(v.vendorName || "-")}</h3>
                                        <p style="color: #22c55e; font-weight: 600;">📍 Distance: ${escapeHtml(v.distance || "0")} KM</p>
                                        <p><strong>Sub:</strong> ${escapeHtml(v.subservice || "-")}</p>
                                        <p><strong>State:</strong> ${escapeHtml(v.state || "-")}</p>
                                        <p style="color: #1848a0; font-weight: 600;">🕒 Slots: ${escapeHtml(v.availTime || "-")}</p>
                                    </div>
                                    <button onclick="goToLogin()" class="book-btn">BOOK</button>
                                </div>
                            `;
                        });

                        resultsHTML += `</div>`;
                        modalMessage.innerHTML = resultsHTML;
                    } else {
                        modalTitle.innerText = "No Availability";
                        iconCircle.style.display = "flex";
                        iconCircle.style.background = "#d9534f";
                        document.getElementById("statusIcon").innerText = "✖";
                        modalMessage.innerHTML = `
                            <div style="text-align:center; padding: 10px; color: #ff4d4d;">
                                <strong>❌ NO SERVICE AVAILABLE</strong>
                                <p style="font-size: 0.8rem; color: #666; margin-top: 5px;">No vendors found within ${escapeHtml(radiusValue)} KM for ${escapeHtml(serviceName)} on ${escapeHtml(dateValue)}. Try increasing the distance radius or choose another date.</p>
                            </div>
                        `;
                    }
                })
                .catch(function(err) {
                    console.error("Error:", err);
                    showModal("Error", "Server connection failed.", false);
                });
            },
            function(error) {
                console.error("Geolocation Error:", error);
                showModal("Location Error", "Failed to retrieve your location. Please allow location permissions in your browser.", false);
            },
            {
                enableHighAccuracy: true,
                timeout: 12000,
                maximumAge: 0
            }
        );
    } else {
        showModal("Not Supported", "Your browser does not support location geolocation.", false);
    }
}

function setEmergency() {
    const dateInput = document.getElementById("searchDate");

    if (dateInput) {
        dateInput.value = getLocalDateString();
    }

    const serviceName = document.getElementById("serviceType").value;

    if (!serviceName) {
        showModal("Selection Required", "Please select your service type first for emergency request.", false);
        return;
    }

    performSearch();
}

function goToLogin() {
    window.location.href = "login.html";
}

function showModal(title, message, isSearching = false) {
    const modal = document.getElementById("statusModal");
    const iconCircle = document.getElementById("statusIconCircle");
    const icon = document.getElementById("statusIcon");

    if (modal) {
        document.getElementById("statusTitle").innerText = title;
        document.getElementById("statusMessage").innerHTML = message;

        iconCircle.style.display = "flex";

        if (isSearching) {
            iconCircle.style.background = "#1848a0";
            icon.innerText = "...";
        } else {
            iconCircle.style.background = "#d9534f";
            icon.innerText = "!";
        }

        modal.style.display = "flex";

        document.getElementById("statusOkBtn").onclick = function() {
            modal.style.display = "none";
        };
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