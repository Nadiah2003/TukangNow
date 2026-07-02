const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let currentSubServices = [];
let shouldReloadAfterPopup = false;

document.addEventListener("DOMContentLoaded", function() {
    loadProfileData();

    bindClick("btnLogout", function() {
        if (confirm("Are you sure you want to log out?")) {
            runLogout();
        }
    });

    bindClick("btnOpenEditProfile", openEditProfileModal);
    bindClick("btnSaveProfile", saveProfile);
    bindClick("btnCancelEditProfile", function() {
        closeModal("editModal");
    });

    bindClick("btnOpenAddService", openAddServiceModal);
    bindClick("btnAddServiceInput", function() {
        addNewSubServiceInput("");
    });
    bindClick("btnSubmitSubServices", submitNewSubServices);
    bindClick("btnCancelAddService", cancelAddServiceModal);

    bindClick("btnEditService", toggleEditService);
    bindClick("btnSaveService", saveServiceChanges);

    bindClick("btnEditAvail", toggleEditAvail);
    bindClick("btnSaveAvail", saveAvailChanges);

    bindClick("btnOpenPasswordModal", openPasswordModal);
    bindClick("btnUpdatePass", updatePassword);
    bindClick("btnCancelPassword", function() {
        closeModal("passwordModal");
    });
    bindClick("toggleOldPass", function() {
        togglePasswordVisibility("oldPass", "oldPassEyeOpen", "oldPassEyeClosed");
    });
    bindClick("toggleNewPass", function() {
        togglePasswordVisibility("newPass", "newPassEyeOpen", "newPassEyeClosed");
    });
    bindClick("statusPopupBtn", closeStatusPopup);

    bindClick("btnChangePic", function() {
        document.getElementById("profileInputHidden").click();
    });

    const profileInput = document.getElementById("profileInputHidden");
    if (profileInput) {
        profileInput.addEventListener("change", uploadProfileImage);
    }

    const newPassEl = document.getElementById("newPass");
    if (newPassEl) {
        newPassEl.addEventListener("input", function() {
            validatePasswordUI(newPassEl.value);
        });
    }
});

function runLogout() {
    fetch(`${baseURL}/LogoutServlet`, {
        method: "POST",
        credentials: "same-origin"
    })
    .then(function(res) {
        return res.json();
    })
    .then(function(data) {
        if (data.status === "success") {
            sessionStorage.clear();
            window.location.href = data.redirectUrl || "login.html";
        } else {
            showStatusPopup("failed", "Failed!", data.message || "Failed to log out.");
        }
    })
    .catch(function() {
        sessionStorage.clear();
        window.location.href = "login.html";
    });
}

function bindClick(id, handler) {
    const el = document.getElementById(id);
    if (el) {
        el.onclick = handler;
    }
}

