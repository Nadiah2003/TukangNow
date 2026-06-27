const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

let originalAmount = 0;
let finalAmount = 0;
let userPoints = 0;
let userWalletBalance = 0;
let availableVouchers = [];
let detectedPaymentType = "first_payment";
let appliedVoucherId = 0;
let globalBookingData = {};
let isProcessingPayment = false;

document.addEventListener("DOMContentLoaded", function() {
    const urlParams = new URLSearchParams(window.location.search);

    globalBookingData = getBestBookingData(urlParams);
    originalAmount = getPaymentAmount(globalBookingData, urlParams);
    finalAmount = originalAmount;

    const targetBookingId = parseInt(urlParams.get("booking_id")) || parseInt(globalBookingData.booking_id) || parseInt(globalBookingData.bookingId) || parseInt(globalBookingData.id) || 0;

    const amountElement = document.getElementById("displayAmount");
    const serviceElement = document.getElementById("displayService");

    if (amountElement) {
        amountElement.innerText = `RM ${originalAmount.toFixed(2)}`;
    }

    if (serviceElement) {
        serviceElement.innerText = globalBookingData.service === "Emergency"
            ? `Emergency (${globalBookingData.category || "General"})`
            : (globalBookingData.service || globalBookingData.servicename || globalBookingData.serviceName || globalBookingData.subservicebooked || globalBookingData.subserviceBooked || "General Service");
    }

    setRewardPointCheckboxState(0, true, "Use Reward Points (Checking...)");
    setEWalletPaymentOptionState(true);
    loadPaymentSystemData(targetBookingId);

    const usePointsCheckbox = document.getElementById("usePointsCheckbox");
    if (usePointsCheckbox) {
        usePointsCheckbox.addEventListener("change", recalculatePrices);
    }

    const applyCodeBtn = document.getElementById("applyCodeBtn");
    if (applyCodeBtn) {
        applyCodeBtn.addEventListener("click", function() {
            const inputCode = document.getElementById("voucherCodeInput").value.trim().toUpperCase();

            if (!inputCode) {
                showLocalModal("Warning", "Please enter a voucher code.", "error");
                return;
            }

            const matchedVoucher = availableVouchers.find(function(v) {
                return v.voucher_code && v.voucher_code.toUpperCase() === inputCode;
            });

            if (matchedVoucher) {
                appliedVoucherId = matchedVoucher.customer_voucher_id;
                recalculatePrices();
                showLocalModal("Success!", `Voucher "${matchedVoucher.event_name}" applied successfully!`, "success");
            } else {
                showLocalModal("Failed", "Invalid voucher code or voucher already used.", "error");
                appliedVoucherId = 0;
                recalculatePrices();
            }
        });
    }

    const payNowBtn = document.getElementById("payNowBtn");
    if (payNowBtn) {
        payNowBtn.addEventListener("click", function() {
            executePaymentProcess(globalBookingData);
        });
    }

    const modalCancelBtn = document.getElementById("modalCancelBtn");
    if (modalCancelBtn) {
        modalCancelBtn.addEventListener("click", function() {
            document.getElementById("passwordModal").style.display = "none";
            clearPasswordInput();
        });
    }

    const modalConfirmBtn = document.getElementById("modalConfirmBtn");
    if (modalConfirmBtn) {
        modalConfirmBtn.addEventListener("click", function() {
            submitPasswordVerifiedPayment();
        });
    }
});

function getBestBookingData(urlParams) {
    const urlAmount = getUrlAmount(urlParams);

    const priorityKeys = [
        "pendingBooking",
        "pendingBookingData",
        "bookingData",
        "selectedBooking",
        "bookingDetails",
        "pending_booking",
        "paymentBooking",
        "currentBooking",
        "paymentData",
        "selectedService",
        "bookingInfo",
        "checkoutData"
    ];

    let fallbackData = {};

    for (let i = 0; i < priorityKeys.length; i++) {
        const found = getStorageObjectByKey(priorityKeys[i], urlParams);

        if (found && getPaymentAmount(found, urlParams) > 0) {
            return found;
        }

        if (found && Object.keys(fallbackData).length === 0) {
            fallbackData = found;
        }
    }

    const allFound = findAnyStorageObjectWithAmount(urlParams);

    if (allFound && getPaymentAmount(allFound, urlParams) > 0) {
        return allFound;
    }

    if (Object.keys(fallbackData).length > 0) {
        const fallbackAmount = getPaymentAmount(fallbackData, urlParams);

        if (fallbackAmount > 0) {
            return fallbackData;
        }
    }

    const directStorageAmount = findDirectStorageAmount();

    if (directStorageAmount > 0) {
        return {
            amount: directStorageAmount,
            booking_id: urlParams.get("booking_id") || "0",
            service: urlParams.get("service") || "Service",
            category: urlParams.get("category") || ""
        };
    }

    return {
        amount: urlAmount,
        booking_id: urlParams.get("booking_id") || "0",
        service: urlParams.get("service") || "Service",
        category: urlParams.get("category") || ""
    };
}

