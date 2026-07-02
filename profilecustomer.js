const pathParts = window.location.pathname.split("/").filter(Boolean);
const appName = pathParts.length > 0 && !pathParts[0].includes(".") ? pathParts[0] : "";
const baseURL = `${window.location.origin}${appName ? "/" + appName : ""}`;

const malaysiaGeoData = {
    "Wilayah Persekutuan Kuala Lumpur": {
        "Pusat Bandaraya": ["50100"],
        "Jalan Tunku Abdul Rahman": ["50350"],
        "Jalan Tun Perak": ["50400"],
        "Ampang KL": ["55100"],
        "Kampung Pandan": ["55000"],
        "Dataran Pandan": ["55100"],
        "Bangsar": ["59000"],
        "Pantai": ["59200"],
        "Bukit Bandaraya": ["59100"],
        "Bukit Damansara": ["50490"],
        "Damansara Heights": ["50490"],
        "Brickfields": ["50470"],
        "Little India": ["50470"],
        "Cheras KL": ["56100"],
        "Taman Maluri": ["55100"],
        "Shamelin": ["56100"],
        "Kepong": ["52100"],
        "Metro Prima": ["52100"],
        "Jinjang": ["52000"],
        "Setapak": ["53300"],
        "Danau Kota": ["53300"],
        "Taman Ibu Kota": ["53000"],
        "Wangsa Maju Seksyen 1 - 10": ["53300"],
        "Taman Melati": ["53100"],
        "Taman Permata KL": ["53100"],
        "Sentul": ["51000"],
        "Bandar Baru Sentul": ["51100"],
        "Jalan Ipoh": ["51200"],
        "Jalan Kuching KL": ["51200"],
        "Sungai Besi": ["57100"],
        "Chan Sow Lin": ["57000"],
        "Sri Petaling": ["57000"],
        "Bukit Jalil": ["57000"],
        "Seputeh": ["58000"],
        "Taman Desa": ["58100"],
        "Jalan Klang Lama": ["58200"],
        "Old Klang Road": ["58200"],
        "Segambut": ["51200"],
        "Mont Kiara": ["50480"],
        "Sri Hartamas": ["50480"],
        "Kerinchi": ["59200"],
        "Bangsar South": ["59200"]
    },
    "Wilayah Persekutuan Putrajaya": {
        "Pusat Pentadbiran Kerajaan Persekutuan (Presint 1 - 20)": ["62000"],
        "Presint 1": ["62000"],
        "Presint 2": ["62000"],
        "Presint 3": ["62000"],
        "Presint 4": ["62000"],
        "Presint 5": ["62200"],
        "Presint 6": ["62200"],
        "Presint 7": ["62250"],
        "Presint 8": ["62250"],
        "Presint 9": ["62250"],
        "Presint 10": ["62250"],
        "Presint 11": ["62300"],
        "Presint 12": ["62300"],
        "Presint 13": ["62300"],
        "Presint 14": ["62300"],
        "Presint 15": ["62300"],
        "Presint 16": ["62674"],
        "Presint 17": ["62674"],
        "Presint 18": ["62674"],
        "Presint 19": ["62674"],
        "Presint 20": ["62675"]
    },
    "Wilayah Persekutuan Labuan": {
        "Bandar Labuan / Pusat Bandar": ["87000"],
        "Kampung Air": ["87000"],
        "Batu Manikar": ["87000"],
        "Bebuloh": ["87000"],
        "Layang-Layangan": ["87000"],
        "Lubok Temiang": ["87000"],
        "Nagalang": ["87000"],
        "Paut": ["87000"],
        "Rancha-Rancha": ["87000"],
        "Sungai Lada": ["87000"],
        "Tanjung Aru": ["87000"]
    },
    "Selangor": {
        "Shah Alam": ["40000", "40100", "40150", "40160", "40170", "40400", "40460", "40470"],
        "Petaling Jaya (PJ)": ["46000", "46100", "46150", "46200", "46300", "46350", "46400", "47300", "47301", "47308", "47400", "47410"],
        "Subang Jaya / USJ / Sunway": ["47500", "47600", "47610", "47620", "47630", "47640", "47650"],
        "Puchong": ["47100", "47110", "47120", "47130", "47140", "47150", "47160", "47170", "47190"],
        "Klang": ["41000", "41050", "41100", "41150", "41200", "41250", "41300", "41400"],
        "Port Klang (Pelabuhan Klang)": ["42000", "42920"],
        "Cyberjaya": ["63000", "63100", "63200", "63300"],
        "Sepang": ["43900"],
        "KLIA / KLIA2": ["64000"],
        "Kajang": ["43000"],
        "Bangi / Bandar Baru Bangi": ["43600", "43650"],
        "Seri Kembangan / Serdang": ["43300", "43400"],
        "Semenyih": ["43500"],
        "Ampang (Selangor)": ["68000"],
        "Batu Caves": ["68100"],
        "Rawang": ["48000", "48010", "48020", "48050"],
        "Gombak": ["53100"],
        "Banting (Bandar Asal)": ["42700"],
        "Telok Panglima Garang (Asing!)": ["42500"],
        "Jenjarom": ["42600"],
        "Pulau Carey": ["42960"],
        "Kuala Selangor": ["45000"],
        "Tanjong Karang": ["45500"],
        "Sekinchan": ["45400"],
        "Bestari Jaya (Batang Berjuntai)": ["45600"],
        "Sabak Bernam": ["45200"],
        "Sungai Besar": ["45300"],
        "Kuala Kubu Bharu": ["44000"],
        "Batang Kali": ["44300"],
        "Serendah": ["48200"],
        "Sungai Buloh": ["47000"],
        "Puncak Alam": ["42300"],
        "Kapar": ["42200"],
        "Jeram": ["45800"]
    },
    "Johor": {
        "Johor Bahru": ["80000", "80100", "80200", "80300", "80400", "81100", "81200"],
        "Skudai": ["81300", "81310"],
        "Iskandar Puteri / Gelang Patah": ["79000", "79100", "81550"],
        "Pasir Gudang": ["81700"],
        "Masai": ["81750"],
        "Ulu Tiram": ["81800"],
        "Plentong": ["81750"],
        "Kulai": ["81000"],
        "Senai": ["81400"],
        "Batu Pahat": ["83000"],
        "Yong Peng": ["83700"],
        "Ayer Hitam": ["86100"],
        "Muar": ["84000"],
        "Pagoh": ["84500"],
        "Tangkak": ["84900"],
        "Kluang": ["86000"],
        "Simpang Renggam": ["86200"],
        "Paloh": ["86600"],
        "Segamat": ["85000"],
        "Labis": ["85300"],
        "Kota Tinggi": ["81900"],
        "Pengerang / Bandar Penawar": ["81930"],
        "Pontian": ["82000"],
        "Pekan Nanas": ["82100"],
        "Benut": ["82200"],
        "Mersing": ["86800", "86810"]
    },
    "Perak": {
        "Ipoh": ["30000", "30100", "30200", "30300", "30400", "31400"],
        "Batu Gajah": ["31000"],
        "Chemor": ["31200"],
        "Tanjung Rambutan": ["31250"],
        "Gopeng": ["31600"],
        "Kampar": ["31900"],
        "Sitiawan": ["32000"],
        "Seri Manjung": ["32040"],
        "Lumut": ["32200"],
        "Pulau Pangkor": ["32300"],
        "Ayer Tawar": ["32400"],
        "Pantai Remis": ["32700"],
        "Kuala Kangsar": ["33000"],
        "Sungai Siput": ["31100"],
        "Lenggong": ["33400"],
        "Gerik": ["33300"],
        "Taiping": ["34000"],
        "Kamunting": ["34600"],
        "Parit Buntar": ["34200"],
        "Bagan Serai": ["34300"],
        "Kuala Kurau": ["34350"],
        "Tapah": ["35000"],
        "Bidor": ["35500"],
        "Sungai Siput (Selatan)": ["31110"],
        "Sungkai": ["35600"],
        "Tanjung Malim": ["35900"],
        "Teluk Intan": ["36000"],
        "Bagan Datuk": ["36100"],
        "Hutan Melintang": ["36400"],
        "Kampong Gajah": ["36800"],
        "Parit": ["32800"],
        "Sri Iskandar": ["32610"]
    },
    "Penang": {
        "Georgetown": ["10000", "10100", "10200", "10300", "10400", "10460"],
        "Ayer Itam": ["11400"],
        "Paya Terubong": ["11500"],
        "Gelugor": ["11700"],
        "Bayan Lepas / Bayan Baru": ["11900", "11950"],
        "Tanjung Bungah": ["11200"],
        "Batu Ferringhi": ["11100"],
        "Balik Pulau": ["11000"],
        "Butterworth": ["13000", "13400"],
        "Perai": ["13600"],
        "Seberang Jaya": ["13700"],
        "Kepala Batas": ["13200"],
        "Tasek Gelugor": ["13300"],
        "Bukit Mertajam": ["14000"],
        "Simpang Ampat": ["14100"],
        "Sungai Bakap": ["14200"],
        "Nibong Tebal": ["14300"]
    },
    "Kedah": {
        "Alor Setar": ["05000", "05100", "05200", "05300", "05400"],
        "Jitra": ["06000"],
        "Kuala Nerang": ["06300"],
        "Pokok Sena": ["06400"],
        "Changlun": ["06050"],
        "Bukit Kayu Hitam": ["06060"],
        "Pendang": ["06700"],
        "Yan": ["06900"],
        "Gurun": ["08100"],
        "Langkawi (Kuah)": ["07000"],
        "Sungai Petani": ["08000"],
        "Bedong": ["08110"],
        "Sik": ["08200"],
        "Kulim": ["09000"],
        "Lunas": ["09600"],
        "Baling": ["09100"],
        "Kuala Ketil": ["09300"],
        "Padang Serai": ["09400"]
    },
    "Terengganu": {
        "Kuala Terengganu": ["20000", "21000"],
        "Kuala Nerus (UMT / Gong Badak)": ["21030"],
        "Marang": ["21600"],
        "Kuala Berang / Hulu Tgn": ["21700"],
        "Besut / Jertih": ["22000"],
        "Kuala Besut": ["22200"],
        "Bandar Permaisuri / Setiu": ["22100"],
        "Dungun": ["23000"],
        "Paka": ["23100"],
        "Kerteh": ["23200"],
        "Chukai / Kemaman": ["24000"],
        "Kijal": ["24100"]
    },
    "Pahang": {
        "Kuantan": ["25000", "25100", "25200", "25300", "25350"],
        "Gambang": ["26300"],
        "Pekan": ["26600"],
        "Maran": ["26500"],
        "Bandar Pusat Jengka": ["26400"],
        "Jerantut": ["27000"],
        "Kuala Lipis": ["27200"],
        "Raub": ["27600"],
        "Bentong": ["28700"],
        "Genting Highlands": ["69000"],
        "Karak": ["28600"],
        "Temerloh": ["28000"],
        "Mentakab": ["28400"],
        "Triang / Bera": ["28300"],
        "Muadzam Shah": ["26700"],
        "Kuala Rompin": ["26800"],
        "Tanah Rata (Cameron)": ["39000"],
        "Brinchang (Cameron)": ["39100"]
    },
    "Kelantan": {
        "Kota Bharu": ["15000", "15100", "15200", "15300", "15400"],
        "Kubang Kerian": ["16150"],
        "Pengkalan Chepa": ["16100"],
        "Bachok": ["16300"],
        "Pasir Puteh": ["16800"],
        "Tumpat": ["16200"],
        "Rantau Panjang": ["17200"],
        "Pasir Mas": ["17000"],
        "Tanah Merah": ["17500"],
        "Jeli": ["17600"],
        "Kuala Krai": ["18000"],
        "Gua Musang": ["18300"],
        "Machang": ["18500"]
    },
    "Negeri Sembilan": {
        "Seremban": ["70000", "70100", "70200", "70300", "70400", "70450"],
        "Senawang": ["70450"],
        "Rantau": ["71200"],
        "Rembau": ["71300"],
        "Tampin": ["73000"],
        "Kuala Klawang / Jelebu": ["71600"],
        "Kuala Pilah": ["72000"],
        "Bahau / Jempol": ["72100"],
        "Port Dickson": ["71000"],
        "Lukut": ["71010"],
        "Si Rusa": ["71050"],
        "Nilai": ["71800"],
        "Mantin": ["71700"]
    },
    "Melaka": {
        "Bandar Melaka / Melaka Tengah": ["75000", "75100", "75200", "75300", "75400"],
        "Ayer Keroh": ["75450"],
        "Batu Berendam": ["75350"],
        "Klebang": ["75200"],
        "Masjid Tanah": ["78300"],
        "Alor Gajah": ["78000"],
        "Jasin": ["77000"],
        "Merlimau": ["77300"]
    },
    "Perlis": {
        "Kangar": ["01000"],
        "Kuala Perlis": ["02000"],
        "Padang Besar": ["02100"],
        "Kaki Bukit": ["02200"],
        "Arau": ["02600"],
        "Simpang Empat (Perlis)": ["02700"]
    },
    "Sabah": {
        "Kota Kinabalu": ["88000", "88100", "88200", "88300", "88400", "88500"],
        "Penampang": ["89500"],
        "Putatan": ["88200"],
        "Inanam": ["88450"],
        "Tuaran": ["89200"],
        "Kota Belud": ["89150"],
        "Kudat": ["89050"],
        "Ranau": ["89300"],
        "Papar": ["89600"],
        "Beaufort": ["89800"],
        "Keningau": ["89000"],
        "Tenom": ["89900"],
        "Sandakan": ["90000"],
        "Beluran": ["90100"],
        "Tawau": ["91000"],
        "Lahad Datu": ["91100"],
        "Semporna": ["91300"],
        "Kunak": ["91200"]
    },
    "Sarawak": {
        "Kuching": ["93000", "93100", "93200", "93300", "93400", "93500"],
        "Padawan": ["93250"],
        "Bau": ["94000"],
        "Lundu": ["94500"],
        "Kota Samarahan": ["94300"],
        "Serian": ["94700"],
        "Sri Aman": ["95000"],
        "Betong": ["95700"],
        "Saratok": ["95400"],
        "Sarikei": ["96100"],
        "Bintangor": ["96500"],
        "Sibu": ["96000"],
        "Kanowit": ["96700"],
        "Kapit": ["96800"],
        "Mukah": ["96400"],
        "Dalat": ["96300"],
        "Bintulu": ["97000"],
        "Tatau": ["97200"],
        "Miri": ["98000", "98100"],
        "Marudi": ["98050"],
        "Limbang": ["98700"],
        "Lawas": ["98850"]
    }
};