function loadProfileData() {
    fetch(`${baseURL}/ProfileVendorServlet`, {
        method: "GET",
        credentials: "same-origin"
    })
    .then(function(res) {
        if (res.status === 401) {
            window.location.href = "login.html";
            return null;
        }
        return res.json();
    })
    .then(function(data) {
        if (!data) {
            return;
        }

        if (data.status !== "success") {
            return;
        }

        const v = data.vendor || {};
        const s = data.service || null;

        document.getElementById("vName").innerText = v.name || "N/A";
        document.getElementById("vEmail").innerText = v.email || "N/A";
        document.getElementById("vPhone").innerText = v.nophone || "-";

        const addrElem = document.getElementById("vAddress");
        addrElem.innerText = v.fullDisplayAddress || "No address set";
        addrElem.dataset.street = v.address || "";
        addrElem.dataset.postcode = v.postcode || "";
        addrElem.dataset.city = v.city || "";
        addrElem.dataset.state = v.state || "";

        if (s) {
            document.getElementById("vMainCategory").textContent = s.servicename || "No Category";
            renderSubServices(s.subServicesList || []);
            document.getElementById("priceDisplay").textContent = "RM " + formatMoney(s.startprice || "0");
            document.getElementById("availDaysDisplay").textContent = s.avail_date || "";
            document.getElementById("availTimeDisplay").textContent = s.avail_time || "";
        } else {
            document.getElementById("vMainCategory").textContent = "No Category";
            renderSubServices([]);
            document.getElementById("priceDisplay").textContent = "RM 0.00";
            document.getElementById("availDaysDisplay").textContent = "";
            document.getElementById("availTimeDisplay").textContent = "";
        }

        document.getElementById("ratingText").innerHTML = `⭐ <strong>${data.avgRating || "0.0"}</strong> (${data.totalReviews || 0} Reviews)`;
        document.getElementById("jobsText").innerHTML = `🏆 <strong>${data.jobsCompleted || 0}</strong> Jobs Completed`;

        const profileImg = document.getElementById("vProfileImg");
        if (v.profile_path && profileImg) {
            profileImg.src = `${baseURL}/profiles/${getOnlyFileName(v.profile_path)}?t=${Date.now()}`;
        } else {
            profileImg.src = "image/profile.png";
        }

        profileImg.onerror = function() {
            profileImg.src = "image/profile.png";
        };
    });
}

function parseSubServices(listData) {
    if (Array.isArray(listData)) {
        return listData.map(function(item) {
            return String(item).trim();
        }).filter(Boolean);
    }

    if (typeof listData === "string") {
        return listData.split(",").map(function(item) {
            return item.trim();
        }).filter(Boolean);
    }

    return [];
}

function renderSubServices(listData) {
    const listContainer = document.getElementById("subServiceList");
    currentSubServices = parseSubServices(listData);

    listContainer.innerHTML = "";
    listContainer.dataset.original = JSON.stringify(currentSubServices);

    if (currentSubServices.length > 0) {
        currentSubServices.forEach(function(item) {
            const li = document.createElement("li");
            const span = document.createElement("span");
            span.textContent = `• ${item}`;
            li.appendChild(span);
            listContainer.appendChild(li);
        });
    } else {
        const li = document.createElement("li");
        li.className = "no-subservice";
        li.textContent = "No subservices listed";
        listContainer.appendChild(li);
    }
}

function getStoredSubServices() {
    const listContainer = document.getElementById("subServiceList");

    try {
        return JSON.parse(listContainer.dataset.original || "[]");
    } catch (e) {
        return [];
    }
}

function openAddServiceModal() {
    const modal = document.getElementById("addServiceModal");
    const rows = document.getElementById("serviceInputRows");
    const existingServices = getStoredSubServices();

    rows.innerHTML = "";
    rows.dataset.original = JSON.stringify(existingServices);
    modal.style.display = "flex";

    if (existingServices.length > 0) {
        existingServices.forEach(function(service) {
            addNewSubServiceInput(service);
        });
    } else {
        addNewSubServiceInput("");
    }

    const firstInput = rows.querySelector(".new-sub-service-input");
    if (firstInput) {
        firstInput.focus();
    }
}

function addNewSubServiceInput(value = "") {
    const rows = document.getElementById("serviceInputRows");
    const isFirstInput = rows.children.length === 0;

    const row = document.createElement("div");
    row.className = "add-service-input-row";

    row.innerHTML = `
        <div class="input-with-icon">
            <span class="service-icon">🛠️</span>
            <input type="text" class="new-sub-service-input" placeholder="Service Name" autocomplete="off">
            ${isFirstInput ? "" : '<button type="button" class="btn-delete-service-input">×</button>'}
        </div>
    `;

    rows.appendChild(row);

    const input = row.querySelector(".new-sub-service-input");
    const deleteBtn = row.querySelector(".btn-delete-service-input");

    if (input) {
        input.value = value;

        input.addEventListener("input", function() {
            if (input.value.length > 0) {
                input.value = input.value.charAt(0).toUpperCase() + input.value.slice(1);
            }
        });

        if (value === "") {
            input.focus();
        }
    }

    if (deleteBtn) {
        deleteBtn.addEventListener("click", function() {
            row.remove();
        });
    }
}

