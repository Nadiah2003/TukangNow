const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

let loggedInCustomerId = 0;
let emergencyServiceIds = {};
let selectedEmergencyFiles = [];

document.addEventListener("DOMContentLoaded", function() {
    const btnBack = document.getElementById("btnBack");
    const btnBroadcast = document.getElementById("btnBroadcast");
    const emergencyImages = document.getElementById("emergencyImages");

    sessionStorage.removeItem("pendingBookingImages");

    if (btnBack) {
        btnBack.addEventListener("click", function() {
            history.back();
        });
    }

    if (btnBroadcast) {
        btnBroadcast.addEventListener("click", function() {
            triggerConfirmModal();
        });
    }

    if (emergencyImages) {
        emergencyImages.addEventListener("change", function() {
            handleEmergencyImageSelection(this.files);
        });
    }

    loadCustomerData();
});

async function loadCustomerData() {
    try {
        const response = await fetch(`${baseURL}/EmergencyServlet`, {
            method: "GET",
            credentials: "same-origin"
        });

        const data = await response.json();

        if (data.status === "session_expired") {
            window.location.href = data.redirect || "index.html";
            return;
        }

        if (data.status === "success") {
            document.getElementById("custName").innerText = data.custName || "-";
            document.getElementById("custEmail").innerText = data.custEmail || "-";
            document.getElementById("custAddress").innerText = data.custAddress || "-";

            loggedInCustomerId = parseInt(data.id) || 0;
            emergencyServiceIds = data.serviceIds || {};

            sessionStorage.setItem("custEmail", data.custEmail || "customer@email.com");
            sessionStorage.setItem("custPhone", data.custPhone || "0123456789");
            return;
        }

        showModal("Failed!", data.message || "Unable to load customer data.", "error");

    } catch (error) {
        showModal("Failed!", "Unable to load customer data. Please try again.", "error");
    }
}

function handleEmergencyImageSelection(fileList) {
    const files = Array.from(fileList || []);

    if (files.length === 0) {
        selectedEmergencyFiles = [];
        renderImagePreview();
        return;
    }

    if (files.length > 3) {
        document.getElementById("emergencyImages").value = "";
        selectedEmergencyFiles = [];
        renderImagePreview();
        showModal("Failed!", "Maximum 3 images only.", "error");
        return;
    }

    const invalidFile = files.find(function(file) {
        return !file.type || !file.type.startsWith("image/");
    });

    if (invalidFile) {
        document.getElementById("emergencyImages").value = "";
        selectedEmergencyFiles = [];
        renderImagePreview();
        showModal("Failed!", "Only image files are allowed.", "error");
        return;
    }

    selectedEmergencyFiles = files;
    renderImagePreview();
}

function renderImagePreview() {
    const previewList = document.getElementById("imagePreviewList");

    if (!previewList) {
        return;
    }

    previewList.innerHTML = "";

    selectedEmergencyFiles.forEach(function(file, index) {
        const card = document.createElement("div");
        card.className = "image-preview-card";

        const img = document.createElement("img");
        img.src = URL.createObjectURL(file);
        img.alt = file.name || "Emergency evidence";

        const removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.className = "remove-image-btn";
        removeBtn.innerText = "×";
        removeBtn.addEventListener("click", function() {
            removeEmergencyImage(index);
        });

        card.appendChild(img);
        card.appendChild(removeBtn);
        previewList.appendChild(card);
    });
}

function removeEmergencyImage(index) {
    selectedEmergencyFiles.splice(index, 1);

    if (selectedEmergencyFiles.length === 0) {
        document.getElementById("emergencyImages").value = "";
    }

    renderImagePreview();
}

function triggerConfirmModal() {
    const category = document.getElementById("serviceCategory").value;
    const problem = document.getElementById("problemDesc").value.trim();

    if (!category || !problem) {
        showModal("Failed!", "Please describe your problem before broadcasting.", "error");
        return;
    }

    const serviceIds = getEmergencyServiceIdsByCategory(category);

    if (serviceIds === "") {
        showModal("Failed!", `No active vendor is available within 30km for ${category} emergency service right now.`, "error");
        return;
    }

    showModal("Proceed to Payment?", "You will be redirected to the secure payment page to pay the RM 50.00 SOS deposit.", "confirm");
}