function getStorageObjectByKey(key, urlParams) {
    const storages = [sessionStorage, localStorage];

    for (let i = 0; i < storages.length; i++) {
        const raw = storages[i].getItem(key);

        if (!raw) {
            continue;
        }

        const parsed = parseStorageValue(raw);

        if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
            return parsed;
        }

        if (Array.isArray(parsed)) {
            const objectFromArray = findObjectInArray(parsed, urlParams);

            if (objectFromArray) {
                return objectFromArray;
            }
        }

        const rawAmount = getNumberValue(raw);

        if (rawAmount > 0 && isAmountLikeKey(key)) {
            return {
                amount: rawAmount,
                service: urlParams.get("service") || "Service",
                booking_id: urlParams.get("booking_id") || "0"
            };
        }
    }

    return null;
}

function findAnyStorageObjectWithAmount(urlParams) {
    const storages = [sessionStorage, localStorage];
    let fallbackObject = null;

    for (let i = 0; i < storages.length; i++) {
        for (let j = 0; j < storages[i].length; j++) {
            const key = storages[i].key(j);
            const raw = storages[i].getItem(key);

            if (!raw) {
                continue;
            }

            const parsed = parseStorageValue(raw);

            if (parsed && typeof parsed === "object") {
                const bestObject = findBestObjectFromValue(parsed, urlParams);

                if (bestObject && getPaymentAmount(bestObject, urlParams) > 0) {
                    return bestObject;
                }

                if (!fallbackObject && bestObject) {
                    fallbackObject = bestObject;
                }
            }

            const rawAmount = getNumberValue(raw);

            if (rawAmount > 0 && isAmountLikeKey(key)) {
                return {
                    amount: rawAmount,
                    service: urlParams.get("service") || "Service",
                    booking_id: urlParams.get("booking_id") || "0"
                };
            }
        }
    }

    return fallbackObject;
}

function findBestObjectFromValue(value, urlParams) {
    if (!value) {
        return null;
    }

    if (Array.isArray(value)) {
        return findObjectInArray(value, urlParams);
    }

    if (typeof value === "object") {
        if (getAmountFromObject(value, 0) > 0) {
            return value;
        }

        const keys = Object.keys(value);

        for (let i = 0; i < keys.length; i++) {
            const child = value[keys[i]];

            if (child && typeof child === "object") {
                const found = findBestObjectFromValue(child, urlParams);

                if (found && getPaymentAmount(found, urlParams) > 0) {
                    return found;
                }
            }
        }

        return value;
    }

    return null;
}

function findObjectInArray(arrayValue, urlParams) {
    let fallbackObject = null;

    for (let i = 0; i < arrayValue.length; i++) {
        const item = arrayValue[i];

        if (item && typeof item === "object") {
            if (!fallbackObject) {
                fallbackObject = item;
            }

            if (getPaymentAmount(item, urlParams) > 0) {
                return item;
            }
        }
    }

    return fallbackObject;
}

function parseStorageValue(raw) {
    try {
        return JSON.parse(raw);
    } catch (e) {
        return raw;
    }
}

function getPaymentAmount(bookingData, urlParams) {
    const objectAmount = getAmountFromObject(bookingData, 0);

    if (objectAmount > 0) {
        return objectAmount;
    }

    const urlAmount = getUrlAmount(urlParams);

    if (urlAmount > 0) {
        return urlAmount;
    }

    const directStorageAmount = findDirectStorageAmount();

    if (directStorageAmount > 0) {
        return directStorageAmount;
    }

    return 0;
}

