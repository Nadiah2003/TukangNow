const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

let currentAdmin = null;
let allAdmins = [];
let allEstimateItems = [];
let selectedAdminChatBookingId = null;
let adminChatPollingTimer = null;
let adminChatBadgeTimer = null;

document.addEventListener("DOMContentLoaded", function() {
    loadAdminProfile();
    bindProfileButtons();
    bindPasswordValidation();
    bindAdminFilters();
    bindAdminChatButtons();
    loadAdminChatBadge();

    adminChatBadgeTimer = setInterval(function() {
        loadAdminChatBadge();
    }, 5000);
});

function getEl(id) {
    return document.getElementById(id);
}

function bindProfileButtons() {
    const btnOpenEditModal = getEl("btnOpenEditModal");
    const btnCancelEdit = getEl("btnCancelEdit");
    const btnSaveEdit = getEl("btnSaveEdit");
    const btnOpenImageModal = getEl("btnOpenImageModal");
    const btnCancelImage = getEl("btnCancelImage");
    const btnUploadImage = getEl("btnUploadImage");
    const btnOpenPasswordModal = getEl("btnOpenPasswordModal");
    const btnCancelPassword = getEl("btnCancelPassword");
    const btnUpdatePass = getEl("btnUpdatePass");
    const statusOkBtn = getEl("statusOkBtn");
    const btnLogout = getEl("btnLogout");
    const btnRegisterAdmin = getEl("btnRegisterAdmin");
    const btnOpenEventModal = getEl("btnOpenEventModal");
    const btnCancelEvent = getEl("btnCancelEvent");
    const btnAddEvent = getEl("btnAddEvent");
    const btnOpenEstimateModal = getEl("btnOpenEstimateModal");
    const btnCancelEstimate = getEl("btnCancelEstimate");
    const btnAddEstimateItem = getEl("btnAddEstimateItem");

    if (btnOpenEditModal) {
        btnOpenEditModal.addEventListener("click", function() {
            if (getEl("editName")) {
                getEl("editName").value = currentAdmin ? currentAdmin.name : "";
            }

            if (getEl("editEmail")) {
                getEl("editEmail").value = currentAdmin ? currentAdmin.email : "";
            }

            openModal("editModal");
        });
    }

    if (btnCancelEdit) {
        btnCancelEdit.addEventListener("click", function() {
            closeModal("editModal");
        });
    }

    if (btnSaveEdit) {
        btnSaveEdit.addEventListener("click", updateProfile);
    }

    if (btnOpenImageModal) {
        btnOpenImageModal.addEventListener("click", function() {
            openModal("imageModal");
        });
    }

    if (btnCancelImage) {
        btnCancelImage.addEventListener("click", function() {
            closeModal("imageModal");
        });
    }

    if (btnUploadImage) {
        btnUploadImage.addEventListener("click", uploadProfileImage);
    }

    if (btnOpenPasswordModal) {
        btnOpenPasswordModal.addEventListener("click", function() {
            if (getEl("oldPass")) {
                getEl("oldPass").value = "";
            }

            if (getEl("newPass")) {
                getEl("newPass").value = "";
            }

            validatePasswordRules();
            openModal("passwordModal");
        });
    }

    if (btnCancelPassword) {
        btnCancelPassword.addEventListener("click", function() {
            closeModal("passwordModal");
        });
    }

    if (btnUpdatePass) {
        btnUpdatePass.addEventListener("click", changePassword);
    }

    if (statusOkBtn) {
        statusOkBtn.addEventListener("click", function() {
            if (getEl("statusModal")) {
                getEl("statusModal").style.display = "none";
            }
        });
    }

    if (btnLogout) {
        btnLogout.addEventListener("click", function() {
            window.location.href = `${baseURL}/LogoutServlet`;
        });
    }

    if (btnRegisterAdmin) {
        btnRegisterAdmin.addEventListener("click", function() {
            window.location.href = "registeradmin.html";
        });
    }

    if (btnOpenEventModal) {
        btnOpenEventModal.addEventListener("click", function() {
            clearEventForm();
            openModal("eventModal");
        });
    }

    if (btnCancelEvent) {
        btnCancelEvent.addEventListener("click", function() {
            closeModal("eventModal");
        });
    }

    if (btnAddEvent) {
        btnAddEvent.addEventListener("click", addEvent);
    }

    if (btnOpenEstimateModal) {
        btnOpenEstimateModal.addEventListener("click", function() {
            clearEstimateForm();
            openModal("estimateModal");
            loadEstimateItems();
        });
    }

    if (btnCancelEstimate) {
        btnCancelEstimate.addEventListener("click", function() {
            closeModal("estimateModal");
        });
    }

    if (btnAddEstimateItem) {
        btnAddEstimateItem.addEventListener("click", addEstimateItem);
    }
}

