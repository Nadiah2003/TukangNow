const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

let inactivityTimeout;
const INACTIVITY_LIMIT = 15 * 60 * 1000;

function resetInactivityTimer() {
    clearTimeout(inactivityTimeout);
    inactivityTimeout = setTimeout(logoutUserDueToInactivity, INACTIVITY_LIMIT);
}

function logoutUserDueToInactivity() {
    sessionStorage.clear();
    window.location.href = `${baseURL}/login.html`;
}

function setupInactivityListeners() {
    window.addEventListener('mousemove', resetInactivityTimer);
    window.addEventListener('click', resetInactivityTimer);
    window.addEventListener('keydown', resetInactivityTimer);
    window.addEventListener('scroll', resetInactivityTimer);
    window.addEventListener('touchstart', resetInactivityTimer);
    resetInactivityTimer();
}

document.addEventListener('DOMContentLoaded', function() {
    setupInactivityListeners();

    const passwordInput = document.getElementById('password');
    const eyeIcon = document.querySelector('.eye-icon');
    const loginForm = document.getElementById('loginForm');
    const modalBtn = document.getElementById('loginStatusBtn');
    const rememberLogin = document.getElementById('rememberLogin');
    const emailInput = document.getElementById('email');

    const savedEmail = localStorage.getItem("rememberedLoginEmail");

    if (savedEmail && emailInput) {
        emailInput.value = savedEmail;
    }

    if (rememberLogin && savedEmail) {
        rememberLogin.checked = true;
    }

    if (passwordInput && eyeIcon) {
        passwordInput.addEventListener('input', function() {
            if (this.value.length > 0) {
                eyeIcon.classList.add('active');
            } else {
                eyeIcon.classList.remove('active');
                passwordInput.setAttribute('type', 'password');
                eyeIcon.style.fill = '#ccc';
            }
        });

        eyeIcon.addEventListener('click', function() {
            const isPass = passwordInput.getAttribute('type') === 'password';
            passwordInput.setAttribute('type', isPass ? 'text' : 'password');
            this.style.fill = isPass ? '#1848a0' : '#ccc';
        });
    }

    if (modalBtn) {
        modalBtn.addEventListener('click', closeLoginStatusModal);
    }

    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const email = document.getElementById('email').value.trim();
            const password = passwordInput.value;
            const btn = e.target.querySelector('button');
            const shouldRemember = rememberLogin && rememberLogin.checked;

            btn.innerText = "Checking...";
            btn.disabled = true;

            fetch(`${baseURL}/api/login`, {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
            })
            .then(res => res.json())
            .then(async data => {
                if (data.status === "success") {
                    const role = data.role ? data.role.toLowerCase().trim() : "";

                    sessionStorage.setItem("userId", data.userId || "");
                    sessionStorage.setItem("role", data.role || "");
                    sessionStorage.setItem("userName", data.name || "");
                    sessionStorage.setItem("state", data.state || "");
                    sessionStorage.setItem("adminLevel", data.adminLevel || "");

                    if (shouldRemember) {
                        localStorage.setItem("rememberedLoginEmail", email);
                        await saveCredentialToBrowser(email, password);
                    } else {
                        localStorage.removeItem("rememberedLoginEmail");
                    }

                    if (role === "admin" || role === "leader admin" || role.includes("admin")) {
                        window.location.href = `${baseURL}/homeadmin.html`;
                    } else if (role === "vendor") {
                        window.location.href = `${baseURL}/homevendor.html`;
                    } else {
                        window.location.href = `${baseURL}/homecustomer.html`;
                    }

                    return;
                }

                showLoginStatusModal("Failed!", data.message || "Invalid Email or Password", "failed");
            })
            .catch(() => {
                showLoginStatusModal("Failed!", "Connection Error", "failed");
            })
            .finally(() => {
                btn.innerText = "Login";
                btn.disabled = false;
            });
        });
    }
});

async function saveCredentialToBrowser(email, password) {
    try {
        if ("credentials" in navigator && window.PasswordCredential) {
            const credential = new PasswordCredential({
                id: email,
                name: email,
                password: password
            });

            await navigator.credentials.store(credential);
        }
    } catch (error) {
        console.log("Password manager save prompt was not available.");
    }
}

function showLoginStatusModal(title, message, type) {
    const modal = document.getElementById('loginStatusModal');
    const iconCircle = document.getElementById('loginStatusIconCircle');
    const icon = document.getElementById('loginStatusIcon');
    const titleEl = document.getElementById('loginStatusTitle');
    const messageEl = document.getElementById('loginStatusMessage');

    if (!modal || !iconCircle || !icon || !titleEl || !messageEl) return;

    titleEl.innerText = title;
    messageEl.innerText = message;
    iconCircle.classList.remove('failed', 'success');

    if (type === "success") {
        iconCircle.classList.add('success');
        icon.innerText = "✓";
    } else {
        iconCircle.classList.add('failed');
        icon.innerText = "×";
    }

    modal.classList.add('is-open');
    modal.setAttribute('aria-hidden', 'false');
}

function closeLoginStatusModal() {
    const modal = document.getElementById('loginStatusModal');

    if (!modal) return;

    modal.classList.remove('is-open');
    modal.setAttribute('aria-hidden', 'true');
}