function getAmountFromObject(value, depth) {
    if (!value || depth > 6) {
        return 0;
    }

    if (typeof value === "number" || typeof value === "string") {
        return getNumberValue(value);
    }

    if (Array.isArray(value)) {
        for (let i = 0; i < value.length; i++) {
            const amount = getAmountFromObject(value[i], depth + 1);

            if (amount > 0) {
                return amount;
            }
        }

        return 0;
    }

    if (typeof value !== "object") {
        return 0;
    }

    const priorityKeys = [
        "paymentAmount",
        "payment_amount",
        "payAmount",
        "pay_amount",
        "payableAmount",
        "payable_amount",
        "depositAmount",
        "deposit_amount",
        "bookingAmount",
        "booking_amount",
        "amount",
        "deposit",
        "totalamount",
        "totalAmount",
        "total_amount",
        "grandTotal",
        "grand_total",
        "estimatedTotal",
        "estimated_total",
        "estimatedCost",
        "estimated_cost",
        "estimatedPrice",
        "estimated_price",
        "servicePrice",
        "service_price",
        "price",
        "total",
        "totalPrice",
        "total_price",
        "cost",
        "serviceCost",
        "service_cost"
    ];

    for (let i = 0; i < priorityKeys.length; i++) {
        if (Object.prototype.hasOwnProperty.call(value, priorityKeys[i])) {
            const amount = getNumberValue(value[priorityKeys[i]]);

            if (amount > 0) {
                return amount;
            }
        }
    }

    const keys = Object.keys(value);

    for (let i = 0; i < keys.length; i++) {
        const key = keys[i];

        if (isAmountLikeKey(key)) {
            const amount = getNumberValue(value[key]);

            if (amount > 0) {
                return amount;
            }
        }
    }

    for (let i = 0; i < keys.length; i++) {
        const child = value[keys[i]];

        if (child && typeof child === "object") {
            const amount = getAmountFromObject(child, depth + 1);

            if (amount > 0) {
                return amount;
            }
        }
    }

    return 0;
}

function getUrlAmount(urlParams) {
    const keys = [
        "paymentAmount",
        "payment_amount",
        "payAmount",
        "pay_amount",
        "payableAmount",
        "payable_amount",
        "depositAmount",
        "deposit_amount",
        "bookingAmount",
        "booking_amount",
        "amount",
        "deposit",
        "totalamount",
        "totalAmount",
        "total_amount",
        "grandTotal",
        "grand_total",
        "estimatedTotal",
        "estimated_total",
        "estimatedCost",
        "estimated_cost",
        "estimatedPrice",
        "estimated_price",
        "servicePrice",
        "service_price",
        "price",
        "total",
        "totalPrice",
        "total_price",
        "cost",
        "serviceCost",
        "service_cost"
    ];

    for (let i = 0; i < keys.length; i++) {
        const value = getNumberValue(urlParams.get(keys[i]));

        if (value > 0) {
            return value;
        }
    }

    return 0;
}

function findDirectStorageAmount() {
    const storages = [sessionStorage, localStorage];

    for (let i = 0; i < storages.length; i++) {
        for (let j = 0; j < storages[i].length; j++) {
            const key = storages[i].key(j);
            const raw = storages[i].getItem(key);

            if (isAmountLikeKey(key)) {
                const amount = getNumberValue(raw);

                if (amount > 0) {
                    return amount;
                }
            }
        }
    }

    return 0;
}

function isAmountLikeKey(key) {
    if (!key) {
        return false;
    }

    const clean = String(key).toLowerCase();

    if (clean.includes("wallet") || clean.includes("reward") || clean.includes("point") || clean.includes("voucher") || clean.includes("distance") || clean.includes("travel") || clean.includes("latitude") || clean.includes("longitude")) {
        return false;
    }

    return clean.includes("amount")
        || clean.includes("deposit")
        || clean.includes("price")
        || clean.includes("total")
        || clean.includes("cost")
        || clean.includes("payable");
}

function getNumberValue(value) {
    if (value === null || value === undefined) {
        return 0;
    }

    if (typeof value === "number") {
        return isNaN(value) ? 0 : value;
    }

    const clean = String(value)
        .replace(/RM/gi, "")
        .replace(/MYR/gi, "")
        .replace(/,/g, "")
        .replace(/[^\d.]/g, "")
        .trim();

    if (clean === "") {
        return 0;
    }

    const parsed = parseFloat(clean);

    if (isNaN(parsed)) {
        return 0;
    }

    return parsed;
}