async function proceedToPayment() {
    const category = document.getElementById("serviceCategory").value;
    const problem = document.getElementById("problemDesc").value.trim();
    const custName = document.getElementById("custName").innerText;
    const custEmail = sessionStorage.getItem("custEmail") || "customer@email.com";
    const custPhone = sessionStorage.getItem("custPhone") || "0123456789";
    const serviceIds = getEmergencyServiceIdsByCategory(category);
    const primaryServiceId = getFirstServiceId(serviceIds);

    if (serviceIds === "" || primaryServiceId <= 0) {
        showModal("Failed!", `No active vendor is available within 30km for ${category} emergency service right now.`, "error");
        return;
    }

    if (selectedEmergencyFiles.length > 3) {
        showModal("Failed!", "Maximum 3 images only.", "error");
        return;
    }

    try {
        setBroadcastButtonLoading(true);

        const evidencePath = await uploadEvidenceImages();

        const pendingBooking = {
            booking_id: 0,
            customer_id: loggedInCustomerId,
            isEmergency: true,
            service: "Emergency",
            category: category,
            service_id: primaryServiceId,
            emergency_service_ids: serviceIds,
            subservicebooked: "Emergency",
            problem: problem,
            amount: "50.00",
            final_amount: "50.00",
            deposit: "50.00",
            vendorId: "SOS_BROADCAST",
            userName: custName,
            userEmail: custEmail,
            userPhone: custPhone,
            travelFee: "0",
            distanceKm: "0",
            evidencePath: evidencePath
        };

        sessionStorage.setItem("pendingBooking", JSON.stringify(pendingBooking));
        window.location.href = "payment.html";

    } catch (error) {
        setBroadcastButtonLoading(false);
        showModal("Failed!", error.message || "Unable to upload evidence image. Please try again.", "error");
    }
}

async function uploadEvidenceImages() {
    if (selectedEmergencyFiles.length === 0) {
        return "";
    }

    const formData = new FormData();
    formData.append("action", "uploadEvidence");

    selectedEmergencyFiles.forEach(function(file) {
        formData.append("evidenceImages", file);
    });

    const response = await fetch(`${baseURL}/EmergencyServlet`, {
        method: "POST",
        body: formData,
        credentials: "same-origin"
    });

    const data = await response.json();

    if (data.status === "session_expired") {
        window.location.href = data.redirect || "index.html";
        return "";
    }

    if (data.status !== "success") {
        throw new Error(data.message || "Upload failed.");
    }

    return data.evidencePath || "";
}

function setBroadcastButtonLoading(isLoading) {
    const btnBroadcast = document.getElementById("btnBroadcast");

    if (!btnBroadcast) {
        return;
    }

    if (isLoading) {
        btnBroadcast.disabled = true;
        btnBroadcast.innerText = "PROCESSING...";
    } else {
        btnBroadcast.disabled = false;
        btnBroadcast.innerText = "BROADCAST NOW";
    }
}

function getEmergencyServiceIdsByCategory(category) {
    if (!emergencyServiceIds) {
        return "";
    }

    const value = emergencyServiceIds[category];

    if (value === undefined || value === null) {
        return "";
    }

    return String(value).trim();
}

function getFirstServiceId(serviceIds) {
    if (!serviceIds || String(serviceIds).trim() === "") {
        return 0;
    }

    const firstValue = String(serviceIds).split(",")[0];
    const parsed = parseInt(firstValue);

    if (isNaN(parsed)) {
        return 0;
    }

    return parsed;
}

function showModal(title, message, type) {
    const modal = document.getElementById("statusModal");
    const icon = document.getElementById("modalIcon");
    const actions = document.getElementById("modalActions");

    document.getElementById("modalTitle").innerText = title;
    document.getElementById("modalMessage").innerText = message;

    icon.className = "modal-icon";
    icon.classList.add(type);
    icon.innerText = type === "success" ? "✓" : type === "confirm" ? "?" : "✕";

    actions.innerHTML = "";

    if (type === "confirm") {
        const btnContinue = document.createElement("button");
        btnContinue.className = "btn-modal-continue";
        btnContinue.type = "button";
        btnContinue.innerText = "Go to Payment";
        btnContinue.addEventListener("click", function() {
            proceedToPayment();
        });

        const btnCancel = document.createElement("button");
        btnCancel.className = "btn-modal-cancel";
        btnCancel.type = "button";
        btnCancel.innerText = "Cancel";
        btnCancel.addEventListener("click", function() {
            closeModal();
        });

        actions.appendChild(btnContinue);
        actions.appendChild(btnCancel);
    } else {
        const btnClose = document.createElement("button");
        btnClose.className = "btn-modal-continue";
        btnClose.type = "button";
        btnClose.innerText = "Continue";
        btnClose.addEventListener("click", function() {
            closeModal();
        });

        actions.appendChild(btnClose);
    }

    modal.classList.remove("hidden");
}

function closeModal() {
    document.getElementById("statusModal").classList.add("hidden");
}