document.addEventListener("DOMContentLoaded", function() {
    loadProfileData();
    bindUIActions();
    initGeoDropdowns();

    const newPass = document.getElementById("newPass");
    const oldPass = document.getElementById("oldPass");

    if (newPass) {
        newPass.addEventListener("input", checkPassword);
    }

    if (oldPass) {
        oldPass.addEventListener("input", checkPassword);
    }
});

function bindUIActions() {
    document.getElementById("btnOpenEditModal")?.addEventListener("click", openEditModal);
    document.getElementById("btnOpenImageModal")?.addEventListener("click", function() {
        openModal("imageModal");
    });
    document.getElementById("btnOpenPasswordModal")?.addEventListener("click", openPasswordModal);
    document.getElementById("btnCancelEdit")?.addEventListener("click", function() {
        closeModal("editModal");
    });
    document.getElementById("btnCancelImage")?.addEventListener("click", function() {
        closeModal("imageModal");
    });
    document.getElementById("btnCancelPass")?.addEventListener("click", function() {
        closeModal("passwordModal");
    });
    document.getElementById("btnSaveEdit")?.addEventListener("click", saveProfile);
    document.getElementById("btnUploadImage")?.addEventListener("click", processImageUpload);
    document.getElementById("btnUpdatePass")?.addEventListener("click", updatePassword);
    document.getElementById("statusOkBtn")?.addEventListener("click", closeStatusModal);
    document.getElementById("btnLogout")?.addEventListener("click", function() {
        openModal("logoutModal");
    });
    document.getElementById("cancelLogout")?.addEventListener("click", function() {
        closeModal("logoutModal");
    });
    document.getElementById("confirmLogout")?.addEventListener("click", processLogout);
}