function cancelAddServiceModal() {
    document.getElementById("serviceInputRows").innerHTML = "";
    document.getElementById("addServiceModal").style.display = "none";
}

function submitNewSubServices() {
    const rows = document.getElementById("serviceInputRows");
    const inputs = document.querySelectorAll(".new-sub-service-input");

    const updatedServices = Array.from(inputs).map(function(input) {
        return input.value.trim();
    }).filter(Boolean);

    if (updatedServices.length === 0) {
        alert("Please enter at least one service name.");
        return;
    }

    let originalServices = [];

    try {
        originalServices = JSON.parse(rows.dataset.original || "[]");
    } catch (e) {
        originalServices = [];
    }

    const noChanges =
        originalServices.length === updatedServices.length &&
        originalServices.every(function(service, index) {
            return service === updatedServices[index];
        });

    if (noChanges) {
        cancelAddServiceModal();
        return;
    }

    const priceVal = getCurrentPriceValue();

    updateServiceInDatabase(priceVal, updatedServices)
    .then(function(data) {
        if (data.status === "success") {
            location.reload();
        } else {
            alert(data.message || "Failed to save service.");
        }
    });
}

function toggleEditService() {
    const priceInput = document.getElementById("priceInput");
    const priceDisplay = document.getElementById("priceDisplay");
    const btnEdit = document.getElementById("btnEditService");
    const btnSave = document.getElementById("btnSaveService");

    if (priceInput.classList.contains("hidden")) {
        priceDisplay.classList.add("hidden");
        priceInput.classList.remove("hidden");
        priceInput.value = priceDisplay.textContent.replace("RM", "").trim();
        btnSave.classList.remove("hidden");
        btnEdit.innerText = "CANCEL";
    } else {
        priceDisplay.classList.remove("hidden");
        priceInput.classList.add("hidden");
        btnSave.classList.add("hidden");
        btnEdit.innerText = "EDIT";
    }
}

function saveServiceChanges() {
    const priceVal = document.getElementById("priceInput").value.trim();
    const subServices = getStoredSubServices();

    updateServiceInDatabase(priceVal, subServices)
    .then(function(data) {
        if (data.status === "success") {
            location.reload();
        } else {
            alert(data.message || "Failed to save changes.");
        }
    });
}

function getCurrentPriceValue() {
    const priceInput = document.getElementById("priceInput");
    const priceDisplay = document.getElementById("priceDisplay");

    if (priceInput && !priceInput.classList.contains("hidden")) {
        return priceInput.value.trim();
    }

    return priceDisplay.textContent.replace("RM", "").trim();
}