function bindPasswordValidation() {
    const newPass = getEl("newPass");

    if (newPass) {
        newPass.addEventListener("input", validatePasswordRules);
    }
}

function bindAdminFilters() {
    const searchInput = getEl("adminSearchInput");
    const roleFilter = getEl("adminRoleFilter");
    const sortSelect = getEl("adminSortSelect");

    if (searchInput) {
        searchInput.addEventListener("input", applyAdminFilters);
    }

    if (roleFilter) {
        roleFilter.addEventListener("change", applyAdminFilters);
    }

    if (sortSelect) {
        sortSelect.addEventListener("change", applyAdminFilters);
    }
}

function bindAdminChatButtons() {
    const btnOpenAdminChat = getEl("btnOpenAdminChat");
    const btnCloseAdminChat = getEl("btnCloseAdminChat");
    const btnRefreshChatList = getEl("btnRefreshChatList");
    const btnSendAdminReply = getEl("btnSendAdminReply");
    const adminReplyInput = getEl("adminReplyInput");

    if (btnOpenAdminChat) {
        btnOpenAdminChat.addEventListener("click", function() {
            openAdminChatPanel();
        });
    }

    if (btnCloseAdminChat) {
        btnCloseAdminChat.addEventListener("click", function() {
            closeAdminChatPanel();
        });
    }

    if (btnRefreshChatList) {
        btnRefreshChatList.addEventListener("click", function() {
            loadAdminConversations();
            loadAdminChatBadge();
        });
    }

    if (btnSendAdminReply) {
        btnSendAdminReply.addEventListener("click", sendAdminReply);
    }

    if (adminReplyInput) {
        adminReplyInput.addEventListener("keypress", function(event) {
            if (event.key === "Enter") {
                sendAdminReply();
            }
        });
    }
}

function loadAdminProfile() {
    fetch(`${baseURL}/ProfileAdminServlet?action=get&_=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status !== "success") {
            showStatus("Failed!", data.message || "Failed to load profile.", "error");
            return;
        }

        currentAdmin = data;
        renderProfile(data);
        renderRoleSections(data.admin_level);
    })
    .catch(() => {
        showStatus("Failed!", "Connection error while loading admin profile.", "error");
    });
}

function renderProfile(data) {
    if (getEl("adminName")) {
        getEl("adminName").innerText = data.name || "-";
    }

    if (getEl("adminEmail")) {
        getEl("adminEmail").innerText = data.email || "-";
    }

    if (getEl("adminLevelDisplay")) {
        getEl("adminLevelDisplay").innerText = roleName(Number(data.admin_level));
    }

    const displayImg = getEl("displayImg");

    if (displayImg) {
        displayImg.onerror = function() {
            this.onerror = null;
            this.src = "image/profile.png";
        };
        displayImg.src = resolveProfilePath(data.profile_path, true);
    }
}

function renderRoleSections(adminLevel) {
    const level = Number(adminLevel);
    const adminSection = getEl("adminManagementSection");
    const eventSection = getEl("eventManagementSection");
    const estimateSection = getEl("estimateManagementSection");

    if (adminSection) {
        adminSection.classList.add("hidden");
    }

    if (eventSection) {
        eventSection.classList.add("hidden");
    }

    if (estimateSection) {
        estimateSection.classList.add("hidden");
    }

    if (level === 1 && adminSection) {
        adminSection.classList.remove("hidden");
        loadAdminList();
    }

    if (level === 2 && eventSection) {
        eventSection.classList.remove("hidden");
    }

    if (level === 3 && estimateSection) {
        estimateSection.classList.remove("hidden");
    }
}

function loadAdminList() {
    fetch(`${baseURL}/ProfileAdminServlet?action=admins&_=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status !== "success") {
            showStatus("Failed!", data.message || "Failed to load admins.", "error");
            return;
        }

        allAdmins = data.admins || [];
        applyAdminFilters();
    })
    .catch(() => {
        showStatus("Failed!", "Connection error.", "error");
    });
}