function initGeoDropdowns() {
    const stateInput = document.getElementById("editState");
    const cityInput = document.getElementById("editCity");
    const postcodeInput = document.getElementById("editPostcode");
    const stateDatalist = document.getElementById("stateList");
    const cityDatalist = document.getElementById("cityList");
    const postcodeDatalist = document.getElementById("postcodeList");

    stateDatalist.innerHTML = "";

    Object.keys(malaysiaGeoData).forEach(function(state) {
        let opt = document.createElement("option");
        opt.value = state;
        stateDatalist.appendChild(opt);
    });

    stateInput?.addEventListener("input", function() {
        const selectedState = this.value;

        cityDatalist.innerHTML = "";
        postcodeDatalist.innerHTML = "";
        cityInput.value = "";
        postcodeInput.value = "";

        if (malaysiaGeoData[selectedState]) {
            cityInput.disabled = false;
            Object.keys(malaysiaGeoData[selectedState]).forEach(function(city) {
                let opt = document.createElement("option");
                opt.value = city;
                cityDatalist.appendChild(opt);
            });
        } else {
            cityInput.disabled = true;
            postcodeInput.disabled = true;
        }
    });

    cityInput?.addEventListener("input", function() {
        const selectedState = stateInput.value;
        const selectedCity = this.value;

        postcodeDatalist.innerHTML = "";
        postcodeInput.value = "";

        if (malaysiaGeoData[selectedState] && malaysiaGeoData[selectedState][selectedCity]) {
            postcodeInput.disabled = false;
            malaysiaGeoData[selectedState][selectedCity].forEach(function(postcode) {
                let opt = document.createElement("option");
                opt.value = postcode;
                postcodeDatalist.appendChild(opt);
            });
        } else {
            postcodeInput.disabled = true;
        }
    });
}

