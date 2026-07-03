# HealthFreak - AI Healthcare Assistant

**HealthFreak** is a comprehensive Android application designed to act as your personal healthcare companion. It integrates artificial intelligence, real-time health tracking, and emergency response systems to provide a complete digital health ecosystem for users. 

This repository contains the **Frontend (Android)** application built in Kotlin.

## 🚀 Key Features

*   🤖 **AI Doctor Consultation (`ConsultActivity`)**
    *   Powered by the Google Gemini API, users can chat with an intelligent AI doctor to get preliminary medical advice, clarify symptoms, and receive health guidance.
*   📊 **Health Tracker (`HealthTrackerActivity`)**
    *   Monitor your vital signs including Blood Pressure, Blood Sugar, and Heart Rate.
    *   Visualize your health history through interactive graphs (powered by MPAndroidChart) to easily spot trends and abnormalities.
*   🚨 **Emergency SOS System (`SosActivity`)**
    *   In case of emergencies, users can instantly trigger an SOS alert that sends their real-time location to emergency contacts and logs the alert on the backend.
*   🚑 **Ambulance Request (`AmbulanceActivity`)**
    *   A dedicated module for requesting immediate ambulance assistance with integrated location tracking.
*   🩹 **First Aid Guidelines (`FirstAidActivity`)**
    *   Instant access to crucial first-aid information and instructions during critical moments.
*   📄 **Medical Report Viewer (`PdfPreviewActivity`)**
    *   Built-in PDF viewer for users to preview their medical reports seamlessly within the app.

## 🛠️ Technology Stack
*   **Language:** Kotlin
*   **Architecture:** MVC/MVVM patterns
*   **Networking:** Retrofit2 & OkHttp (Interacts with a dedicated Node.js backend)
*   **AI Integration:** Google Gemini API
*   **UI Components:** Material Design 3, MPAndroidChart (for data visualization)
*   **Concurrency:** Kotlin Coroutines

---
*Note: This repository only contains the Android frontend. The backend server (Node.js/MySQL) is hosted and managed separately.*