function applyAdminFilters() {
    const searchValue = getEl("adminSearchInput") ? getEl("adminSearchInput").value.toLowerCase().trim() : "";
    const roleFilter = getEl("adminRoleFilter") ? getEl("adminRoleFilter").value : "all";
    const sortValue = getEl("adminSortSelect") ? getEl("adminSortSelect").value : "role";

    let filteredAdmins = allAdmins.filter(admin => {
        const roleText = roleName(Number(admin.admin_level)).toLowerCase();
        const matchSearch = !searchValue ||
            String(admin.name || "").toLowerCase().includes(searchValue) ||
            String(admin.email || "").toLowerCase().includes(searchValue) ||
            roleText.includes(searchValue);

        const matchRole = roleFilter === "all" || String(admin.admin_level) === roleFilter;

        return matchSearch && matchRole;
    });

    filteredAdmins.sort((a, b) => {
        if (sortValue === "name") {
            return String(a.name || "").localeCompare(String(b.name || ""));
        }

        if (sortValue === "email") {
            return String(a.email || "").localeCompare(String(b.email || ""));
        }

        const roleA = roleSortOrder(Number(a.admin_level));
        const roleB = roleSortOrder(Number(b.admin_level));

        if (roleA !== roleB) {
            return roleA - roleB;
        }

        return String(a.name || "").localeCompare(String(b.name || ""));
    });

    renderAdminList(filteredAdmins);
}

function roleSortOrder(level) {
    if (level === 1) {
        return 1;
    }

    if (level === 2) {
        return 2;
    }

    if (level === 3) {
        return 3;
    }

    return 4;
}

function renderAdminList(admins) {
    const container = getEl("adminListContainer");

    if (!container) {
        return;
    }

    container.innerHTML = "";

    if (admins.length === 0) {
        container.innerHTML = "<p>No admin found.</p>";
        return;
    }

    admins.forEach(admin => {
        const div = document.createElement("div");
        div.className = "admin-item";
        div.innerHTML = `
            <div>
                <div class="admin-name">${escapeHtml(admin.name || "-")}</div>
                <div class="admin-email">${escapeHtml(admin.email || "-")}</div>
                <span class="admin-role-label">${roleName(Number(admin.admin_level))}</span>
            </div>
            <select class="role-select" data-admin-id="${Number(admin.id)}">
                <option value="1" ${Number(admin.admin_level) === 1 ? "selected" : ""}>Leader Admin</option>
                <option value="2" ${Number(admin.admin_level) === 2 ? "selected" : ""}>Event Admin</option>
                <option value="3" ${Number(admin.admin_level) === 3 ? "selected" : ""}>Estimate Admin</option>
                <option value="0" ${Number(admin.admin_level) === 0 ? "selected" : ""}>Admin</option>
            </select>
            <button class="btn-small" type="button" data-admin-id="${Number(admin.id)}">Update</button>
        `;
        container.appendChild(div);
    });

    document.querySelectorAll(".btn-small[data-admin-id]").forEach(button => {
        button.addEventListener("click", function() {
            const adminId = this.getAttribute("data-admin-id");
            const select = document.querySelector(`.role-select[data-admin-id="${adminId}"]`);

            if (select) {
                updateAdminRole(adminId, select.value);
            }
        });
    });
}

