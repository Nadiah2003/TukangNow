const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

let receiptNextUrl = "";

document.addEventListener("DOMContentLoaded", function() {
    const params = new URLSearchParams(window.location.search);

    const directId = params.get("id") || params.get("bookingId");
    const paymentType = normalizePaymentType(params.get("payment_type") || params.get("type") || "");
    receiptNextUrl = params.get("next") || "";

    const paymentReference =
        params.get("order_id") ||
        params.get("billcode") ||
        params.get("transaction_id") ||
        params.get("refno") ||
        directId;

    if (params.get("bookingId")) {
        fetchReceiptData(params.get("bookingId"), paymentReference, paymentType);
    } else if (paymentReference) {
        fetchReceiptData(paymentReference, paymentReference, paymentType);
    } else {
        alert("Receipt reference not found.");
    }
});

async function fetchReceiptData(idOrRef, directRef = null, paymentType = "first_payment") {
    const loadingOverlay = document.getElementById("loadingOverlay");

    if (loadingOverlay) {
        loadingOverlay.classList.add("active");
    }

    try {
        const query = new URLSearchParams();
        query.append("idOrRef", idOrRef);
        query.append("payment_type", normalizePaymentType(paymentType));

        if (receiptNextUrl) {
            query.append("next", receiptNextUrl);
        }

        const response = await fetch(`${baseURL}/ReceiptServlet?${query.toString()}`, {
            method: "GET",
            credentials: "same-origin"
        });

        const result = await response.json();

        if (result.status === "session_expired") {
            window.location.href = result.redirect || "index.html";
            return;
        }

        if (result.status === "success") {
            receiptNextUrl = result.next_url || receiptNextUrl;
            renderReceipt(result.data, directRef, result.payment_type || paymentType);
        } else {
            alert("Error loading receipt data: " + result.message);
        }
    } catch (error) {
        console.error("Fetch error:", error);
        alert("Failed to load receipt data.");
    } finally {
        if (loadingOverlay) {
            loadingOverlay.classList.remove("active");
        }
    }
}

function renderReceipt(data, directRef, paymentType) {
    const cleanPaymentType = normalizePaymentType(paymentType);
    const isSecondPayment = cleanPaymentType === "second_payment";

    document.getElementById("idLabel").textContent = "Booking ID";
    document.getElementById("bookingId").textContent = `#${data.id || "N/A"}`;

    const finalRef = directRef && directRef !== "0" ? directRef : data.payment_reference;
    document.getElementById("refNo").textContent = finalRef && finalRef !== "0" ? finalRef : "N/A";

    document.getElementById("receiptDate").textContent = `Date: ${data.paymentdate || data.bookingdate || "Today"}`;

    const dynamicDetails = document.getElementById("dynamicReceiptDetails");
    const paymentRows = document.getElementById("paymentRowsContainer");

    const serviceName = data.subservicebooked || "General Service";
    const vendorName = data.vendor_name || "Searching Vendor...";
    const appointment = data.bookingdate || "-";
    const problem = data.problem || "-";
    const bookingStatus = data.status || "-";
    const paymentMethod = data.paymentmethod || "-";
    const distanceKm = Number(data.distancekm || 0);
    const deposit = Number(data.deposit || 0);
    const travelFee = Number(data.travelfee || 0);
    const balanceAmount = Number(data.totalamount || 0);
    const paidAmount = Number(data.paymentpaid || data.totalamount || 0);

    if (dynamicDetails) {
        dynamicDetails.innerHTML = `
            <div class="details-section">
                <h3>Service Details</h3>
                <div class="detail-row">
                    <span>Service</span>
                    <strong>${escapeHtml(serviceName)}</strong>
                </div>
                <div class="detail-row">
                    <span>Vendor</span>
                    <strong>${escapeHtml(vendorName)}</strong>
                </div>
                <div class="detail-row">
                    <span>Appointment</span>
                    <strong>${escapeHtml(appointment)}</strong>
                </div>
                <div class="detail-row">
                    <span>Problem</span>
                    <strong>${escapeHtml(problem)}</strong>
                </div>
                <div class="detail-row">
                    <span>Status</span>
                    <strong>${escapeHtml(bookingStatus)}</strong>
                </div>
            </div>`;
    }

    if (paymentRows) {
        if (isSecondPayment) {
            paymentRows.innerHTML = `
                <div class="price-row">
                    <span>Balance Amount</span>
                    <span>RM ${balanceAmount.toFixed(2)}</span>
                </div>
                <div class="price-row">
                    <span>Payment Method</span>
                    <span>${escapeHtml(paymentMethod)}</span>
                </div>`;
        } else {
            paymentRows.innerHTML = `
                <div class="price-row">
                    <span>Deposit Amount</span>
                    <span>RM ${deposit.toFixed(2)}</span>
                </div>
                <div class="price-row">
                    <span>Travel Fee</span>
                    <span>RM ${travelFee.toFixed(2)}</span>
                </div>
                <div class="price-row">
                    <span>Distance</span>
                    <span>${distanceKm.toFixed(2)} KM</span>
                </div>
                <div class="price-row">
                    <span>Payment Method</span>
                    <span>${escapeHtml(paymentMethod)}</span>
                </div>`;
        }
    }

    const totalPaidEl = document.getElementById("totalPaid");
    const refundNoteEl = document.getElementById("refundNoteText");

    if (totalPaidEl) {
        totalPaidEl.textContent = `RM ${paidAmount.toFixed(2)}`;
    }

    if (refundNoteEl) {
        if (isSecondPayment) {
            refundNoteEl.textContent = "Note: This receipt is for balance payment only and does not include the deposit paid earlier.";
        } else {
            refundNoteEl.textContent = "Note: If the booking is rejected, the payment will be credited into your wallet and you can pay using your wallet balance for future bookings.";
        }
    }
}

function downloadReceipt() {
    const element = document.getElementById("receiptContent");
    const bookingId = document.getElementById("bookingId").textContent.replace("#", "").trim() || "receipt";

    const opt = {
        margin: 10,
        filename: `TukangNow_Receipt_${bookingId}.pdf`,
        image: {
            type: "jpeg",
            quality: 0.98
        },
        html2canvas: {
            scale: 2
        },
        jsPDF: {
            unit: "mm",
            format: "a4",
            orientation: "portrait"
        }
    };

    html2pdf().set(opt).from(element).save();
}

function goToMyOrders() {
    if (receiptNextUrl && receiptNextUrl.trim() !== "") {
        window.location.href = receiptNextUrl;
        return;
    }

    window.location.href = "myorder.html";
}

function normalizePaymentType(type) {
    const clean =String(type || "").trim().toLowerCase().replaceAll("-", "_").replaceAll(" ", "_");

    if (clean === "second_payment" || clean === "balance") {
        return "second_payment";
    }

    return "first_payment";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}