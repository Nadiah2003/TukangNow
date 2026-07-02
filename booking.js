const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let selectedFiles = [];
let redirectAfterModal = "";
let originalDeposit = 0;
let estimatePricingCache = {};
let selectedServiceId = 0;

document.addEventListener("DOMContentLoaded", function() {
    const dateInput = document.getElementById("bookingDate");
    const imageInput = document.getElementById("imageInput");
    const sqftInput = document.getElementById("sqftInput");
    const serviceSelect = document.getElementById("chooseService");
    const pageParams = new URLSearchParams(window.location.search);

    if (pageParams.get("payment") === "failed") {
        setTimeout(function() {
            showModal("Failed!", "Failed, please try again!", "error");
        }, 300);
    }

    if (dateInput) {
        const today = new Date().toISOString().split("T")[0];
        dateInput.setAttribute("min", today);
        dateInput.addEventListener("change", loadBookingPage);
    }

    if (imageInput) {
        imageInput.addEventListener("change", handleImageUpload);
    }

    if (sqftInput) {
        sqftInput.addEventListener("input", updateCostBreakdown);
    }

    if (serviceSelect) {
        serviceSelect.addEventListener("change", function() {
            const selectedOption = serviceSelect.options[serviceSelect.selectedIndex];

            if (selectedOption && selectedOption.dataset.serviceId) {
                selectedServiceId = Number(selectedOption.dataset.serviceId || 0);
            }

            updateCostBreakdown();
        });
    }

    loadBookingPage();
});

async function loadBookingPage() {
    const urlParams = new URLSearchParams(window.location.search);
    const vendorId = urlParams.get("id");
    const dateInput = document.getElementById("bookingDate");
    const serviceSelect = document.getElementById("chooseService");
    const selectedDate = dateInput ? dateInput.value : "";
    const loadingOverlay = document.getElementById("loadingOverlay");

    if (!vendorId) {
        return;
    }

    if (loadingOverlay) {
        loadingOverlay.classList.add("active");
    }

    try {
        const response = await fetch(`${baseURL}/BookingServlet?vendorId=${encodeURIComponent(vendorId)}&date=${encodeURIComponent(selectedDate)}`, {
            method: "GET",
            credentials: "same-origin"
        });

        const data = await response.json();

        if (data.status !== "success") {
            showModal("Failed!", data.message || "Failed to load booking data.", "error");
            return;
        }

        selectedServiceId = Number(data.serviceId || 0);

        document.getElementById("vendorName").innerText = data.vendorName || "N/A";
        document.getElementById("startPrice").innerText = `RM${parseFloat(data.startPrice || 0).toFixed(2)}`;

        const ratingValue = Number(data.averageRating || 0).toFixed(1);
        const ratingCount = Number(data.ratingCount || 0);
        document.getElementById("vendorRating").innerText = `⭐ ${ratingValue} (${ratingCount})`;

        const vImg = document.getElementById("vendorImg");

        if (vImg) {
            vImg.src = resolveProfilePath(data.profilePic);
            vImg.onerror = function() {
                vImg.src = "image/profile.png";
            };
        }

        document.getElementById("custName").innerText = data.custName || "-";
        document.getElementById("custAddress").innerText = data.custAddress || "-";
        document.getElementById("custEmailInput").value = data.custEmail || "";
        document.getElementById("custPhoneInput").value = data.custPhone || "";

        const distanceValue = parseFloat(data.distance || 0);
        document.getElementById("distanceText").innerText = `${distanceValue.toFixed(2)} km`;

        document.getElementById("travelFee").innerText = parseFloat(data.travelFee || 0).toFixed(2);

        originalDeposit = parseFloat(data.totalDeposit || data.startPrice || 0);
        document.getElementById("totalDeposit").innerText = originalDeposit.toFixed(2);

        if (serviceSelect) {
            const currentVal = serviceSelect.value;
            serviceSelect.dataset.serviceId = selectedServiceId;
            serviceSelect.innerHTML = '<option value="" disabled selected>Choose service</option>';

            if (data.subServices && String(data.subServices).trim() !== "") {
                data.subServices.split(",").forEach(function(serviceName) {
                    const clean = serviceName.trim();

                    if (clean) {
                        const opt = document.createElement("option");
                        opt.value = clean;
                        opt.textContent = clean;
                        opt.dataset.serviceId = selectedServiceId;

                        if (clean === currentVal) {
                            opt.selected = true;
                        }

                        serviceSelect.appendChild(opt);
                    }
                });
            } else if (data.serviceName && String(data.serviceName).trim() !== "") {
                const opt = document.createElement("option");
                opt.value = data.serviceName.trim();
                opt.textContent = data.serviceName.trim();
                opt.dataset.serviceId = selectedServiceId;
                serviceSelect.appendChild(opt);
            }
        }

        updateTimeSlots(data);

        if (serviceSelect && serviceSelect.value) {
            updateCostBreakdown();
        }

    } catch (error) {
        console.error(error);
        showModal("Failed!", "Server connection error.", "error");
    } finally {
        if (loadingOverlay) {
            loadingOverlay.classList.remove("active");
        }
    }
}