function loadProfileData() {
    fetch(`${baseURL}/ProfileCustomerServlet?action=get&t=${Date.now()}`, {
        method: "GET",
        credentials: "same-origin"
    })
        .then(function(res) {
            return res.json();
        })
        .then(function(data) {
            if (handleSessionExpired(data)) {
                return;
            }

            if (data.status === "success") {
                fillProfileUI(data);
            } else {
                showStatusModal("error", "Failed!", data.message || "Unable to load profile.");
            }
        })
        .catch(function() {
            showStatusModal("error", "Failed!", "Server connection error.");
        });
}

function handleSessionExpired(data) {
    if (data && data.status === "session_expired") {
        sessionStorage.clear();
        window.location.href = data.redirect || "index.html";
        return true;
    }

    return false;
}

function fillProfileUI(data) {
    document.getElementById("userName").innerText = data.name || "N/A";
    document.getElementById("userEmail").innerText = data.email || "N/A";
    document.getElementById("userPhone").innerText = data.nophone || "-";
    document.getElementById("userAddress").innerText = data.address || "-";
    document.getElementById("userPostcode").innerText = data.postcode || "";
    document.getElementById("userCity").innerText = data.city || "";

    let stateDisplay = data.state || "";

    if (stateDisplay.toLowerCase() === "kl" || stateDisplay.toLowerCase() === "kuala lumpur" || stateDisplay === "W.P. Kuala Lumpur") {
        stateDisplay = "Wilayah Persekutuan Kuala Lumpur";
    }

    document.getElementById("userState").innerText = stateDisplay;

    const countryWrap = document.getElementById("userCountryWrap");

    if (data.country) {
        document.getElementById("userCountry").innerText = data.country;

        if (countryWrap) {
            countryWrap.style.display = "inline";
        }
    } else {
        if (countryWrap) {
            countryWrap.style.display = "none";
        }
    }

    const img = document.getElementById("displayImg");
    img.src = resolveProfileImageSrc(data.profile_path);

    document.getElementById("editName").value = data.name || "";
    document.getElementById("editEmail").value = data.email || "";
    document.getElementById("editPhone").value = data.nophone || "";
    document.getElementById("editAddress").value = data.address || "";
    document.getElementById("editState").value = stateDisplay;

    const cityInput = document.getElementById("editCity");
    const postcodeInput = document.getElementById("editPostcode");

    cityInput.disabled = false;
    postcodeInput.disabled = false;

    if (stateDisplay && malaysiaGeoData[stateDisplay]) {
        const cityDatalist = document.getElementById("cityList");
        cityDatalist.innerHTML = "";

        Object.keys(malaysiaGeoData[stateDisplay]).forEach(function(city) {
            let opt = document.createElement("option");
            opt.value = city;
            cityDatalist.appendChild(opt);
        });

        if (data.city) {
            const postcodeDatalist = document.getElementById("postcodeList");
            postcodeDatalist.innerHTML = "";

            if (malaysiaGeoData[stateDisplay][data.city]) {
                malaysiaGeoData[stateDisplay][data.city].forEach(function(postcode) {
                    let opt = document.createElement("option");
                    opt.value = postcode;
                    postcodeDatalist.appendChild(opt);
                });
            }
        }
    }

    cityInput.value = data.city || "";
    postcodeInput.value = data.postcode || "";
    document.getElementById("editCountry").value = data.country || "";
}