function updateAdminRole(adminId, roleValue) {
    const formData = new FormData();
    formData.append("action", "updateRole");
    formData.append("admin_id", adminId);
    formData.append("admin_level", roleValue);

    fetch(`${baseURL}/ProfileAdminServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData,
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "success") {
            showStatus("Success!", data.message, "success");
            loadAdminList();
        } else {
            showStatus("Failed!", data.message || "Failed to update role.", "error");
        }
    })
    .catch(() => {
        showStatus("Failed!", "Connection error.", "error");
    });
}

function updateProfile() {
    const name = getEl("editName") ? getEl("editName").value.trim() : "";
    const email = getEl("editEmail") ? getEl("editEmail").value.trim() : "";

    if (!name || !email) {
        showStatus("Failed!", "Name and Email cannot be empty.", "error");
        return;
    }

    const formData = new FormData();
    formData.append("action", "updateProfile");
    formData.append("name", name);
    formData.append("email", email);

    fetch(`${baseURL}/ProfileAdminServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData,
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "success") {
            closeModal("editModal");
            showStatus("Success!", data.message, "success");
            loadAdminProfile();
        } else {
            showStatus("Failed!", data.message || "Failed to update profile.", "error");
        }
    })
    .catch(() => {
        showStatus("Failed!", "Connection error.", "error");
    });
}

function uploadProfileImage() {
    const fileInput = getEl("profilePicInput");

    if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
        showStatus("Failed!", "Please choose an image.", "error");
        return;
    }

    const formData = new FormData();
    formData.append("action", "uploadImage");
    formData.append("profilePic", fileInput.files[0]);

    fetch(`${baseURL}/ProfileAdminServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData,
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "success") {
            closeModal("imageModal");
            fileInput.value = "";

            if (data.path) {
                const displayImg = getEl("displayImg");

                if (displayImg) {
                    displayImg.onerror = function() {
                        this.onerror = null;
                        this.src = "image/profile.png";
                    };
                    displayImg.src = resolveProfilePath(data.path, true);
                }
            }

            showStatus("Success!", data.message, "success");
            loadAdminProfile();
        } else {
            showStatus("Failed!", data.message || "Failed to upload image.", "error");
        }
    })
    .catch(() => {
        showStatus("Failed!", "Connection error.", "error");
    });
}

function changePassword() {
    const oldPass = getEl("oldPass") ? getEl("oldPass").value : "";
    const newPass = getEl("newPass") ? getEl("newPass").value : "";

    if (!oldPass || !newPass) {
        showStatus("Failed!", "Password cannot be empty.", "error");
        return;
    }

    if (!isPasswordValid(newPass)) {
        showStatus("Failed!", "New password does not meet the requirements.", "error");
        return;
    }

    const formData = new FormData();
    formData.append("action", "changePassword");
    formData.append("oldPass", oldPass);
    formData.append("newPass", newPass);

    fetch(`${baseURL}/ProfileAdminServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData,
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "success") {
            closeModal("passwordModal");
            showStatus("Success!", data.message, "success");
        } else {
            showStatus("Failed!", data.message || "Failed to update password.", "error");
        }
    })
    .catch(() => {
        showStatus("Failed!", "Connection error.", "error");
    });
}

function addEvent() {
    const title = getEl("eventTitle") ? getEl("eventTitle").value.trim() : "";
    const description = getEl("eventDescription") ? getEl("eventDescription").value.trim() : "";
    const image = getEl("eventImage");
    const discountCode = getEl("eventDiscountCode") ? getEl("eventDiscountCode").value.trim().toUpperCase() : "";
    const discountPercentage = getEl("eventDiscountPercentage") ? getEl("eventDiscountPercentage").value : "";
    const startDate = getEl("eventStartDate") ? getEl("eventStartDate").value : "";
    const endDate = getEl("eventEndDate") ? getEl("eventEndDate").value : "";

    if (!title || !description || !discountCode || !discountPercentage || !startDate || !endDate) {
        showStatus("Failed!", "Please complete all event fields.", "error");
        return;
    }

    const formData = new FormData();
    formData.append("action", "addEvent");
    formData.append("title", title);
    formData.append("description", description);
    formData.append("discount_code", discountCode);
    formData.append("discount_percentage", discountPercentage);
    formData.append("start_date", startDate);
    formData.append("end_date", endDate);

    if (image && image.files && image.files.length > 0) {
        formData.append("eventImage", image.files[0]);
    }

    fetch(`${baseURL}/ProfileAdminServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData,
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "success") {
            clearEventForm();
            closeModal("eventModal");
            showStatus("Success!", data.message, "success");
        } else {
            showStatus("Failed!", data.message || "Failed to add event.", "error");
        }
    })
    .catch(() => {
        showStatus("Failed!", "Connection error.", "error");
    });
}

function loadEstimateItems() {
    const tbody = getEl("estimateTableBody");

    if (!tbody) {
        return;
    }

    tbody.innerHTML = `<tr><td colspan="4">Loading...</td></tr>`;

    fetch(`${baseURL}/ProfileAdminServlet?action=estimateItems&_=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status !== "success") {
            tbody.innerHTML = `<tr><td colspan="4">No data found.</td></tr>`;
            showStatus("Failed!", data.message || "Failed to load estimate items.", "error");
            return;
        }

        allEstimateItems = data.items || [];
        renderEstimateTable(allEstimateItems);
    })
    .catch(() => {
        tbody.innerHTML = `<tr><td colspan="4">Connection error.</td></tr>`;
        showStatus("Failed!", "Connection error.", "error");
    });
}