async function loadPaymentSystemData(bookingId) {
    try {
        const response = await fetch(`${baseURL}/PaymentServlet?action=fetch_initial&booking_id=${encodeURIComponent(bookingId)}`, {
            method: "GET",
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();

        if (result.status === "session_expired") {
            window.location.href = result.redirect || "index.html";
            return;
        }

        if (result.status === "error") {
            setRewardPointCheckboxState(0, true, "Use Reward Points (Unavailable)");
            setEWalletPaymentOptionState(true);
            return;
        }

        userPoints = parseInt(result.rewards_points) || 0;
        userWalletBalance = parseFloat(result.wallet_balance) || 0;
        detectedPaymentType = "first_payment";
        availableVouchers = result.vouchers || [];

        const paymentTypeTitle = document.getElementById("paymentTypeTitle");
        if (paymentTypeTitle) {
            paymentTypeTitle.innerText = "First Payment (Deposit)";
        }

        if (userPoints > 0) {
            setRewardPointCheckboxState(userPoints, false, `Use Reward Points (Available: ${userPoints} pts)`);
        } else {
            setRewardPointCheckboxState(0, true, "Use Reward Points (Available: 0 pts)");
        }

        const walletBalanceLabel = document.getElementById("walletBalanceLabel");
        if (walletBalanceLabel) {
            walletBalanceLabel.innerText = `RM ${userWalletBalance.toFixed(2)}`;
        }

        recalculatePrices();

    } catch (e) {
        setRewardPointCheckboxState(0, true, "Use Reward Points (Connection Error)");
        setEWalletPaymentOptionState(true);
    }
}

function setRewardPointCheckboxState(points, disabled, labelText) {
    const usePointsCheckbox = document.getElementById("usePointsCheckbox");
    const pointsLabel = document.getElementById("pointsLabel");

    userPoints = parseInt(points) || 0;

    if (usePointsCheckbox) {
        usePointsCheckbox.disabled = disabled;

        if (disabled) {
            usePointsCheckbox.checked = false;
        }
    }

    if (pointsLabel) {
        pointsLabel.innerText = labelText;
    }
}

function recalculatePrices() {
    let pointDiscount = 0;
    let voucherDiscount = 0;

    const usePointsCheckbox = document.getElementById("usePointsCheckbox");

    if (usePointsCheckbox && usePointsCheckbox.checked && !usePointsCheckbox.disabled && userPoints > 0) {
        const maxPointValue = userPoints / 100;
        pointDiscount = Math.min(maxPointValue, originalAmount);
    }

    if (appliedVoucherId > 0 && availableVouchers.length > 0) {
        const activeVoucher = availableVouchers.find(function(v) {
            return v.customer_voucher_id === appliedVoucherId;
        });

        if (activeVoucher) {
            const discountPercentage = parseFloat(activeVoucher.discount_amount) || 0;
            const remainingAmountAfterPoints = originalAmount - pointDiscount;
            voucherDiscount = remainingAmountAfterPoints * (discountPercentage / 100);
            voucherDiscount = Math.min(voucherDiscount, remainingAmountAfterPoints);
        }
    }

    finalAmount = originalAmount - pointDiscount - voucherDiscount;

    if (finalAmount < 0) {
        finalAmount = 0;
    }

    updateEWalletPaymentOptionState();

    const summaryBlock = document.getElementById("discountSummaryBlock");

    if (pointDiscount > 0 || voucherDiscount > 0) {
        if (summaryBlock) {
            summaryBlock.style.display = "block";
        }

        const summaryOriginal = document.getElementById("summaryOriginal");
        if (summaryOriginal) {
            summaryOriginal.innerText = `RM ${originalAmount.toFixed(2)}`;
        }

        const ptsRow = document.getElementById("summaryPointsRow");
        const summaryPoints = document.getElementById("summaryPoints");

        if (pointDiscount > 0) {
            if (ptsRow) {
                ptsRow.style.display = "flex";
            }

            if (summaryPoints) {
                summaryPoints.innerText = `RM ${pointDiscount.toFixed(2)}`;
            }
        } else {
            if (ptsRow) {
                ptsRow.style.display = "none";
            }
        }

        const vchRow = document.getElementById("summaryVoucherRow");
        const summaryVoucher = document.getElementById("summaryVoucher");

        if (voucherDiscount > 0) {
            if (vchRow) {
                vchRow.style.display = "flex";
            }

            if (summaryVoucher) {
                summaryVoucher.innerText = `RM ${voucherDiscount.toFixed(2)}`;
            }
        } else {
            if (vchRow) {
                vchRow.style.display = "none";
            }
        }

        const summaryFinal = document.getElementById("summaryFinal");
        if (summaryFinal) {
            summaryFinal.innerText = `RM ${finalAmount.toFixed(2)}`;
        }

        const displayAmount = document.getElementById("displayAmount");
        if (displayAmount) {
            displayAmount.innerText = `RM ${finalAmount.toFixed(2)}`;
        }
    } else {
        if (summaryBlock) {
            summaryBlock.style.display = "none";
        }

        const displayAmount = document.getElementById("displayAmount");
        if (displayAmount) {
            displayAmount.innerText = `RM ${originalAmount.toFixed(2)}`;
        }
    }
}

function updateEWalletPaymentOptionState() {
    const insufficientBalance = finalAmount > 0 && userWalletBalance < finalAmount;
    setEWalletPaymentOptionState(insufficientBalance);
}

function setEWalletPaymentOptionState(disabled) {
    const ewalletRadio = document.querySelector('input[name="paymentMethod"][value="ewallet"]');
    const fpxRadio = document.querySelector('input[name="paymentMethod"][value="fpx"]');
    const walletBalanceLabel = document.getElementById("walletBalanceLabel");

    if (!ewalletRadio) {
        return;
    }

    const optionLabel = ewalletRadio.closest(".method-option");
    const methodBox = optionLabel ? optionLabel.querySelector(".method-box") : null;

    ewalletRadio.disabled = disabled;

    if (disabled && ewalletRadio.checked && fpxRadio) {
        fpxRadio.checked = true;
    }

    if (optionLabel) {
        optionLabel.style.cursor = disabled ? "not-allowed" : "pointer";
    }

    if (methodBox) {
        methodBox.style.opacity = disabled ? "0.55" : "1";
        methodBox.style.cursor = disabled ? "not-allowed" : "pointer";
        methodBox.style.pointerEvents = disabled ? "none" : "auto";
    }

    if (walletBalanceLabel) {
        walletBalanceLabel.innerText = `RM ${userWalletBalance.toFixed(2)}`;

        if (disabled) {
            walletBalanceLabel.innerText = `RM ${userWalletBalance.toFixed(2)} - Insufficient`;
        }
    }
}

function executePaymentProcess(bookingData) {
    if (isProcessingPayment) {
        return;
    }

    const urlParams = new URLSearchParams(window.location.search);
    originalAmount = getPaymentAmount(bookingData, urlParams);
    finalAmount = finalAmount > 0 ? finalAmount : originalAmount;

    if (originalAmount <= 0) {
        showLocalModal("Failed!", "Payment amount is invalid. Please go back and create the booking again.", "error");
        return;
    }

    const selectedMethod = document.querySelector('input[name="paymentMethod"]:checked');

    if (!selectedMethod) {
        showLocalModal("Warning", "Please select a payment method.", "error");
        return;
    }

    const method = selectedMethod.value;

    if (method === "ewallet") {
        if (finalAmount > 0 && userWalletBalance < finalAmount) {
            showLocalModal("Failed!", "Insufficient e-wallet balance!", "error");
            updateEWalletPaymentOptionState();
            return;
        }

        clearPasswordInput();
        document.getElementById("passwordModal").style.display = "flex";
        return;
    }

    isProcessingPayment = true;

    const payNowBtn = document.getElementById("payNowBtn");
    payNowBtn.disabled = true;
    payNowBtn.innerText = "Processing...";

    const formData = buildPaymentFormData(bookingData, "fpx", "");

    sendPaymentRequest(formData);
}

function buildPaymentFormData(bookingData, method, passwordValue) {
    const usePointsCheckbox = document.getElementById("usePointsCheckbox");
    const checkedPoints = usePointsCheckbox && usePointsCheckbox.checked && !usePointsCheckbox.disabled && userPoints > 0;
    const urlParams = new URLSearchParams(window.location.search);
    const bookingId = parseInt(urlParams.get("booking_id")) || parseInt(bookingData.booking_id) || parseInt(bookingData.bookingId) || parseInt(bookingData.id) || 0;
    const pendingImagesJson = sessionStorage.getItem("pendingBookingImages") || "[]";
    const amountToSend = getPaymentAmount(bookingData, urlParams);
    const safeOriginalAmount = amountToSend > 0 ? amountToSend : originalAmount;
    const safeFinalAmount = finalAmount > 0 ? finalAmount : safeOriginalAmount;

    const formData = new URLSearchParams();
    formData.append("payment_method", method);
    formData.append("payment_type", "first_payment");
    formData.append("amount", safeOriginalAmount.toFixed(2));
    formData.append("final_amount", safeFinalAmount.toFixed(2));
    formData.append("use_points", checkedPoints ? "1" : "0");
    formData.append("customer_voucher_id", appliedVoucherId);
    formData.append("booking_id", bookingId);
    formData.append("service", bookingData.service || bookingData.servicename || bookingData.serviceName || "Service");
    formData.append("category", bookingData.category || "");
    formData.append("vendorId", bookingData.vendorId || bookingData.vendor_id || "SOS_BROADCAST");
    formData.append("service_id", bookingData.service_id || bookingData.serviceId || "1");
    formData.append("subservicebooked", bookingData.subservicebooked || bookingData.subserviceBooked || bookingData.subservice || bookingData.service || "");
    formData.append("problem", bookingData.problem || bookingData.details || bookingData.description || "");
    formData.append("userName", bookingData.userName || bookingData.name || "Customer");
    formData.append("userEmail", bookingData.userEmail || bookingData.email || "customer@email.com");
    formData.append("userPhone", bookingData.userPhone || bookingData.phone || bookingData.nophone || "0123456789");
    formData.append("date", bookingData.date || bookingData.bookingDate || bookingData.booking_date || "");
    formData.append("time", bookingData.time || bookingData.bookingTime || bookingData.booking_time || "");
    formData.append("travelFee", bookingData.travelFee || bookingData.travelfee || "0");
    formData.append("distanceKm", bookingData.distanceKm || bookingData.distancekm || "0");
    formData.append("evidencePath", bookingData.evidencePath || bookingData.evidencepath || "");
    formData.append("pending_images_json", bookingId <= 0 ? pendingImagesJson : "[]");
    formData.append("password", passwordValue === undefined || passwordValue === null ? "" : String(passwordValue));

    return formData;
}

async function sendPaymentRequest(formData) {
    try {
        const response = await fetch(`${baseURL}/PaymentServlet`, {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: formData.toString()
        });

        const result = await response.json();

        if (result.status === "session_expired") {
            window.location.href = result.redirect || "index.html";
            return;
        }

        if (result.status === "success") {
            if (result.booking_id) {
                sessionStorage.setItem("lastBookingId", result.booking_id);
            }

            sessionStorage.removeItem("pendingBooking");
            sessionStorage.removeItem("pendingBookingData");
            sessionStorage.removeItem("bookingData");
            sessionStorage.removeItem("selectedBooking");
            sessionStorage.removeItem("bookingDetails");
            sessionStorage.removeItem("pending_booking");
            sessionStorage.removeItem("paymentBooking");
            sessionStorage.removeItem("currentBooking");
            sessionStorage.removeItem("paymentData");
            sessionStorage.removeItem("selectedService");
            sessionStorage.removeItem("bookingInfo");
            sessionStorage.removeItem("checkoutData");
            sessionStorage.removeItem("pendingBookingImages");

            const paymentUrl = result.paymentUrl || result.payment_url || "";

            if (paymentUrl && paymentUrl.trim() !== "") {
                window.location.href = paymentUrl;
                return;
            }

            showSimpleReceiptModal(result.order_id || result.transaction_id, result.booking_id, finalAmount, result.payment_type || "first_payment", result.method || "Payment");
            return;
        }

        showLocalModal("Failed!", result.message || "Transaction error.", "error");
        resetPayButton();

    } catch (e) {
        showLocalModal("Failed!", "System connection error.", "error");
        resetPayButton();
    }
}

function submitPasswordVerifiedPayment() {
    if (isProcessingPayment) {
        return;
    }

    const passwordInput = getPasswordInputElement();
    const passwordEntered = passwordInput ? passwordInput.value : "";

    if (passwordEntered.trim() === "") {
        showLocalModal("Warning", "Kata laluan tidak boleh dibiarkan kosong.", "error");
        return;
    }

    const urlParams = new URLSearchParams(window.location.search);
    originalAmount = getPaymentAmount(globalBookingData, urlParams);
    finalAmount = finalAmount > 0 ? finalAmount : originalAmount;

    if (originalAmount <= 0) {
        showLocalModal("Failed!", "Payment amount is invalid. Please go back and create the booking again.", "error");
        return;
    }

    isProcessingPayment = true;

    document.getElementById("passwordModal").style.display = "none";

    const payNowBtn = document.getElementById("payNowBtn");
    payNowBtn.disabled = true;
    payNowBtn.innerText = "Processing Wallet Direct...";

    const formData = buildPaymentFormData(globalBookingData, "ewallet", passwordEntered);

    sendPaymentRequest(formData);
}

function getPasswordInputElement() {
    return document.getElementById("modalPasswordInput")
        || document.querySelector("#passwordModal input[type='password']")
        || document.querySelector("#passwordModal input");
}

function clearPasswordInput() {
    const passwordInput = getPasswordInputElement();

    if (passwordInput) {
        passwordInput.value = "";
    }
}

function resetPayButton() {
    isProcessingPayment = false;

    const payNowBtn = document.getElementById("payNowBtn");

    if (payNowBtn) {
        payNowBtn.disabled = false;
        payNowBtn.innerText = "Pay Now";
    }
}

function showLocalModal(title, message, type) {
    const modal = document.getElementById("notificationModal");
    const modalTitle = document.getElementById("modalTitle");
    const modalMsg = document.getElementById("modalMessage");
    const statusIcon = document.getElementById("statusIcon");
    const iconSymbol = document.getElementById("iconSymbol");

    if (modal && modalTitle && modalMsg) {
        modalTitle.innerText = title;
        modalMsg.innerText = message;

        if (type === "error") {
            if (statusIcon) {
                statusIcon.style.backgroundColor = "#ff4d4d";
            }

            if (iconSymbol) {
                iconSymbol.innerText = "×";
            }
        } else {
            if (statusIcon) {
                statusIcon.style.backgroundColor = "#2ecc71";
            }

            if (iconSymbol) {
                iconSymbol.innerText = "✓";
            }
        }

        modal.style.display = "flex";

        document.getElementById("modalContinueBtn").onclick = function() {
            modal.style.display = "none";
        };
    }
}

function showSimpleReceiptModal(txnId, bookingId, paidAmount, paymentType, methodName) {
    const modal = document.getElementById("notificationModal");
    const modalTitle = document.getElementById("modalTitle");
    const modalMsg = document.getElementById("modalMessage");
    const statusIcon = document.getElementById("statusIcon");
    const iconSymbol = document.getElementById("iconSymbol");

    if (modal && modalTitle && modalMsg) {
        modalTitle.innerText = "Payment Successful!";

        modalMsg.innerHTML = `
            <div class="receipt-box">
                <div class="receipt-line">
                    <span class="receipt-label">Transaction ID:</span>
                    <span class="receipt-value">${txnId || 'N/A'}</span>
                </div>
                <div class="receipt-line">
                    <span class="receipt-label">Booking ID:</span>
                    <span class="receipt-value">${bookingId || 'N/A'}</span>
                </div>
                <div class="receipt-line">
                    <span class="receipt-label">Payment Stage:</span>
                    <span class="receipt-value">First Payment</span>
                </div>
                <div class="receipt-line">
                    <span class="receipt-label">Amount Paid:</span>
                    <span class="receipt-value" style="color: var(--success);">RM ${parseFloat(paidAmount).toFixed(2)}</span>
                </div>
                <div class="receipt-line">
                    <span class="receipt-label">Method:</span>
                    <span class="receipt-value">${methodName || "Payment"}</span>
                </div>
            </div>
            <p style="margin:0; font-size:14px; color:var(--muted);">Thank you for choosing TukangNow!</p>
        `;

        if (statusIcon) {
            statusIcon.style.backgroundColor = "#2ecc71";
        }

        if (iconSymbol) {
            iconSymbol.innerText = "✓";
        }

        modal.style.display = "flex";

        document.getElementById("modalContinueBtn").onclick = function() {
            modal.style.display = "none";
            resetPayButton();
            window.location.href = "myorder.html";
        };
    }
}