function resolveProfileImageSrc(profilePath) {
    const defaultImage = "image/profile.png";
    const cleanPath = String(profilePath || "").trim();

    if (cleanPath === "") {
        return defaultImage;
    }

    const fileName = cleanPath.split("\\").pop().split("/").pop();
    const lowerPath = cleanPath.toLowerCase();
    const lowerFileName = fileName.toLowerCase();

    if (lowerFileName === "profile.png" || lowerPath === "image/profile.png") {
        return defaultImage;
    }

    if (lowerPath.startsWith("http://") || lowerPath.startsWith("https://") || lowerPath.startsWith("data:image")) {
        return cleanPath;
    }

    if (lowerPath.startsWith("profiles/")) {
        return `${baseURL}/${cleanPath}?t=${Date.now()}`;
    }

    return `${baseURL}/profiles/${fileName}?t=${Date.now()}`;
}

function saveProfile() {
    const params = new URLSearchParams();
    params.append("action", "updateProfile");
    params.append("name", document.getElementById("editName").value);
    params.append("email", document.getElementById("editEmail").value);
    params.append("phone", document.getElementById("editPhone").value);
    params.append("address", document.getElementById("editAddress").value);
    params.append("postcode", document.getElementById("editPostcode").value);
    params.append("city", document.getElementById("editCity").value);
    params.append("state", document.getElementById("editState").value);
    params.append("country", document.getElementById("editCountry").value);

    fetch(`${baseURL}/ProfileCustomerServlet`, {
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
            if (handleSessionExpired(data)) {
                return;
            }

            closeModal("editModal");
            showStatusModal(data.status, data.status === "success" ? "Success" : "Failed", data.message);
            loadProfileData();
        })
        .catch(function() {
            showStatusModal("error", "Failed", "Server connection error.");
        });
}