function renderEstimateTable(items) {
    const tbody = getEl("estimateTableBody");

    if (!tbody) {
        return;
    }

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4">No estimate item found.</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(item => {
        const id = Number(item.id);
        return `
            <tr data-estimate-id="${id}">
                <td>
                    <input type="text" class="estimate-service-input" value="${escapeHtml(item.serviceKeyword || "")}">
                </td>
                <td>
                    <input type="text" class="estimate-name-input" value="${escapeHtml(item.itemName || "")}">
                </td>
                <td>
                    <input type="number" class="estimate-price-input" value="${Number(item.itemPrice || 0).toFixed(2)}" min="0" step="0.01">
                </td>
                <td>
                    <div class="estimate-action-row">
                        <button class="btn-estimate-update" type="button" data-estimate-id="${id}">Update</button>
                        <button class="btn-estimate-delete" type="button" data-estimate-id="${id}">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");

    document.querySelectorAll(".btn-estimate-update").forEach(button => {
        button.addEventListener("click", function() {
            updateEstimateItem(this.getAttribute("data-estimate-id"));
        });
    });

    document.querySelectorAll(".btn-estimate-delete").forEach(button => {
        button.addEventListener("click", function() {
            deleteEstimateItem(this.getAttribute("data-estimate-id"));
        });
    });
}

function addEstimateItem() {
    const serviceKeyword = getEl("estimateServiceKeyword") ? getEl("estimateServiceKeyword").value.trim() : "";
    const itemName = getEl("estimateItemName") ? getEl("estimateItemName").value.trim() : "";
    const itemPrice = getEl("estimateItemPrice") ? getEl("estimateItemPrice").value : "";

    if (!serviceKeyword || !itemName || itemPrice === "") {
        showStatus("Failed!", "Please complete all estimate fields.", "error");
        return;
    }

    const formData = new FormData();
    formData.append("action", "addEstimate");
    formData.append("service_keyword", serviceKeyword);
    formData.append("item_name", itemName);
    formData.append("item_price", itemPrice);

    fetch(`${baseURL}/ProfileAdminServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData,
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "success") {
            clearEstimateForm();
            loadEstimateItems();
            showStatus("Success!", data.message, "success");
        } else {
            showStatus("Failed!", data.message || "Failed to add estimate item.", "error");
        }
    })
    .catch(() => {
        showStatus("Failed!", "Connection error.", "error");
    });
}

function updateEstimateItem(id) {
    const row = document.querySelector(`tr[data-estimate-id="${id}"]`);

    if (!row) {
        showStatus("Failed!", "Estimate item not found.", "error");
        return;
    }

    const serviceKeyword = row.querySelector(".estimate-service-input").value.trim();
    const itemName = row.querySelector(".estimate-name-input").value.trim();
    const itemPrice = row.querySelector(".estimate-price-input").value;

    if (!serviceKeyword || !itemName || itemPrice === "") {
        showStatus("Failed!", "Please complete all estimate fields.", "error");
        return;
    }

    const formData = new FormData();
    formData.append("action", "updateEstimate");
    formData.append("id", id);
    formData.append("service_keyword", serviceKeyword);
    formData.append("item_name", itemName);
    formData.append("item_price", itemPrice);

    fetch(`${baseURL}/ProfileAdminServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData,
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "success") {
            loadEstimateItems();
            showStatus("Success!", data.message, "success");
        } else {
            showStatus("Failed!", data.message || "Failed to update estimate item.", "error");
        }
    })
    .catch(() => {
        showStatus("Failed!", "Connection error.", "error");
    });
}

function deleteEstimateItem(id) {
    const confirmDelete = confirm("Are you sure you want to delete this estimate item?");

    if (!confirmDelete) {
        return;
    }

    const formData = new FormData();
    formData.append("action", "deleteEstimate");
    formData.append("id", id);

    fetch(`${baseURL}/ProfileAdminServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData,
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === "success") {
            loadEstimateItems();
            showStatus("Success!", data.message, "success");
        } else {
            showStatus("Failed!", data.message || "Failed to delete estimate item.", "error");
        }
    })
    .catch(() => {
        showStatus("Failed!", "Connection error.", "error");
    });
}