function updateServiceInDatabase(priceVal, subServices) {
    const params = new URLSearchParams();

    params.append("action", "updateService");
    params.append("price", priceVal);
    params.append("subservices", subServices.join(", "));

    return fetch(`${baseURL}/ProfileVendorServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: params.toString()
    }).then(function(res) {
        return res.json();
    });
}

function validatePasswordUI(pw) {
    const checks = {
        "char-length": pw.length >= 8,
        "char-upper": /[A-Z]/.test(pw),
        "char-number": /[0-9]/.test(pw),
        "char-special": /[!@#$%^&*]/.test(pw)
    };

    let allOk = true;

    for (const id in checks) {
        const ok = checks[id];
        const el = document.getElementById(id);

        if (!el) {
            continue;
        }

        el.style.color = ok ? "#28a745" : "#ff4d4d";
        el.querySelector(".icon").innerText = ok ? "✔" : "✖";

        if (!ok) {
            allOk = false;
        }
    }

    document.getElementById("btnUpdatePass").disabled = !allOk;
}

function openEditProfileModal() {
    const addrElem = document.getElementById("vAddress");

    document.getElementById("editName").value = document.getElementById("vName").innerText;
    document.getElementById("editEmail").value = document.getElementById("vEmail").innerText;
    document.getElementById("editPhone").value = document.getElementById("vPhone").innerText;
    document.getElementById("editAddress").value = addrElem.dataset.street || "";
    document.getElementById("editPostcode").value = addrElem.dataset.postcode || "";
    document.getElementById("editCity").value = addrElem.dataset.city || "";
    document.getElementById("editState").value = addrElem.dataset.state || "";

    document.getElementById("editModal").style.display = "flex";
}

function saveProfile() {
    const params = new URLSearchParams();

    params.append("action", "updateProfileInfo");
    params.append("name", document.getElementById("editName").value);
    params.append("email", document.getElementById("editEmail").value);
    params.append("phone", document.getElementById("editPhone").value);
    params.append("address", document.getElementById("editAddress").value);
    params.append("postcode", document.getElementById("editPostcode").value);
    params.append("city", document.getElementById("editCity").value);
    params.append("state", document.getElementById("editState").value);

    fetch(`${baseURL}/ProfileVendorServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: params.toString()
    })
    .then(function(res) {
        return res.json();
    })
    .then(function(data) {
        if (data.status === "success") {
            location.reload();
        } else {
            alert(data.message || "Failed to save profile.");
        }
    });
}

function parseAvailabilityRange(value) {
    const cleanValue = (value || "").trim();

    if (!cleanValue) {
        return {
            from: "",
            until: ""
        };
    }

    if (cleanValue.includes(" - ")) {
        const parts = cleanValue.split(" - ");

        return {
            from: parts[0].trim(),
            until: parts[1].trim()
        };
    }

    return {
        from: "",
        until: ""
    };
}

function toggleEditAvail() {
    const dayInputs = document.getElementById("availDayInputs");
    const timeInputs = document.getElementById("availTimeInputs");
    const daysDisplay = document.getElementById("availDaysDisplay");
    const timeDisplay = document.getElementById("availTimeDisplay");
    const btnSave = document.getElementById("btnSaveAvail");
    const btnEdit = document.getElementById("btnEditAvail");

    if (dayInputs.classList.contains("hidden")) {
        const dayRange = parseAvailabilityRange(daysDisplay.textContent);
        const timeRange = parseAvailabilityRange(timeDisplay.textContent);

        document.getElementById("availDayFromInput").value = dayRange.from;
        document.getElementById("availDayUntilInput").value = dayRange.until;
        document.getElementById("availTimeFromInput").value = timeRange.from;
        document.getElementById("availTimeUntilInput").value = timeRange.until;

        daysDisplay.classList.add("hidden");
        timeDisplay.classList.add("hidden");

        dayInputs.classList.remove("hidden");
        timeInputs.classList.remove("hidden");

        btnSave.classList.remove("hidden");
        btnEdit.innerText = "CANCEL";
    } else {
        location.reload();
    }
}

function saveAvailChanges() {
    const dayFrom = document.getElementById("availDayFromInput").value;
    const dayUntil = document.getElementById("availDayUntilInput").value;
    const timeFrom = document.getElementById("availTimeFromInput").value;
    const timeUntil = document.getElementById("availTimeUntilInput").value;

    if (!dayFrom || !dayUntil || !timeFrom || !timeUntil) {
        alert("Please choose day and time.");
        return;
    }

    const params = new URLSearchParams();

    params.append("action", "updateAvailability");
    params.append("days", `${dayFrom} - ${dayUntil}`);
    params.append("time", `${timeFrom} - ${timeUntil}`);

    fetch(`${baseURL}/ProfileVendorServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: params.toString()
    })
    .then(function(res) {
        return res.json();
    })
    .then(function(data) {
        if (data.status === "success") {
            location.reload();
        } else {
            alert(data.message || "Failed to save availability.");
        }
    });
}

function updatePassword() {
    const oldPass = document.getElementById("oldPass").value;
    const newPass = document.getElementById("newPass").value;

    if (!oldPass || !newPass) {
        showStatusPopup("failed", "Failed!", "Please enter old password and new password.");
        return;
    }

    const params = new URLSearchParams();

    params.append("action", "updatePassword");
    params.append("oldPass", oldPass);
    params.append("newPass", newPass);

    fetch(`${baseURL}/ProfileVendorServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: params.toString()
    })
    .then(function(res) {
        return res.json();
    })
    .then(function(data) {
        if (data.status === "success") {
            showStatusPopup("success", "Success!", "Password updated successfully!", true);
        } else {
            showStatusPopup("failed", "Failed!", data.message || "Failed to update password.");
        }
    });
}

