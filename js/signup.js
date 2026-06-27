const baseURL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/TukangNow`;

document.addEventListener("DOMContentLoaded", function() {
    const phoneInput = document.getElementById('phone');
    const phoneWarn = document.getElementById('phone-warn');
    const passwordInput = document.getElementById('password');
    const signupForm = document.getElementById('signupForm');
    const eyeIcon = document.getElementById('toggleSignupPass');
    const emailInput = document.getElementById('email');

    blockPasswordManagerDetection();

    if (document.getElementById('address')) document.getElementById('address').removeAttribute('readonly');
    if (document.getElementById('city')) document.getElementById('city').removeAttribute('readonly');
    if (document.getElementById('state')) document.getElementById('state').removeAttribute('readonly');
    if (document.getElementById('postcode')) document.getElementById('postcode').removeAttribute('readonly');

    if (eyeIcon && passwordInput) {
        eyeIcon.addEventListener('click', function() {
            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                eyeIcon.style.fill = '#1848a0';
            } else {
                passwordInput.type = 'password';
                eyeIcon.style.fill = '#ccc';
            }
        });
    }

    if (passwordInput) {
        passwordInput.addEventListener('input', function() {
            const val = passwordInput.value;
            updateReq('char-length', val.length >= 6, "Minimum 6 characters");
            updateReq('char-upper', /[A-Z]/.test(val), "One capital letter (A-Z)");
            updateReq('char-number', /[0-9]/.test(val), "One number (0-9)");
            updateReq('char-special', /[!@#$%^&*(),.?":{}|<>]/.test(val), "One special character (@#$%^&*)");
        });
    }

    function blockPasswordManagerDetection() {
        const stamp = Date.now().toString();

        if (signupForm) {
            signupForm.setAttribute('autocomplete', 'off');
            signupForm.setAttribute('data-lpignore', 'true');
            signupForm.setAttribute('data-form-type', 'other');
        }

        if (emailInput) {
            emailInput.setAttribute('name', 'tn_customer_contact_' + stamp);
            emailInput.setAttribute('autocomplete', 'off');
            emailInput.setAttribute('data-lpignore', 'true');
            emailInput.setAttribute('data-form-type', 'other');
            emailInput.setAttribute('readonly', 'readonly');
            emailInput.addEventListener('focus', function() {
                emailInput.removeAttribute('readonly');
            });
        }

        if (passwordInput) {
            passwordInput.setAttribute('name', 'tn_customer_secret_' + stamp);
            passwordInput.setAttribute('autocomplete', 'off');
            passwordInput.setAttribute('data-lpignore', 'true');
            passwordInput.setAttribute('data-form-type', 'other');
            passwordInput.setAttribute('readonly', 'readonly');
            passwordInput.addEventListener('focus', function() {
                passwordInput.removeAttribute('readonly');
            });
        }
    }

    function prepareSignupFieldsForRedirect() {
        if (passwordInput) {
            passwordInput.value = "";
            passwordInput.type = "text";
            passwordInput.setAttribute('readonly', 'readonly');
            passwordInput.setAttribute('autocomplete', 'off');
        }

        if (emailInput) {
            emailInput.value = "";
            emailInput.setAttribute('readonly', 'readonly');
            emailInput.setAttribute('autocomplete', 'off');
        }
    }

    function updateReq(id, isValid, text) {
        const el = document.getElementById(id);
        if (!el) return;

        if (isValid) {
            el.classList.remove('invalid');
            el.classList.add('valid');
            el.innerText = "✔ " + text;
        } else {
            el.classList.remove('valid');
            el.classList.add('invalid');
            el.innerText = "✖ " + text;
        }
    }

    if (phoneInput) {
        phoneInput.addEventListener('input', function() {
            let rawVal = phoneInput.value.replace(/\D/g, '');

            if (rawVal.startsWith('60')) {
                rawVal = rawVal.substring(2);
            } else if (rawVal.startsWith('0')) {
                rawVal = rawVal.substring(1);
            }

            if (rawVal.length > 10) {
                rawVal = rawVal.substring(0, 10);
            }

            let formattedVal = rawVal;

            if (rawVal.length > 2) {
                formattedVal = rawVal.substring(0, 2) + '-' + rawVal.substring(2);
            }

            phoneInput.value = formattedVal;

            if (rawVal.length >= 8 && rawVal.length <= 10) {
                phoneWarn.style.display = 'none';
            } else if (rawVal.length > 0) {
                phoneWarn.style.display = 'block';
                phoneWarn.innerText = 'Nombor telefon tidak sah (Sila masukkan nombor telefon yang betul)';
            } else {
                phoneWarn.style.display = 'none';
            }
        });
    }

    const openTerms = document.getElementById('openTerms');
    const termsModal = document.getElementById('termsModal');
    const closeModal = document.getElementById('closeModal');
    const agreeCheck = document.getElementById('agreeCheck');
    const modalBody = document.querySelector('.modal-body');

    if (agreeCheck) {
        agreeCheck.addEventListener('click', function(e) {
            if (agreeCheck.disabled) {
                e.preventDefault();
                showStatus("Sila Baca Terma", "Sila klik pautan Terma & Syarat dan skrol sehingga bawah terlebih dahulu.", "error");
            }
        });
    }

    if (openTerms && termsModal) {
        openTerms.addEventListener('click', function(e) {
            e.preventDefault();
            termsModal.style.display = 'flex';

            if (closeModal && (!agreeCheck || !agreeCheck.checked)) {
                closeModal.disabled = true;
                closeModal.innerText = "Sila Skrol Ke Bawah Semuanya";
                closeModal.style.opacity = "0.5";
                closeModal.style.cursor = "not-allowed";
            }
        });
    }

    if (modalBody && closeModal) {
        modalBody.addEventListener('scroll', function() {
            if (modalBody.scrollHeight - modalBody.scrollTop <= modalBody.clientHeight + 5) {
                closeModal.disabled = false;
                closeModal.innerText = "I Agree";
                closeModal.style.opacity = "1";
                closeModal.style.cursor = "pointer";
            }
        });
    }

    if (closeModal && termsModal) {
        closeModal.addEventListener('click', function() {
            if (closeModal.disabled) return;

            termsModal.style.display = 'none';

            if (agreeCheck) {
                agreeCheck.disabled = false;
                agreeCheck.checked = true;
            }
        });
    }

    if (termsModal) {
        window.addEventListener('click', function(e) {
            if (e.target === termsModal) {
                termsModal.style.display = 'none';
            }
        });
    }

    async function getCoordinatesFromAddress(address, postcode, city, state) {
        const queryList = [
            `${address}, ${postcode}, ${city}, ${state}, Malaysia`,
            `${address}, ${postcode}, ${state}, Malaysia`,
            `${postcode}, ${city}, ${state}, Malaysia`,
            `${city}, ${state}, Malaysia`
        ];

        for (const query of queryList) {
            const cleanQuery = query
                .replaceAll("undefined", "")
                .replaceAll("null", "")
                .replaceAll("  ", " ")
                .trim();

            if (!cleanQuery || cleanQuery.length < 10) {
                continue;
            }

            try {
                const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=my&q=${encodeURIComponent(cleanQuery)}`;
                const response = await fetch(url);
                const data = await response.json();

                if (Array.isArray(data) && data.length > 0) {
                    const latitude = parseFloat(data[0].lat);
                    const longitude = parseFloat(data[0].lon);

                    if (!isNaN(latitude) && !isNaN(longitude)) {
                        return {
                            latitude: latitude,
                            longitude: longitude
                        };
                    }
                }
            } catch (error) {
                console.log("Geocoding failed for query:", cleanQuery);
            }
        }

        return null;
    }

    if (signupForm) {
        signupForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            const name = document.getElementById('fullname').value.trim();
            const email = document.getElementById('email').value.trim();
            const phone = phoneInput ? phoneInput.value.replace(/\D/g, '').trim() : '';
            const password = passwordInput ? passwordInput.value : '';
            const fullPhone = '60' + phone;

            const address = document.getElementById('address').value.trim();
            const city = document.getElementById('city') ? document.getElementById('city').value.trim() : '';
            const state = document.getElementById('state').value.trim();
            const postcode = document.getElementById('postcode').value.trim();

            if (!agreeCheck || !agreeCheck.checked || agreeCheck.disabled) {
                showStatus("Registration Failed", "Sila baca dan setuju dengan Terma & Syarat keselamatan.", "error");
                return;
            }

            if (!name || !email || !phone || !password || !address || !state || !postcode) {
                showStatus("Registration Failed", "Sila lengkapkan semua butiran maklumat borang pendaftaran.", "error");
                return;
            }

            if (phone.length < 8 || phone.length > 10) {
                showStatus("Registration Failed", "Nombor telefon bimbit tidak memenuhi syarat.", "error");
                return;
            }

            const isPassValid = val => (val.length >= 6 && /[A-Z]/.test(val) && /[0-9]/.test(val) && /[!@#$%^&*(),.?":{}|<>]/.test(val));

            if (!isPassValid(password)) {
                showStatus("Registration Failed", "Kata laluan tidak memenuhi tahap keselamatan.", "error");
                return;
            }

            showStatus("Finding Location", "Sila tunggu sebentar. Sistem sedang mendapatkan latitude dan longitude berdasarkan alamat anda...", "loading");

            const coordinates = await getCoordinatesFromAddress(address, postcode, city, state);

            if (!coordinates) {
                showStatus("Registration Failed", "Alamat tidak dapat dikesan. Sila semak semula alamat, postcode, city dan state.", "error");
                return;
            }

            showStatus("Creating Account", "Sila tunggu sebentar sementara akaun diproses...", "loading");

            try {
                const formData = new URLSearchParams();
                formData.append('action', 'register');
                formData.append('fullname', name);
                formData.append('email', email);
                formData.append('phone', fullPhone);
                formData.append('password', password);
                formData.append('address', address);
                formData.append('city', city);
                formData.append('state', state);
                formData.append('postcode', postcode);
                formData.append('latitude', coordinates.latitude);
                formData.append('longitude', coordinates.longitude);

                const response = await fetch(`${baseURL}/api/signup`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    body: formData.toString()
                });

                const result = await response.json();

                if (result.status === "success") {
                    prepareSignupFieldsForRedirect();
                    showStatus("Success", "Pendaftaran berjaya! Membuka halaman log masuk...", "success");
                    setTimeout(() => {
                        window.location.href = `${baseURL}/login.html`;
                    }, 2000);
                } else {
                    showStatus("Registration Failed", result.message || "Gagal menyimpan rekod profil ke pelayan.", "error");
                }

            } catch (error) {
                console.error("Signup core process breakdown:", error);
                showStatus("Registration Failed", "Ralat sistem dalaman semasa proses pendaftaran akaun.", "error");
            }
        });
    }

    function showStatus(title, message, type) {
        const toast = document.getElementById('toastNotification');
        if (!toast) return;

        const toastTitle = document.getElementById('toastTitle');
        const toastMessage = document.getElementById('toastMessage');
        const spinner = toast.querySelector('.toast-spinner');
        const iconCircle = document.getElementById('toastIconCircle');
        const iconSpan = document.getElementById('toastIcon');

        if (toastTitle) toastTitle.innerText = title;
        if (toastMessage) toastMessage.innerText = message;

        if (type === "loading") {
            if (spinner) spinner.style.display = "block";
            if (iconCircle) iconCircle.style.display = "none";
        } else {
            if (spinner) spinner.style.display = "none";
            if (iconCircle) iconCircle.style.display = "flex";

            if (type === "success") {
                if (iconCircle) iconCircle.style.backgroundColor = "#2e7d32";
                if (iconSpan) iconSpan.innerText = "✔";
            } else {
                if (iconCircle) iconCircle.style.backgroundColor = "#d32f2f";
                if (iconSpan) iconSpan.innerText = "✖";
            }
        }

        toast.classList.add('show');

        if (type !== "loading") {
            setTimeout(() => {
                hideStatus();
            }, 4500);
        }
    }

    function hideStatus() {
        const toast = document.getElementById('toastNotification');

        if (toast) {
            toast.classList.remove('show');
        }
    }
});