function clearEstimateForm() {
    if (getEl("estimateServiceKeyword")) {
        getEl("estimateServiceKeyword").value = "";
    }

    if (getEl("estimateItemName")) {
        getEl("estimateItemName").value = "";
    }

    if (getEl("estimateItemPrice")) {
        getEl("estimateItemPrice").value = "";
    }
}

function clearEventForm() {
    if (getEl("eventTitle")) {
        getEl("eventTitle").value = "";
    }

    if (getEl("eventDescription")) {
        getEl("eventDescription").value = "";
    }

    if (getEl("eventImage")) {
        getEl("eventImage").value = "";
    }

    if (getEl("eventDiscountCode")) {
        getEl("eventDiscountCode").value = "";
    }

    if (getEl("eventDiscountPercentage")) {
        getEl("eventDiscountPercentage").value = "";
    }

    if (getEl("eventStartDate")) {
        getEl("eventStartDate").value = "";
    }

    if (getEl("eventEndDate")) {
        getEl("eventEndDate").value = "";
    }
}

function validatePasswordRules() {
    const newPass = getEl("newPass");
    const password = newPass ? newPass.value : "";
    const isLengthValid = password.length >= 8;
    const isUpperValid = /[A-Z]/.test(password);
    const isNumberValid = /[0-9]/.test(password);
    const isSpecialValid = /[@#$%^&*]/.test(password);

    updateRequirement("char-length", isLengthValid, "Minimum 8 characters");
    updateRequirement("char-upper", isUpperValid, "At least one uppercase letter (A-Z)");
    updateRequirement("char-number", isNumberValid, "At least one number (0-9)");
    updateRequirement("char-special", isSpecialValid, "At least one special character (@#$%^&*)");

    if (getEl("btnUpdatePass")) {
        getEl("btnUpdatePass").disabled = !isPasswordValid(password);
    }
}

function updateRequirement(id, isValid, text) {
    const element = getEl(id);

    if (!element) {
        return;
    }

    element.classList.remove("valid", "invalid");
    element.classList.add(isValid ? "valid" : "invalid");
    element.innerText = `${isValid ? "✓" : "✖"} ${text}`;
}

function isPasswordValid(password) {
    return password.length >= 8 && /[A-Z]/.test(password) && /[0-9]/.test(password) && /[@#$%^&*]/.test(password);
}

function roleName(level) {
    const role = Number(level);

    if (role === 1) {
        return "Leader Admin";
    }

    if (role === 2) {
        return "Event Admin";
    }

    if (role === 3) {
        return "Estimate Admin";
    }

    return "Admin";
}

function resolveProfilePath(path, forceRefresh = false) {
    if (!path || path === "null" || String(path).trim() === "") {
        return "image/profile.png";
    }

    const cleanPath = String(path).trim();

    if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
        return forceRefresh ? addCacheBuster(cleanPath) : cleanPath;
    }

    if (cleanPath.startsWith("image/")) {
        return cleanPath;
    }

    const normalizedPath = cleanPath.replaceAll("\\", "/");
    const parts = normalizedPath.split("/");
    const fileName = parts[parts.length - 1];

    if (!fileName) {
        return "image/profile.png";
    }

    const imageUrl = `${baseURL}/profiles/${encodeURIComponent(fileName)}`;

    return forceRefresh ? addCacheBuster(imageUrl) : imageUrl;
}

function addCacheBuster(url) {
    if (url.includes("?")) {
        return `${url}&v=${Date.now()}`;
    }

    return `${url}?v=${Date.now()}`;
}

function openModal(id) {
    const modal = getEl(id);

    if (modal) {
        modal.style.display = "flex";
    }
}

function closeModal(id) {
    const modal = getEl(id);

    if (modal) {
        modal.style.display = "none";
    }
}

function openAdminChatPanel() {
    selectedAdminChatBookingId = null;

    if (getEl("adminChatModal")) {
        getEl("adminChatModal").style.display = "flex";
    }

    if (getEl("activeChatTitle")) {
        getEl("activeChatTitle").innerText = "Select a chat";
    }

    if (getEl("activeChatSubtitle")) {
        getEl("activeChatSubtitle").innerText = "Messages will appear here.";
    }

    if (getEl("adminMessageList")) {
        getEl("adminMessageList").innerHTML = `<div class="chat-empty">No chat selected.</div>`;
    }

    if (getEl("adminReplyInput")) {
        getEl("adminReplyInput").value = "";
        getEl("adminReplyInput").disabled = true;
    }

    if (getEl("btnSendAdminReply")) {
        getEl("btnSendAdminReply").disabled = true;
    }

    loadAdminConversations();
    loadAdminChatBadge();
}

function closeAdminChatPanel() {
    selectedAdminChatBookingId = null;
    clearInterval(adminChatPollingTimer);

    if (getEl("adminChatModal")) {
        getEl("adminChatModal").style.display = "none";
    }
}

function loadAdminChatBadge() {
    const dot = getEl("adminChatRedDot");

    if (!dot) {
        return;
    }

    fetch(`${baseURL}/AdminProfileChatServlet?action=badge&_=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data && data.status === "success" && Number(data.unreadCount || 0) > 0) {
            dot.style.display = "block";
        } else {
            dot.style.display = "none";
        }
    })
    .catch(() => {
        dot.style.display = "none";
    });
}

function loadAdminConversations() {
    const container = getEl("adminConversationList");

    if (!container) {
        return;
    }

    container.innerHTML = `<div class="chat-empty">Loading chats...</div>`;

    fetch(`${baseURL}/AdminProfileChatServlet?action=conversations&_=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data && data.status === "error") {
            container.innerHTML = `<div class="chat-empty">${escapeHtml(data.message || "Unable to load chats.")}</div>`;
            return;
        }

        renderAdminConversations(data.conversations || []);
    })
    .catch(() => {
        container.innerHTML = `<div class="chat-empty">Connection error while loading chats.</div>`;
    });
}