function uploadProfileImage() {
    const input = document.getElementById("profileInputHidden");

    if (!input.files || input.files.length === 0) {
        return;
    }

    const formData = new FormData();
    formData.append("action", "uploadImage");
    formData.append("profilePic", input.files[0]);

    fetch(`${baseURL}/ProfileVendorServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData
    })
    .then(function(res) {
        return res.json();
    })
    .then(function(data) {
        if (data.status === "success") {
            location.reload();
        } else {
            showStatusPopup("failed", "Failed!", data.message || "Failed to upload image.");
        }
    })
    .catch(function() {
        showStatusPopup("failed", "Error", "A system connection error occurred.");
    });
}

function openPasswordModal() {
    const oldPass = document.getElementById("oldPass");
    const newPass = document.getElementById("newPass");

    oldPass.value = "";
    newPass.value = "";
    oldPass.type = "password";
    newPass.type = "password";
    oldPass.removeAttribute("readonly");

    document.getElementById("oldPassEyeOpen").classList.remove("hidden");
    document.getElementById("oldPassEyeClosed").classList.add("hidden");
    document.getElementById("newPassEyeOpen").classList.remove("hidden");
    document.getElementById("newPassEyeClosed").classList.add("hidden");

    validatePasswordUI("");
    document.getElementById("passwordModal").style.display = "flex";
}

function togglePasswordVisibility(inputId, eyeOpenId, eyeClosedId) {
    const input = document.getElementById(inputId);
    const eyeOpen = document.getElementById(eyeOpenId);
    const eyeClosed = document.getElementById(eyeClosedId);

    if (input.type === "password") {
        input.type = "text";
        eyeOpen.classList.add("hidden");
        eyeClosed.classList.remove("hidden");
    } else {
        input.type = "password";
        eyeOpen.classList.remove("hidden");
        eyeClosed.classList.add("hidden");
    }
}

function showStatusPopup(type, title, message, reloadAfterClose = false) {
    const popup = document.getElementById("statusPopup");
    const icon = document.getElementById("statusPopupIcon");
    const titleEl = document.getElementById("statusPopupTitle");
    const messageEl = document.getElementById("statusPopupMessage");

    shouldReloadAfterPopup = reloadAfterClose;

    icon.classList.remove("success");

    if (type === "success") {
        icon.classList.add("success");
        icon.innerText = "✓";
    } else {
        icon.innerText = "×";
    }

    titleEl.innerText = title;
    messageEl.innerText = message;
    popup.classList.remove("hidden");
}

function closeStatusPopup() {
    document.getElementById("statusPopup").classList.add("hidden");

    if (shouldReloadAfterPopup) {
        location.reload();
    }
}

function closeModal(id) {
    document.getElementById(id).style.display = "none";
}

function getOnlyFileName(path) {
    return String(path)
        .replaceAll("\\", "/")
        .split("/")
        .pop();
}

function formatMoney(value) {
    const amount = parseFloat(value);

    if (isNaN(amount)) {
        return "0.00";
    }

    return amount.toFixed(2);
}