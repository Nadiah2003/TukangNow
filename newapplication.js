const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let allVendors = [];

document.addEventListener('DOMContentLoaded', function() {
    fetchNewApplications();

    document.getElementById('vendorSearch').addEventListener('input', function() {
        filterSearchApplications();
    });

    document.getElementById('filterSelect').addEventListener('change', function() {
        sortApplications();
    });
});

function fetchNewApplications() {
    fetch(`${baseURL}/NewApplicationServlet`, {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(res => {
        if (!res.ok) {
            throw new Error("Server returned error " + res.status);
        }

        return res.json();
    })
    .then(data => {
        if (data.error) {
            console.error(data.error);
            allVendors = [];
            renderTable(allVendors);
            return;
        }

        allVendors = Array.isArray(data) ? data : [];
        renderTable(allVendors);
    })
    .catch(err => {
        console.error("Error loading applications:", err);
        allVendors = [];
        renderTable(allVendors);
    });
}

function renderTable(vendors) {
    const tbody = document.getElementById('newApplicationBody');

    if (!vendors || vendors.length === 0) {
        tbody.innerHTML = '<tr><td colspan="2" style="text-align:center; padding:20px;">No applications found.</td></tr>';
        return;
    }

    tbody.innerHTML = vendors.map(v => {
        const profileImg = resolveProfilePath(v.profilePath);
        const licensePath = resolveLicensePath(v.licenseUrl);

        return `
            <tr>
                <td>
                    <div class="vendor-cell">
                        <img src="${escapeHtml(profileImg)}" class="table-avatar" onerror="this.src='image/profile.png'">
                        <div>
                            <strong>${escapeHtml(v.name || 'No Name')}</strong><br>
                            <small>${escapeHtml(v.nophone || 'No Phone')}</small>
                        </div>
                    </div>
                </td>
                <td>
                    <button class="btn-view" onclick="viewDetail('${escapeJs(v.id)}', '${escapeJs(licensePath)}')">VIEW</button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterSearchApplications() {
    const searchInput = document.getElementById('vendorSearch').value.toLowerCase();

    const filtered = allVendors.filter(v =>
        (v.name && v.name.toLowerCase().includes(searchInput)) ||
        (v.nophone && v.nophone.includes(searchInput))
    );

    renderTable(filtered);
}

function sortApplications() {
    const filterValue = document.getElementById('filterSelect').value;
    let sortedVendors = [...allVendors];

    if (filterValue === 'az') {
        sortedVendors.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
    } else if (filterValue === 'za') {
        sortedVendors.sort((a, b) => (b.name || '').localeCompare(a.name || ''));
    } else if (filterValue === 'newest') {
        sortedVendors.sort((a, b) => Number(b.id || 0) - Number(a.id || 0));
    } else if (filterValue === 'oldest') {
        sortedVendors.sort((a, b) => Number(a.id || 0) - Number(b.id || 0));
    }

    const searchInput = document.getElementById('vendorSearch').value.toLowerCase();

    if (searchInput) {
        sortedVendors = sortedVendors.filter(v =>
            (v.name && v.name.toLowerCase().includes(searchInput)) ||
            (v.nophone && v.nophone.includes(searchInput))
        );
    }

    renderTable(sortedVendors);
}

function viewDetail(vendorId, licensePath) {
    sessionStorage.setItem("viewVendorId", vendorId);
    sessionStorage.setItem("viewVendorLicensePath", licensePath || "");
    window.location.href = "detailvendor.html";
}

function resolveProfilePath(path) {
    if (!path || path === "null" || path.trim() === "") {
        return "image/profile.png";
    }

    const cleanPath = path.trim();
    const fileName = cleanPath.substring(cleanPath.lastIndexOf("/") + 1).substring(cleanPath.lastIndexOf("\\") + 1);

    if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
        return cleanPath;
    }

    if (cleanPath.startsWith("image/")) {
        return cleanPath;
    }

    return `${baseURL}/profiles/${fileName}`;
}

function resolveLicensePath(path) {
    if (!path || path === "null" || path.trim() === "") {
        return "";
    }

    const cleanPath = path.trim();

    if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
        return cleanPath;
    }

    const firstFile = cleanPath.split(",")[0].trim();
    const fileName = firstFile.substring(firstFile.lastIndexOf("/") + 1).substring(firstFile.lastIndexOf("\\") + 1);

    return `${baseURL}/licences/${fileName}`;
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