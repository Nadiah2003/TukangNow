const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

let allVendors = [];

document.addEventListener('DOMContentLoaded', function() {
    const adminName = sessionStorage.getItem("userName");
    document.getElementById('adminName').innerText = adminName || "Admin";

    fetchDashboardData();

    document.getElementById('vendorSearch').addEventListener('input', function(e) {
        const term = e.target.value.toLowerCase();

        const filtered = allVendors.filter(v =>
            (v.name && v.name.toLowerCase().includes(term)) ||
            (v.phone && v.phone.includes(term))
        );

        renderVendorTable(filtered);
    });

    document.getElementById('filterSelect').addEventListener('change', function() {
        sortVendors();
    });

    document.getElementById('modalCloseBtn').addEventListener('click', function() {
        closeModal();
    });
});

function showModal(title, message) {
    document.getElementById('modalTitle').innerText = title;
    document.getElementById('modalMessage').innerText = message;
    document.getElementById('customModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('customModal').style.display = 'none';
}

function fetchDashboardData() {
    fetch(`${baseURL}/HomeAdminServlet`, {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(res => {
        if (res.status === 401) {
            window.location.href = "login.html";
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

        if (data.error) {
            showModal("Failed!", data.error);
            return;
        }

        document.getElementById('newAppCount').innerText = data.newApps || 0;
        document.getElementById('activeVendorCount').innerText = data.activeVendors || 0;
        document.getElementById('newReportCount').innerText = data.newReports || 0;

        const adminProfileImg = document.getElementById('adminProfileImg');
        adminProfileImg.src = resolveProfilePath(data.adminProfile);
        adminProfileImg.onerror = function() {
            this.src = "image/profile.png";
        };

        allVendors = data.vendors || [];
        renderVendorTable(allVendors);
    })
    .catch(err => {
        showModal("Failed!", err.message || "Server returned error. Please check database connection.");
    });
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

    return `${baseURL}/licences/${encodeURIComponent(onlyName)}`;
}

function getOnlyFileName(path) {
    return String(path)
        .replaceAll("\\", "/")
        .split("/")
        .pop();
}

function renderVendorTable(vendors) {
    const tbody = document.getElementById('vendorTableBody');

    if (!vendors || vendors.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 20px;">No vendors found.</td></tr>`;
        return;
    }

    tbody.innerHTML = vendors.map(v => {
        const fileNames = v.docUrl ? v.docUrl.split(',') : [];

        const fileLinksHTML = fileNames.map(fileName => {
            const trimmedName = fileName.trim();

            if (!trimmedName) {
                return '';
            }

            const onlyName = getOnlyFileName(trimmedName);
            const parts = onlyName.split('_');
            const displayName = parts.length > 2 ? parts.slice(2).join('_') : onlyName;
            const docHref = resolveDocPath(trimmedName);

            return `
                <div style="margin-bottom: 4px;">
                    <a href="${escapeHtml(docHref)}" target="_blank" 
                       style="color:#1848a0; font-weight:600; text-decoration:none; font-size:0.7rem; display: flex; align-items: center; gap: 4px;">
                        📄 ${escapeHtml(displayName)}
                    </a>
                </div>`;
        }).join('');

        const profileImgSrc = resolveProfilePath(v.profilePath);
        const statusText = v.status ? v.status : "inactive";
        const statusClass = statusText.toLowerCase() === 'active' ? 'active-pill' : 'expired-pill';

        return `
            <tr>
                <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <img src="${escapeHtml(profileImgSrc)}"
                             style="width: 38px; height: 38px; border-radius: 50%; object-fit: cover; border: 1px solid #eee;"
                             onerror="this.src='image/profile.png'">
                        <div>
                            <strong style="font-size:0.85rem;">${escapeHtml(v.name || 'No Name')}</strong><br>
                            <span style="font-size: 0.75rem; color:#666;">${escapeHtml(v.phone || 'No Phone')}</span>
                        </div>
                    </div>
                </td>
                <td>${fileLinksHTML || '<span style="color:#999; font-size:0.75rem;">No License</span>'}</td>
                <td>
                    <span style="color:#f39c12; font-weight:bold; font-size:0.8rem;">★ ${escapeHtml(v.rating || '0.0')}</span><br>
                    <span style="font-size: 0.7rem; color:#888;">${escapeHtml(v.workDone || '0')} jobs</span>
                </td>
                <td>
                    <span class="status-pill ${statusClass}">
                        ${escapeHtml(statusText.toUpperCase())}
                    </span>
                </td>
                <td>
                    <button onclick="viewVendorDetails('${escapeJs(v.phone || '')}')" class="view-btn">
                        VIEW
                    </button>
                </td>
            </tr>`;
    }).join('');
}

function sortVendors() {
    const sortType = document.getElementById('filterSelect').value;
    let sorted = [...allVendors];

    if (sortType === "az") {
        sorted.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
    } else if (sortType === "za") {
        sorted.sort((a, b) => (b.name || '').localeCompare(a.name || ''));
    } else if (sortType === "rating") {
        sorted.sort((a, b) => parseFloat(b.rating || 0) - parseFloat(a.rating || 0));
    } else if (sortType === "newest") {
        sorted.reverse();
    }

    renderVendorTable(sorted);
}

function viewVendorDetails(phone) {
    if (!phone || phone === 'No Phone') {
        showModal("Error", "Phone number not found for this vendor.");
        return;
    }

    sessionStorage.setItem("viewVendorPhone", phone);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeJs(value) {
    return String(value)
        .replaceAll("\\", "\\\\")
        .replaceAll("'", "\\'")
        .replaceAll('"', '\\"');
}