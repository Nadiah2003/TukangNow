const pathParts = window.location.pathname.split('/').filter(Boolean);
const contextPath = pathParts.length > 0 && !pathParts[0].includes('.') ? `/${pathParts[0]}` : "";
const baseURL = `${window.location.origin}${contextPath}`;

document.addEventListener('DOMContentLoaded', function() {
    fetchMainCategory();

    document.getElementById('addInputBtn').addEventListener('click', function() {
        addNewInput();
    });

    document.getElementById('modalContinueBtn').addEventListener('click', function() {
        closeModal();
    });

    document.getElementById('serviceForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const inputs = document.querySelectorAll('input[name="subservice"]');
        const servicesArray = Array.from(inputs)
            .map(input => input.value.trim())
            .filter(val => val !== "");
        
        const combinedServices = servicesArray.join(', ');

        try {
            const response = await fetch(`${baseURL}/AddServiceServlet`, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 
                    'Content-Type': 'application/x-www-form-urlencoded' 
                },
                body: `subservices=${encodeURIComponent(combinedServices)}`
            });

            if (!response.ok) {
                throw new Error("Server error: " + response.status);
            }

            const result = await response.json();

            if (result.status === "success") {
                showModal("Success!", "Services updated successfully!", true);
            } else {
                showModal("Failed!", result.message || "Update failed.", false); 
            }
        } catch (error) {
            showModal("Failed!", "Connection error.", false);
        }
    });
});

async function fetchMainCategory() {
    try {
        const response = await fetch(`${baseURL}/AddServiceServlet`, {
            method: 'GET',
            credentials: 'same-origin'
        });

        if (!response.ok) {
            throw new Error("Server error: " + response.status);
        }

        const data = await response.json();

        if (data.status === "success") {
            document.getElementById('mainCatDisplay').innerText = data.mainCategory || "-";

            if (data.existingSubServices && data.existingSubServices.trim() !== "" && data.existingSubServices !== "null") {
                const subArray = data.existingSubServices.split(',').map(s => s.trim()).filter(s => s !== "");
                const existingInputs = document.querySelectorAll('input[name="subservice"]');
                
                subArray.forEach((val, index) => {
                    if (existingInputs[index]) {
                        existingInputs[index].value = val;
                    } else {
                        renderInputRow(val);
                    }
                });
            }
        } else {
            showModal("Failed!", data.message || "No service record found.", false);
        }
    } catch (error) {
        showModal("Failed!", "Error loading data.", false);
    }
}

function renderInputRow(value = "") {
    const container = document.getElementById('inputContainer');
    const div = document.createElement('div');
    div.className = 'input-group dynamic-row';
    
    div.innerHTML = `
        <span class="icon">🛠️</span>
        <input type="text" name="subservice" placeholder="Service Name" value="${escapeHtml(value)}" required>
        <button type="button" class="btn-remove">×</button>
    `;

    div.querySelector('.btn-remove').addEventListener('click', function() {
        div.remove();
    });

    container.appendChild(div);
}

function addNewInput() {
    renderInputRow("");
}

function showModal(title, message, isSuccess) {
    const modal = document.getElementById('statusModal');
    const icon = document.getElementById('modalIcon');
    const iconSymbol = document.getElementById('iconSymbol');
    
    document.getElementById('modalTitle').innerText = title;
    document.getElementById('modalMessage').innerText = message;
    
    if (isSuccess) {
        icon.classList.add('success-state');
        iconSymbol.innerText = "✓";
    } else {
        icon.classList.remove('success-state');
        iconSymbol.innerText = "×";
    }

    modal.style.display = "flex";
}

function closeModal() {
    const modal = document.getElementById('statusModal');
    modal.style.display = "none";

    if (document.getElementById('modalTitle').innerText === "Success!") {
        window.location.href = 'profilevendor.html'; 
    }
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}