// PEMBETULAN: Ditukar kepada TukangNow berasaskan permintaan anda
const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

document.addEventListener('DOMContentLoaded', function() {
    const dateInput = document.getElementById('searchDate');
    const now = new Date();
    const minDate = now.toISOString().split('T')[0];
    if (dateInput) {
        dateInput.setAttribute('min', minDate);
        dateInput.value = minDate; 
    }
});

function performSearch() {
    const serviceName = document.getElementById('serviceType').value;
    const radiusValue = document.getElementById('searchRadius').value;
    const dateValue = document.getElementById('searchDate').value;

    if (!serviceName || !radiusValue || !dateValue) {
        showModal("Selection Required", "Please select a service type, radius range and date.", false);
        return;
    }

    showModal("Location Access", "Requesting current location coordinates...", true);

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                
                showModal("Searching", "Searching for available nearby vendors...", true);

                // PEMBETULAN: Ditukar endpoint dari api/search (PHP style) ke SearchServlet (Java style)
                fetch(`${baseURL}/SearchServlet?serviceName=${encodeURIComponent(serviceName)}&radius=${radiusValue}&date=${dateValue}&latitude=${lat}&longitude=${lng}`)
                .then(res => res.json())
                .then(data => {
                    const modalTitle = document.getElementById('statusTitle');
                    const modalMessage = document.getElementById('statusMessage');
                    const iconCircle = document.getElementById('statusIconCircle');

                    if (data && data.length > 0) {
                        modalTitle.innerText = "Vendors Found";
                        iconCircle.style.display = "none"; 
                        
                        let resultsHTML = '<div class="vendor-list-container">';
                        data.forEach(v => {
                            resultsHTML += `
                                <div class="vendor-card-modal">
                                    <div class="vendor-info-modal">
                                        <h3>${v.vendorName}</h3>
                                        <p style="color: #22c55e; font-weight: 600;">📍 Distance: ${v.distance} KM</p>
                                        <p><strong>Sub:</strong> ${v.subservice}</p>
                                        <p><strong>State:</strong> ${v.state}</p>
                                        <p style="color: #1848a0; font-weight: 600;">🕒 Slots: ${v.availTime}</p>
                                    </div>
                                    <button onclick="goToLogin()" class="book-btn">BOOK</button>
                                </div>
                            `;
                        });
                        resultsHTML += '</div>';
                        modalMessage.innerHTML = resultsHTML;
                    } else {
                        modalTitle.innerText = "No Availability";
                        iconCircle.style.display = "flex";
                        iconCircle.style.background = "#d9534f";
                        document.getElementById('statusIcon').innerText = "✖";
                        modalMessage.innerHTML = `
                            <div style="text-align:center; padding: 10px; color: #ff4d4d;">
                                <strong>❌ NO SERVICE AVAILABLE</strong>
                                <p style="font-size: 0.8rem; color: #666; margin-top: 5px;">No vendors found within ${radiusValue} KM. Try increasing the distance radius.</p>
                            </div>`;
                    }
                })
                .catch(err => {
                    console.error("Error:", err);
                    showModal("Error", "Server connection failed.", false);
                });
            },
            (error) => {
                console.error("Geolocation Error:", error);
                showModal("Location Error", "Failed to retrieve your location. Please allow location permissions in your browser.", false);
            }
        );
    } else {
        showModal("Not Supported", "Your browser does not support location geolocation.", false);
    }
}

function setEmergency() {
    const now = new Date();
    const dateInput = document.getElementById('searchDate');
    if(dateInput) dateInput.value = now.toISOString().split('T')[0];
    
    const serviceName = document.getElementById('serviceType').value;
    if(!serviceName) {
        showModal("Selection Required", "Please select your service type first for emergency request.", false);
        return;
    }
    performSearch();
}

function goToLogin() { 
    window.location.href = "login.html"; 
}

function showModal(title, message, isSearching = false) {
    const modal = document.getElementById('statusModal');
    const iconCircle = document.getElementById('statusIconCircle');
    const icon = document.getElementById('statusIcon');
    
    if (modal) {
        document.getElementById('statusTitle').innerText = title;
        document.getElementById('statusMessage').innerHTML = message;
        
        iconCircle.style.display = "flex";
        if (isSearching) {
            iconCircle.style.background = "#1848a0";
            icon.innerText = "...";
        } else {
            iconCircle.style.background = "#d9534f";
            icon.innerText = "!";
        }
        
        modal.style.display = 'flex';
        document.getElementById('statusOkBtn').onclick = () => { modal.style.display = 'none'; };
    }
}