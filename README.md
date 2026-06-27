# 🛠️ TukangNow - On-Demand Home Services Platform

TukangNow is a **web-based on-demand home services system** that connects customers with verified service providers such as electricians, plumbers, and other home maintenance professionals. The platform ensures fast booking, transparent pricing, real-time job tracking, and secure payment processing.

---

## 📌 Project Overview

TukangNow aims to solve common issues in the home services industry such as:
- Lack of trust in service providers
- Unclear and inconsistent pricing
- Slow manual booking process
- Lack of emergency service response
- Poor communication between customer and vendor

This system provides a **centralized digital marketplace** where users can book services instantly and safely.

---

## 🚀 Features

### 👤 Customer
- Register and login
- Browse and book services
- Emergency booking
- Track job status in real-time
- Make secure payments (ToyyibPay)
- Submit ratings and reviews
- Report issues to admin

### 🧑‍🔧 Vendor (Service Provider)
- Accept or reject job requests
- View active and pending bookings
- Manage job status (on the way, started, completed)
- Wallet system (earnings & withdrawal)
- Chat with admin support
- View reports from customers

### 🛡️ Admin / Authenticator
- Verify vendors (ST/SPAN certification)
- Manage users and vendors
- Handle reports and disputes
- Monitor system activities
- Suspend or ban accounts if necessary

---

## 🧱 Tech Stack

### Frontend
- HTML5
- CSS3
- JavaScript (Vanilla)

### Backend
- Java Servlet (Maven Project)
- JDBC

### Database
- MySQL

### External Services / APIs
- ToyyibPay (Payment Gateway)
- OpenStreetMap (OMS) / Stadia Maps (Location Services)
- Email / SMS Notification (Free API alternatives such as EmailJS / SMTP)

---

## 🗄️ Database Modules
- customer
- vendor
- booking
- booking_material_items
- wallet_transactions
- customer_wallet
- vendor_wallet
- reports
- admin_chats

---

## ⚙️ System Architecture

- Web-based 3-tier architecture
  - Presentation Layer (HTML/CSS/JS)
  - Business Logic Layer (Java Servlets)
  - Data Layer (MySQL Database)

---

## 📂 Project Structure
TukangNow/
│
├── Web Pages/
│ ├── META-INF/
│ ├── WEB-INF/
│ ├── css/
│ ├── events/
│ ├── evidence/
│ ├── icon/
│ ├── image/
│ ├── js/
│ ├── licences/
│ ├── profiles/
│ ├── resit/
├── Source Packages/
│ ├── Config/
│ ├── DAO/
│ ├── Model/
│ ├── Servlet/
│
├── Test Packages/
├── Dependencies/
├── Java Dependencies/
├── Project Files/

---

## 🔐 Key Highlights

- Real-time booking system
- Emergency service request feature
- Secure payment integration
- Vendor verification system
- Chat support between user and admin
- Report & complaint system
- Wallet & earnings system for vendors

---

## 📊 Future Improvements

- Live GPS tracking (real-time map updates)
- Push notification system
- AI-based service recommendation
- Multi-language support
- Mobile app version (Android/iOS)

---

## 👨‍💻 Developer Notes

This project is developed as a **Final Year Project (FYP)** focusing on real-world on-demand service system design and implementation using Java Web technologies.

---

## 📄 License

This project is for academic purposes only.

---

## 💙 Acknowledgement

Special thanks to supervisors, lecturers, and testers who contributed to the development of TukangNow system. Also my friends who always give support to finish this system.
