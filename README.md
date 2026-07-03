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

## 🔒 Setup & Configuration (For Developers)

To run this project locally, you must provide your own API keys and configure the backend connection. These are intentionally omitted from version control for security.

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/AdityaDive08/HealthFreak-Application.git
    ```
2.  **Configure `local.properties`**
    Navigate to the root directory of the Android project and open (or create) the `local.properties` file. Add the following lines:
    ```properties
    # Your Google Gemini API Key
    GEMINI_API_KEY=your_gemini_api_key_here

    # Your Local or Production Node.js Backend URL (must include trailing slash)
    BACKEND_BASE_URL=http://192.168.x.x:3000/
    ```
    *Note: The `local.properties` file is git-ignored to prevent leaking credentials.*
3.  **Build and Run**
    Sync the project with Gradle files and run it on an Android Emulator or physical device.

---
*Note: This repository only contains the Android frontend. The backend server (Node.js/MySQL) is hosted and managed separately.*