function processImageUpload() {
    const fileInput = document.getElementById("modalFileInput");

    if (!fileInput.files[0]) {
        showStatusModal("error", "Failed", "Please select an image first.");
        return;
    }

    const formData = new FormData();
    formData.append("action", "uploadImage");
    formData.append("profilePic", fileInput.files[0]);

    fetch(`${baseURL}/ProfileCustomerServlet`, {
        method: "POST",
        credentials: "same-origin",
        body: formData
    })
        .then(function(res) {
            return res.json();
        })
        .then(function(data) {
            if (handleSessionExpired(data)) {
                return;
            }

            closeModal("imageModal");
            showStatusModal(data.status, data.status === "success" ? "Success" : "Failed", data.message);
            fileInput.value = "";
            loadProfileData();
        })
        .catch(function() {
            showStatusModal("error", "Failed", "Server connection error.");
        });
}

function updatePassword() {
    const params = new URLSearchParams();
    params.append("action", "changePassword");
    params.append("oldPass", document.getElementById("oldPass").value);
    params.append("newPass", document.getElementById("newPass").value);

    fetch(`${baseURL}/ProfileCustomerServlet`, {
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
            if (handleSessionExpired(data)) {
                return;
            }

            if (data.status === "success") {
                closeModal("passwordModal");
                showStatusModal("success", "Success", "Password updated!");
            } else {
                showStatusModal("error", "Failed", data.message);
            }
        })
        .catch(function() {
            showStatusModal("error", "Failed", "Server connection error.");
        });
}

