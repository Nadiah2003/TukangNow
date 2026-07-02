const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let pendingAction = null;

document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('backBtn').addEventListener('click', function() {
        history.back();
    });

    document.getElementById('approveBtn').addEventListener('click', function() {
        showServiceSection();
    });

    document.getElementById('rejectBtn').addEventListener('click', function() {
        processReject();
    });

    document.getElementById('addServiceBtn').addEventListener('click', function() {
        addNewServiceRow();
    });

    document.getElementById('submitApprovalBtn').addEventListener('click', function() {
        submitApproval();
    });

    document.getElementById('modalContinueBtn').addEventListener('click', function() {
        closeModal();
    });

    const vendorId = sessionStorage.getItem("viewVendorId");

    if (vendorId) {
        fetchVendorDetails(vendorId);
    } else {
        showModal("Error", "Vendor ID not found.", true);
    }
});

function fetchVendorDetails(id) {
    fetch(`${baseURL}/DetailVendorServlet?action=get&id=${encodeURIComponent(id)}`, {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(res => res.json())
    .then(v => {
        if (v.status !== "success") {
            showModal("Error", v.message || "Failed to load vendor details.", true);
            return;
        }

        document.getElementById('vName').innerText = v.name || "N/A";
        document.getElementById('vPhone').innerText = v.phone || "N/A";
        document.getElementById('vEmail').innerText = v.email || "N/A";

        const profileImg = document.getElementById('vendorProfileImg');
        profileImg.src = resolveProfilePath(v.profilePath);
        profileImg.onerror = function() {
            profileImg.src = 'image/profile.png';
        };

        const docContainer = document.getElementById('documentLinks');

        if (v.licenseUrl && v.licenseUrl.trim() !== "") {
            const files = v.licenseUrl.split(',');
            docContainer.innerHTML = files.map(f => {
                const fileName = getOnlyFileName(f.trim());
                return `
                    <a href="${baseURL}/licences/${escapeHtml(fileName)}" target="_blank" class="doc-link">
                        📄 View License: ${escapeHtml(fileName)}
                    </a>
                `;
            }).join('');
        } else {
            docContainer.innerHTML = '<span style="color:#888;">No license uploaded.</span>';
        }
    })
    .catch(() => showModal("Error", "Failed to load vendor details.", true));
}

function showServiceSection() {
    document.getElementById('serviceSection').style.display = 'block';
}

function addNewServiceRow() {
    const container = document.getElementById('dynamicServiceList');
    const div = document.createElement('div');
    div.className = 'service-row';
    div.innerHTML = `
        <div class="input-group">
            <span class="icon">🛠</span>
            <select name="serviceName" class="service-select">
                <option value="Electrical">Electrical</option>
                <option value="Plumber">Plumber</option>
                <option value="Lawn Mower">Lawn Mower</option>
            </select>
        </div>
        <button type="button" class="btn-remove">REMOVE</button>
    `;

    div.querySelector('.btn-remove').addEventListener('click', function() {
        div.remove();
    });

    container.appendChild(div);
}

function submitApproval() {
    const vendorId = sessionStorage.getItem("viewVendorId");
    const expiry = document.getElementById('expireDate').value;
    const services = Array.from(document.querySelectorAll('.service-select')).map(s => s.value);

    if (!vendorId) {
        showModal("Failed!", "Vendor ID not found.", true);
        return;
    }

    if (!expiry || services.length === 0) {
        showModal("Failed!", "Please select expiry date and at least one service.", true);
        return;
    }

    fetch(`${baseURL}/DetailVendorServlet`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 
            'Content-Type': 'application/x-www-form-urlencoded' 
        },
        body: `action=approve&id=${encodeURIComponent(vendorId)}&expiry=${encodeURIComponent(expiry)}&services=${encodeURIComponent(services.join(','))}`
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === "success") {
            showModal("Success!", "Vendor activated successfully!", false, { redirectOnClose: true });
        } else {
            showModal("Error!", data.message || "Approval failed.", true);
        }
    })
    .catch(() => showModal("Error!", "Connection to server failed.", true));
}

function processReject() {
    showModal("Confirm Rejection", "Are you sure you want to reject this application?", true, { onContinue: sendReject });
}

function sendReject() {
    const vendorId = sessionStorage.getItem("viewVendorId");

    if (!vendorId) {
        showModal("Error!", "Vendor ID not found.", true);
        return;
    }

    fetch(`${baseURL}/DetailVendorServlet`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 
            'Content-Type': 'application/x-www-form-urlencoded' 
        },
        body: `action=updateStatus&id=${encodeURIComponent(vendorId)}&status=rejected`
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === "success") {
            showModal("Rejected", "The application has been rejected.", false, { redirectOnClose: true });
        } else {
            showModal("Error!", data.message || "Rejection failed.", true);
        }
    })
    .catch(() => showModal("Error!", "Connection to server failed.", true));
}

function showModal(title, message, isError = true, options = {}) {
    const modal = document.getElementById('statusModal');
    const icon = document.getElementById('modalIcon');

    document.getElementById('modalTitle').innerText = title;
    document.getElementById('modalMessage').innerText = message;

    icon.className = isError ? "modal-icon error" : "modal-icon success";
    icon.innerText = isError ? "✕" : "✓";

    pendingAction = typeof options.onContinue === "function" ? options.onContinue : null;
    modal.dataset.redirect = options.redirectOnClose ? "1" : "0";
    modal.dataset.target = options.target || "newapplication.html";
    modal.style.display = "flex";
}

function closeModal() {
    const modal = document.getElementById('statusModal');

    if (pendingAction) {
        const action = pendingAction;
        pendingAction = null;
        modal.style.display = "none";
        action();
        return;
    }

    modal.style.display = "none";

    if (modal.dataset.redirect === "1") {
        window.location.href = modal.dataset.target;
    }
}

function resolveProfilePath(path) {
    if (!path || path === "null" || path.trim() === "") {
        return "image/profile.png";
    }

    const cleanPath = path.trim();

    if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
        return cleanPath;
    }

    if (cleanPath.startsWith("image/")) {
        return cleanPath;
    }

    return `${baseURL}/profiles/${getOnlyFileName(cleanPath)}`;
}

function getOnlyFileName(path) {
    return String(path)
        .replaceAll("\\", "/")
        .split("/")
        .pop();
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}