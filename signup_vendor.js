const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

let selectedFiles = [];
let profileFile = null;
let map, marker;

$(document).ready(function() {
    const passwordInput = $('#password');
    const togglePassword = $('#togglePassword');
    const phoneInput = $('#phone');
    const phoneWarn = $('#phone-warn');
    const statusModal = $('#statusModal');
    const termsModal = $('#termsModal');
    const dropZone = $('#dropZone');
    const profileZone = $('#profileZone');
    const docInput = $('#docFile');
    const profileInput = $('#profileFile');
    const emailInput = $('#email').length ? $('#email') : $('input[type="email"]').first();

    blockPasswordManagerDetection();

    function blockPasswordManagerDetection() {
        const stamp = Date.now().toString();
        const form = $('#vendorForm');

        if (form.length) {
            form.attr('autocomplete', 'off');
            form.attr('data-lpignore', 'true');
            form.attr('data-form-type', 'other');
        }

        if (emailInput.length) {
            emailInput.attr('name', 'tn_vendor_contact_' + stamp);
            emailInput.attr('autocomplete', 'off');
            emailInput.attr('data-lpignore', 'true');
            emailInput.attr('data-form-type', 'other');
            emailInput.attr('readonly', true);

            emailInput.on('focus', function() {
                emailInput.removeAttr('readonly');
            });
        }

        if (passwordInput.length) {
            passwordInput.attr('name', 'tn_vendor_secret_' + stamp);
            passwordInput.attr('autocomplete', 'off');
            passwordInput.attr('data-lpignore', 'true');
            passwordInput.attr('data-form-type', 'other');
            passwordInput.attr('readonly', true);

            passwordInput.on('focus', function() {
                passwordInput.removeAttr('readonly');
            });
        }
    }

    function prepareVendorSignupFieldsForRedirect() {
        if (passwordInput.length) {
            passwordInput.val('');
            passwordInput.attr('type', 'text');
            passwordInput.attr('readonly', true);
            passwordInput.attr('autocomplete', 'off');
        }

        if (emailInput.length) {
            emailInput.val('');
            emailInput.attr('readonly', true);
            emailInput.attr('autocomplete', 'off');
        }
    }

    function initMap() {
        const defaultLat = 5.3182100;
        const defaultLng = 103.1189400;

        map = L.map('map').setView([defaultLat, defaultLng], 15);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OpenStreetMap'
        }).addTo(map);

        marker = L.marker([defaultLat, defaultLng], { draggable: true }).addTo(map);

        $('#latitude').val(defaultLat);
        $('#longitude').val(defaultLng);

        const geocoder = L.Control.Geocoder.nominatim();

        const searchControl = L.Control.geocoder({
            geocoder: geocoder,
            defaultMarkGeocode: false,
            placeholder: "Cari nama jalan atau bandar...",
            collapsed: false
        }).addTo(map);

        searchControl.on('markgeocode', function(e) {
            const bbox = e.geocode.bbox;
            const center = e.geocode.center;

            map.fitBounds(bbox);
            marker.setLatLng(center);

            $('#latitude').val(center.lat.toFixed(7));
            $('#longitude').val(center.lng.toFixed(7));
        });

        marker.on('dragend', function() {
            const position = marker.getLatLng();

            $('#latitude').val(position.lat.toFixed(7));
            $('#longitude').val(position.lng.toFixed(7));
        });

        map.on('click', function(e) {
            marker.setLatLng(e.latlng);

            $('#latitude').val(e.latlng.lat.toFixed(7));
            $('#longitude').val(e.latlng.lng.toFixed(7));
        });
    }

    initMap();

    if (togglePassword.length) {
        togglePassword.on('click', function() {
            const type = passwordInput.attr('type') === 'password' ? 'text' : 'password';

            passwordInput.attr('type', type);
            this.style.color = type === 'text' ? 'var(--primary-blue)' : '#999';
        });
    }

    phoneInput.on('input', function(e) {
        let num = e.target.value.replace(/\D/g, '');

        if (num.startsWith('0')) {
            num = num.substring(1);
        }

        num = num.substring(0, 11);

        e.target.value = num.length > 2 ? num.substring(0, 2) + '-' + num.substring(2) : num;

        phoneWarn.css('display', (num.length > 0 && num.length < 9) ? 'block' : 'none');
    });

    function showStatus(type, title, message, redirect = null) {
        const card = $('#statusCard');
        const icon = $('#statusIcon');
        const titleEl = $('#statusTitle');
        const msgEl = $('#statusMessage');
        const btn = $('#statusOkBtn');

        card.attr('class', 'status-card ' + (type === 'success' ? 'status-success' : 'status-fail'));
        icon.html(type === 'success' ? '✓' : '✕');
        titleEl.text(title);
        msgEl.text(message);

        statusModal.css('display', 'flex');

        btn.off('click').on('click', function() {
            statusModal.css('display', 'none');

            if (redirect) {
                window.location.href = redirect;
            }
        });
    }

    $('#openModal').on('click', function(e) {
        e.preventDefault();

        termsModal.css('display', 'flex');

        const modalBody = $('.modal-body')[0];
        modalBody.scrollTop = 0;

        if (modalBody.scrollHeight <= modalBody.clientHeight) {
            $('#closeModal').prop('disabled', false);
        } else {
            $('#closeModal').prop('disabled', true);
        }
    });

    $('.modal-body').on('scroll', function() {
        const totalHeight = this.scrollHeight;
        const currentScroll = this.scrollTop + this.clientHeight;

        if (totalHeight - currentScroll <= 10) {
            $('#closeModal').prop('disabled', false);
        }
    });

    $('#closeModal').on('click', function() {
        termsModal.css('display', 'none');

        const ck = $('#agree');

        ck.prop('disabled', false);
        ck.prop('checked', true);
    });

    passwordInput.on('input', function() {
        const val = this.value;

        const requirements = {
            'req-length': val.length >= 6,
            'req-capital': /[A-Z]/.test(val),
            'req-number': /[0-9]/.test(val),
            'req-special': /[!@#$%^&*(),.?":{}|<>]/.test(val)
        };

        for (const [id, isValid] of Object.entries(requirements)) {
            const el = $('#' + id);

            if (el.length) {
                el.toggleClass('valid', isValid);
                el.toggleClass('invalid', !isValid);
                el.html((isValid ? '✔ ' : '✖ ') + el.text().substring(2));
            }
        }
    });

    dropZone.on('click', function(e) {
        if ($(e.target).closest('.remove-file-btn').length) {
            return;
        }

        docInput[0].click();
    });

    profileZone.on('click', function() {
        profileInput[0].click();
    });

    docInput.on('click', function(e) {
        e.stopPropagation();
    });

    profileInput.on('click', function(e) {
        e.stopPropagation();
    });

    docInput.on('change', function(e) {
        if (e.target.files && e.target.files.length > 0) {
            handleDocFiles(e.target.files);
        }

        this.value = '';
    });

    profileInput.on('change', function(e) {
        if (e.target.files && e.target.files[0]) {
            handleProfileFile(e.target.files[0]);
        }

        this.value = '';
    });

    dropZone.on('dragenter dragover', function(e) {
        e.preventDefault();
        e.stopPropagation();
        dropZone.addClass('dragover');
    });

    dropZone.on('dragleave dragend', function(e) {
        e.preventDefault();
        e.stopPropagation();
        dropZone.removeClass('dragover');
    });

    dropZone.on('drop', function(e) {
        e.preventDefault();
        e.stopPropagation();
        dropZone.removeClass('dragover');

        const files = e.originalEvent.dataTransfer.files;

        if (files && files.length > 0) {
            handleDocFiles(files);
        }
    });

    profileZone.on('dragenter dragover', function(e) {
        e.preventDefault();
        e.stopPropagation();
        profileZone.addClass('dragover');
    });

    profileZone.on('dragleave dragend', function(e) {
        e.preventDefault();
        e.stopPropagation();
        profileZone.removeClass('dragover');
    });

    profileZone.on('drop', function(e) {
        e.preventDefault();
        e.stopPropagation();
        profileZone.removeClass('dragover');

        const files = e.originalEvent.dataTransfer.files;

        if (files && files[0]) {
            handleProfileFile(files[0]);
        }
    });

    $(document).on('dragover drop', function(e) {
        e.preventDefault();
        e.stopPropagation();
    });

    function handleProfileFile(file) {
        if (!file) {
            return;
        }

        if (!file.type.startsWith('image/')) {
            showStatus('fail', 'Invalid File', 'Please upload an image file for your professional photo.');
            return;
        }

        profileFile = file;

        $('#profileNameDisplay').html(`<span class="blue-text">${file.name}</span>`);

        const reader = new FileReader();

        reader.onload = function(e) {
            $('#profilePreviewContainer').html(`
                <img src="${e.target.result}" style="width:80px;height:80px;border-radius:50%;object-fit:cover;border:2px solid var(--primary-blue);margin-top:10px;">
            `);
        };

        reader.readAsDataURL(profileFile);
    }

    function handleDocFiles(files) {
        if (!files || files.length === 0) {
            return;
        }

        selectedFiles = selectedFiles.concat(Array.from(files));

        renderFileList();
    }

    function renderFileList() {
        const container = $('#fileListContainer');

        if (selectedFiles.length === 0) {
            container.html('');
            $('#fileNameDisplay').html(`<span class="blue-text">Click here</span> or drag & drop license documents`);
            return;
        }

        $('#fileNameDisplay').html(`<span class="blue-text">${selectedFiles.length} file(s) selected</span>`);

        container.html(selectedFiles.map((f, i) => `
            <div class="file-item">
                <span>📄 ${f.name}</span>
                <span style="color:red;cursor:pointer" class="remove-file-btn" data-index="${i}">✖</span>
            </div>
        `).join(''));
    }

    $(document).on('click', '.remove-file-btn', function(e) {
        e.preventDefault();
        e.stopPropagation();

        const index = $(this).data('index');

        selectedFiles.splice(index, 1);

        renderFileList();
    });

    $('#vendorForm').on('submit', function(e) {
        e.preventDefault();

        const phoneRaw = phoneInput.val().replace(/\D/g, '');

        if (phoneRaw.length < 9) {
            phoneWarn.css('display', 'block');
            return;
        }

        if (!$('#agree').is(':checked')) {
            showStatus('fail', 'Wait!', 'Please agree to the terms first.');
            return;
        }

        const submitBtn = $('#submitBtn');

        submitBtn.prop('disabled', true).text('Registering...');

        finalSubmit();
    });

    async function finalSubmit() {
        const vendorEmailValue = emailInput.length ? emailInput.val().trim() : '';
        const vendorPasswordValue = passwordInput.length ? passwordInput.val() : '';
        const formData = new FormData($('#vendorForm')[0]);

        if (vendorEmailValue) {
            formData.set('email', vendorEmailValue);
        }

        if (vendorPasswordValue) {
            formData.set('password', vendorPasswordValue);
        }

        if (profileFile) {
            formData.append('profileImage', profileFile);
        }

        formData.set('nophone', phoneInput.val().replace(/\D/g, ''));

        selectedFiles.forEach(function(file) {
            formData.append('docFiles[]', file);
        });

        $.ajax({
            url: 'SignupVendorServlet',
            type: 'POST',
            data: formData,
            contentType: false,
            processData: false,
            dataType: 'json',
            success: function(data) {
                if (data.status === 'success') {
                    prepareVendorSignupFieldsForRedirect();
                    showStatus('success', 'Success!', 'Registration successful! Please wait for admin approval.', 'login.html');
                } else {
                    showStatus('fail', 'Failed!', data.message || 'Registration failed.');
                    resetBtn();
                }
            },
            error: function(xhr, textStatus, errorThrown) {
                let message = 'Could not connect to server.';

                if (xhr.responseText) {
                    message = xhr.responseText.replace(/<[^>]*>/g, '').trim();

                    if (message.length > 250) {
                        message = message.substring(0, 250) + '...';
                    }
                } else if (errorThrown) {
                    message = errorThrown;
                } else if (textStatus) {
                    message = textStatus;
                }

                showStatus('fail', 'Server Error', message);
                resetBtn();
            }
        });
    }

    function resetBtn() {
        const b = $('#submitBtn');

        b.prop('disabled', false).text('Signup');
    }
});