function renderAdminConversations(conversations) {
    const container = getEl("adminConversationList");

    if (!container) {
        return;
    }

    container.innerHTML = "";

    if (!conversations || conversations.length === 0) {
        container.innerHTML = `<div class="chat-empty">No customer chat yet.</div>`;
        return;
    }

    conversations.forEach(function(item) {
        const bookingId = Number(item.bookingId || 0);
        const unreadCount = Number(item.unreadCount || 0);
        const activeClass = selectedAdminChatBookingId === bookingId ? "active" : "";
        const dotHTML = unreadCount > 0 ? `<span class="admin-conversation-dot"></span>` : "";

        const div = document.createElement("div");
        div.className = `admin-conversation-item ${activeClass}`;
        div.innerHTML = `
            ${dotHTML}
            <div class="admin-conversation-top">
                <span class="admin-conversation-name">${escapeHtml(item.customerName || "Customer")}</span>
                <span class="admin-conversation-time">${escapeHtml(item.lastTime || "-")}</span>
            </div>
            <p class="admin-conversation-info">Order #${bookingId} • ${escapeHtml(item.serviceName || "Service")} • ${escapeHtml(item.status || "-")}</p>
            <p class="admin-conversation-preview">${escapeHtml(item.lastSender || "-")}: ${escapeHtml(item.lastMessage || "-")}</p>
        `;

        div.addEventListener("click", function() {
            openAdminConversation(bookingId, item.customerName || "Customer", item.serviceName || "Service", item.status || "-");
        });

        container.appendChild(div);
    });
}