function processLogout() {
    const params = new URLSearchParams();
    params.append("action", "logout");

    fetch(`${baseURL}/ProfileCustomerServlet`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: params.toString()
    })
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            sessionStorage.clear();

            if (data.status === "success" || data.status === "session_expired") {
                window.location.href = data.redirect || "index.html";
            } else {
                closeModal("logoutModal");
                showStatusModal("error", "Failed!", data.message);
            }
        })
        .catch(function() {
            sessionStorage.clear();
            closeModal("logoutModal");
            showStatusModal("error", "Error!", "A system error occurred.");
        });
}

function openModal(id) {
    document.getElementById(id)?.classList.add("is-open");
}

function closeModal(id) {
    document.getElementById(id)?.classList.remove("is-open");
}

function openEditModal() {
    openModal("editModal");
}

function openPasswordModal() {
    document.getElementById("oldPass").value = "";
    document.getElementById("newPass").value = "";
    checkPassword();
    openModal("passwordModal");
}

function togglePass(inputId, iconContainer) {
    const input = document.getElementById(inputId);
    const isPassword = input.type === "password";
    input.type = isPassword ? "text" : "password";

    if (isPassword) {
        iconContainer.innerHTML = `<svg class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>`;
    } else {
        iconContainer.innerHTML = `<svg class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>`;
    }
}

function checkPassword() {
    const pass = document.getElementById("newPass")?.value || "";
    const oldP = document.getElementById("oldPass")?.value || "";
    const rules = {
        length: pass.length >= 6,
        upper: /[A-Z]/.test(pass),
        number: /[0-9]/.test(pass),
        special: /[@#$%^&*]/.test(pass)
    };

    updateRequirementUI("char-length", rules.length, "Minimum 8 characters");
    updateRequirementUI("char-upper", rules.upper, "One capital letter (A-Z)");
    updateRequirementUI("char-number", rules.number, "One number (0-9)");
    updateRequirementUI("char-special", rules.special, "One special character (@#$%^&*)");

    const btn = document.getElementById("btnUpdatePass");

    if (btn) {
        btn.disabled = !(rules.length && rules.upper && rules.number && rules.special && oldP.trim() !== "");
    }
}

function updateRequirementUI(id, ok, text) {
    const el = document.getElementById(id);

    if (!el) {
        return;
    }

    el.className = "requirement " + (ok ? "valid" : "invalid");
    el.textContent = (ok ? "✔ " : "✖ ") + text;
}

function showStatusModal(type, title, message) {
    const overlay = document.getElementById("statusModal");
    document.getElementById("statusTitle").textContent = title;
    document.getElementById("statusMessage").textContent = message || "";
    const circle = document.getElementById("statusIconCircle");
    circle.className = "status-icon-circle " + (type === "success" ? "success" : "error");
    document.getElementById("statusIcon").textContent = type === "success" ? "✓" : "✕";
    overlay.classList.add("is-open");
}

function closeStatusModal() {
    document.getElementById("statusModal")?.classList.remove("is-open");
}