async function updateCostBreakdown() {
    const rawSelected = document.getElementById("chooseService").value;
    const breakdownList = document.getElementById("breakdownList");
    const estimateBox = document.getElementById("estimateBreakdown");
    const sqftSection = document.getElementById("sqftSection");
    const grandTotalSpan = document.getElementById("grandTotalEst");
    const travelFee = parseFloat(document.getElementById("travelFee").innerText) || 0;

    if (!rawSelected) {
        if (estimateBox) {
            estimateBox.style.display = "none";
        }
        return;
    }

    if (breakdownList) {
        breakdownList.innerHTML = "";
    }

    if (sqftSection) {
        sqftSection.style.display = "none";
    }

    if (estimateBox) {
        estimateBox.style.display = "block";
    }

    if (grandTotalSpan) {
        grandTotalSpan.innerText = "Loading...";
    }

    const loadingItem = document.createElement("li");
    loadingItem.textContent = "Loading estimated item breakdown...";
    breakdownList.appendChild(loadingItem);

    try {
        const estimateData = await getTransparentEstimate(rawSelected);

        breakdownList.innerHTML = "";

        if (estimateData.status === "success" && estimateData.estimate) {
            renderTransparentEstimate(estimateData.estimate, travelFee, breakdownList, estimateBox, grandTotalSpan);
        } else {
            renderEstimateUnavailable(estimateData.message || "Estimate unavailable.", breakdownList, estimateBox, grandTotalSpan);
        }
    } catch (error) {
        console.error(error);
        renderEstimateUnavailable("Estimate unavailable. Please try again later.", breakdownList, estimateBox, grandTotalSpan);
    }
}