function openAdminConversation(bookingId, customerName, serviceName, status) {
    selectedAdminChatBookingId = Number(bookingId);

    if (getEl("activeChatTitle")) {
        getEl("activeChatTitle").innerText = `Order #${selectedAdminChatBookingId} - ${customerName}`;
    }

    if (getEl("activeChatSubtitle")) {
        getEl("activeChatSubtitle").innerText = `${serviceName} • ${status}`;
    }

    if (getEl("adminReplyInput")) {
        getEl("adminReplyInput").disabled = false;
        getEl("adminReplyInput").focus();
    }

    if (getEl("btnSendAdminReply")) {
        getEl("btnSendAdminReply").disabled = false;
    }

    loadAdminMessages();

    clearInterval(adminChatPollingTimer);
    adminChatPollingTimer = setInterval(function() {
        if (selectedAdminChatBookingId) {
            loadAdminMessages();
        }
    }, 3000);
}

function loadAdminMessages() {
    if (!selectedAdminChatBookingId) {
        return;
    }

    fetch(`${baseURL}/AdminProfileChatServlet?action=messages&booking_id=${encodeURIComponent(selectedAdminChatBookingId)}&_=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store"
    })
    .then(response => response.json())
    .then(data => {
        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data && data.status === "error") {
            showStatus("Failed!", data.message || "Unable to load messages.", "error");
            return;
        }

        renderAdminMessages(data.messages || []);
        loadAdminConversations();
        loadAdminChatBadge();
    })
    .catch(() => {
        showStatus("Failed!", "Connection error while loading messages.", "error");
    });
}

function renderAdminMessages(messages) {
    const container = getEl("adminMessageList");

    if (!container) {
        return;
    }

    container.innerHTML = "";

    if (!messages || messages.length === 0) {
        container.innerHTML = `<div class="chat-empty">No messages yet.</div>`;
        return;
    }

    messages.forEach(function(item) {
        const sender = String(item.sender || "").toLowerCase().trim();
        const bubbleClass = sender === "admin" ? "admin" : "customer";

        const div = document.createElement("div");
        div.className = `admin-message-bubble ${bubbleClass}`;
        div.innerHTML = `
            ${escapeHtml(item.message || "")}
            <span class="admin-message-time">${escapeHtml(item.createdAt || "")}</span>
        `;

        container.appendChild(div);
    });

    container.scrollTop = container.scrollHeight;
}

function sendAdminReply() {
    if (!selectedAdminChatBookingId) {
        showStatus("Failed!", "Please select a chat first.", "error");
        return;
    }

    const input = getEl("adminReplyInput");

    if (!input) {
        return;
    }

    const message = input.value.trim();

    if (message === "") {
        return;
    }

    input.disabled = true;

    if (getEl("btnSendAdminReply")) {
        getEl("btnSendAdminReply").disabled = true;
    }

    fetch(`${baseURL}/AdminProfileChatServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        body: JSON.stringify({
            action: "sendMessage",
            booking_id: selectedAdminChatBookingId,
            message: message
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data && data.status === "session_expired") {
            window.location.href = data.redirect || "login.html";
            return;
        }

        if (data && data.status === "error") {
            showStatus("Failed!", data.message || "Unable to send message.", "error");
            return;
        }

        input.value = "";
        loadAdminMessages();
        loadAdminConversations();
        loadAdminChatBadge();
    })
    .catch(() => {
        showStatus("Failed!", "Connection error while sending message.", "error");
    })
    .finally(() => {
        input.disabled = false;

        if (getEl("btnSendAdminReply")) {
            getEl("btnSendAdminReply").disabled = false;
        }

        input.focus();
    });
}

function showStatus(title, message, type) {
    const modal = getEl("statusModal");
    const card = getEl("statusCard");
    const icon = getEl("statusIcon");
    const statusTitle = getEl("statusTitle");
    const statusMessage = getEl("statusMessage");

    if (!modal || !card || !icon || !statusTitle || !statusMessage) {
        alert(message);
        return;
    }

    card.classList.remove("success", "error");
    card.classList.add(type === "success" ? "success" : "error");

    icon.innerText = type === "success" ? "✓" : "×";
    statusTitle.innerText = title;
    statusMessage.innerText = message;

    modal.style.display = "flex";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}