async function getTransparentEstimate(serviceName) {
    const vendorId = new URLSearchParams(window.location.search).get("id") || "";
    const cacheKey = `${vendorId}_${serviceName.toLowerCase().trim()}`;

    if (estimatePricingCache[cacheKey]) {
        return estimatePricingCache[cacheKey];
    }

    const response = await fetch(`${baseURL}/BookingServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: `action=estimate&service=${encodeURIComponent(serviceName)}&vendorId=${encodeURIComponent(vendorId)}`
    });

    const data = await response.json();
    estimatePricingCache[cacheKey] = data;

    return data;
}

function renderTransparentEstimate(estimate, travelFee, breakdownList, estimateBox, grandTotalSpan) {
    estimateBox.style.display = "block";

    let itemSubtotal = 0.00;

    const title = document.createElement("li");
    title.style.fontWeight = "700";
    title.textContent = "Estimated Possible Items / Materials";
    breakdownList.appendChild(title);

    if (Array.isArray(estimate.items) && estimate.items.length > 0) {
        estimate.items.forEach(function(item) {
            const itemName = item.itemName || "Estimated item";
            const itemPrice = Number(item.itemPrice || 0);

            itemSubtotal += itemPrice;

            const li = document.createElement("li");
            li.textContent = `${itemName}: RM${itemPrice.toFixed(2)}`;
            breakdownList.appendChild(li);
        });
    } else {
        const li = document.createElement("li");
        li.textContent = "No estimate item found.";
        breakdownList.appendChild(li);
    }

    itemSubtotal = Math.round(itemSubtotal * 100) / 100;
    const totalEstimate = itemSubtotal + travelFee;

    const subtotalLi = document.createElement("li");
    subtotalLi.style.fontWeight = "600";
    subtotalLi.textContent = `Estimated item subtotal: RM${itemSubtotal.toFixed(2)}`;
    breakdownList.appendChild(subtotalLi);

    const travelLi = document.createElement("li");
    travelLi.style.fontWeight = "600";
    travelLi.textContent = `Travel fee: RM${travelFee.toFixed(2)}`;
    breakdownList.appendChild(travelLi);

    if (estimate.note) {
        const note = document.createElement("li");
        note.textContent = estimate.note;
        breakdownList.appendChild(note);
    }

    grandTotalSpan.innerText = totalEstimate.toFixed(2);
}

function renderEstimateUnavailable(message, breakdownList, estimateBox, grandTotalSpan) {
    estimateBox.style.display = "block";

    const li = document.createElement("li");
    li.textContent = message;
    breakdownList.appendChild(li);

    grandTotalSpan.innerText = "-";
}

function updateTimeSlots(data) {
    const timeSelect = document.getElementById("bookingTime");
    const dateInput = document.getElementById("bookingDate");

    timeSelect.innerHTML = '<option value="" disabled selected>Time</option>';

    if (!data.availTime || !data.availTime.includes("-")) {
        return;
    }

    const parts = data.availTime.split("-").map(function(t) {
        return t.trim();
    });

    const start = convertTo24h(parts[0]);
    const end = convertTo24h(parts[1]);
    const booked = data.bookedTimes ? data.bookedTimes.map(function(t) {
        return parseInt(t.split(":")[0]);
    }) : [];

    const now = new Date();
    const todayStr = now.toISOString().split("T")[0];
    const isToday = dateInput && dateInput.value === todayStr;
    const currentHour = now.getHours();

    for (let hour = start; hour <= end; hour++) {
        if (!booked.includes(hour) && !(isToday && hour <= currentHour)) {
            const opt = document.createElement("option");
            opt.value = `${String(hour).padStart(2, "0")}:00:00`;

            let displayHour = hour > 12 ? hour - 12 : (hour === 0 ? 12 : hour);
            opt.textContent = `${displayHour}${hour >= 12 ? " PM" : " AM"}`;

            timeSelect.appendChild(opt);
        }
    }
}

function convertTo24h(t) {
    let parts = t.match(/(\d+)\s*(AM|PM)/i);

    if (!parts) {
        return parseInt(t) || 0;
    }

    let h = parseInt(parts[1]);
    let ampm = parts[2].toUpperCase();

    if (ampm === "PM" && h !== 12) {
        h += 12;
    }

    if (ampm === "AM" && h === 12) {
        h = 0;
    }

    return h;
}

function handleImageUpload(e) {
    const files = Array.from(e.target.files);

    if (selectedFiles.length + files.length > 3) {
        showModal("Failed!", "Max 3 images.", "error");
        return;
    }

    files.forEach(function(file) {
        selectedFiles.push(file);

        const reader = new FileReader();

        reader.onload = function(ev) {
            const wrap = document.createElement("div");
            wrap.className = "preview-wrapper";
            wrap.innerHTML = `<img src="${ev.target.result}"><button type="button" class="remove-btn">×</button>`;

            wrap.querySelector(".remove-btn").onclick = function() {
                selectedFiles = selectedFiles.filter(function(f) {
                    return f !== file;
                });

                wrap.remove();
                updateImageUI();
            };

            document.getElementById("previewContainer").appendChild(wrap);
            updateImageUI();
        };

        reader.readAsDataURL(file);
    });
}

function updateImageUI() {
    document.getElementById("imageCount").innerText = `${selectedFiles.length} / 3 selected`;
    document.getElementById("uploadBox").style.display = selectedFiles.length >= 3 ? "none" : "flex";
}

async function proceedToPayment() {
    const rawService = document.getElementById("chooseService").value;
    const date = document.getElementById("bookingDate").value;
    const time = document.getElementById("bookingTime").value;
    const email = document.getElementById("custEmailInput").value;
    const phone = document.getElementById("custPhoneInput").value;
    const details = document.querySelector('textarea[name="details"]').value;

    if (!rawService || !date || !time || !email || !phone) {
        showModal("Failed!", "Please complete all fields!", "error");
        return;
    }

    const loadingOverlay = document.getElementById("loadingOverlay");

    if (loadingOverlay) {
        loadingOverlay.classList.add("active");
    }

    try {
        const vendorId = new URLSearchParams(window.location.search).get("id");
        const amount = document.getElementById("totalDeposit").innerText;
        const travelFee = document.getElementById("travelFee").innerText;
        const distanceTextValue = document.getElementById("distanceText").innerText.replace(" km", "");
        const distanceKm = (parseFloat(distanceTextValue) || 0).toFixed(2);
        const resolvedServiceId = await resolveSelectedServiceId(vendorId, rawService);

        if (!resolvedServiceId || resolvedServiceId <= 0) {
            showModal("Failed!", "Service not found for selected vendor.", "error");
            return;
        }

        selectedServiceId = resolvedServiceId;

        const pendingBooking = {
            vendorId: vendorId,
            serviceId: selectedServiceId,
            service: rawService.trim(),
            date: date,
            time: time,
            userName: document.getElementById("custName").innerText,
            userEmail: email,
            userPhone: phone,
            details: details,
            amount: amount,
            travelFee: travelFee,
            distanceKm: distanceKm,
            bookingApi: `${baseURL}/BookingServlet`
        };

        const pendingImages = await Promise.all(selectedFiles.map(async function(f) {
            return {
                name: f.name,
                type: f.type,
                dataUrl: await fileToDataUrl(f)
            };
        }));

        sessionStorage.setItem("pendingBooking", JSON.stringify(pendingBooking));
        sessionStorage.setItem("pendingBookingImages", JSON.stringify(pendingImages));

        window.location.href = `payment.html?amount=${encodeURIComponent(amount)}`;
    } catch (e) {
        console.error(e);
        showModal("Failed!", e.message || "Failed to prepare payment.", "error");
    } finally {
        if (loadingOverlay) {
            loadingOverlay.classList.remove("active");
        }
    }
}

async function resolveSelectedServiceId(vendorId, serviceName) {
    const serviceSelect = document.getElementById("chooseService");
    let currentServiceId = selectedServiceId;

    if (serviceSelect) {
        const selectedOption = serviceSelect.options[serviceSelect.selectedIndex];

        if (selectedOption && selectedOption.dataset.serviceId) {
            currentServiceId = Number(selectedOption.dataset.serviceId || selectedServiceId || 0);
        }
    }

    const body = new URLSearchParams();
    body.append("action", "resolveService");
    body.append("vendorId", vendorId || "");
    body.append("serviceId", currentServiceId || 0);
    body.append("service", serviceName || "");

    const response = await fetch(`${baseURL}/BookingServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: body.toString()
    });

    const result = await response.json();

    if (result.status === "success") {
        return Number(result.serviceId || 0);
    }

    throw new Error(result.message || "Unable to resolve selected service.");
}

function fileToDataUrl(file) {
    return new Promise(function(resolve, reject) {
        const reader = new FileReader();

        reader.onload = function(e) {
            resolve(e.target.result);
        };

        reader.onerror = reject;
        reader.readAsDataURL(file);
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

function getOnlyFileName(path) {
    return String(path)
        .replaceAll("\\", "/")
        .split("/")
        .pop();
}

function showModal(title, message, type = "error", redirectUrl = "") {
    document.getElementById("modalTitle").innerText = title;
    document.getElementById("modalMessage").innerText = message;
    document.getElementById("modalIconCircle").className = `modal-icon-circle ${type}`;
    document.getElementById("modalIcon").innerText = type === "success" ? "✓" : "×";
    document.getElementById("statusModal").classList.add("show-flex");
    redirectAfterModal = redirectUrl;
}

function closeModal() {
    document.getElementById("statusModal").classList.remove("show-flex");

    if (redirectAfterModal) {
        window.location.href = redirectAfterModal;
    }
}