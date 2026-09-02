# 🌟 LORI - Complete Private AI Assistant Ecosystem Setup Guide

**Lori (लोरी)** is a private personal AI assistant ecosystem featuring bilingual voice conversation (Hindi, Hinglish, English), deep mobile command integration via official Android APIs, proactive lock-screen support, a secure Node.js/PostgreSQL backend with server-side Gemini AI protection, and a modern companion website.

---

## 📱 Section 1: Android Application Setup & Installation

### Step 1.1: Build & Install on Your Phone
1. **Open Project**: Open the repository root in Android Studio (Jellyfish / Koala or newer).
2. **Sync Gradle**: Let Gradle sync the dependencies (Jetpack Compose, Room, Moshi, Retrofit, Lottie).
3. **Build APK**:
   - In Android Studio menu: `Build` > `Build Bundle(s) / APK(s)` > `Build APK(s)`.
   - Or run in terminal: `gradle :app:assembleDebug`
4. **Install on Phone**:
   - Enable **Developer Options** and **USB Debugging** on your Android device.
   - Connect via USB and click **Run 'app'**, or transfer the generated `app-debug.apk` to your phone and install it.

---

### Step 1.2: Granting Required Android Permissions
When you launch Lori for the first time, grant the necessary permissions:
1. **Microphone (`RECORD_AUDIO`)**: Needed for real-time speech-to-text voice recognition and wake word detection.
2. **Notifications (`POST_NOTIFICATIONS`)**: Required to post background foreground status alerts and reminders.
3. **Notification Listener Service**:
   - Open Android **Settings** > **Apps** > **Special App Access** > **Device & App Notifications** (Notification Access).
   - Turn **ON** permission for **Lori**. (This enables Lori to read and announce incoming WhatsApp messages and notifications).
4. **Battery Optimization Exemption**:
   - Go to Android **Settings** > **Apps** > **Lori** > **App Battery Usage**.
   - Select **Unrestricted** (prevents Android OS from killing Lori's background foreground service when the phone is locked or screen is off).
5. **Set Lori as Default Digital Assistant (Optional)**:
   - Go to Android **Settings** > **Apps** > **Default Apps** > **Digital Assistant App**.
   - Select **Lori** (allows long-pressing the home button or power button to summon Lori instantly).

---

## 🔐 Section 2: Single-User Authentication & Security

Lori is designed strictly as a **Private Single-User Assistant** with zero public registration:
- On first launch, enter your **Master Phone Number** and set a secure password/PIN.
- All credentials and access tokens are encrypted with **AES-256-GCM** using the Android `MasterKey` and `EncryptedSharedPreferences`.
- Biometric quick unlock (Fingerprint / Face Unlock) can be enabled from the **Settings** or **Login Screen**.
- Consequential operations (such as sending messages or placing calls) request confirmation before execution.

---

## 🖥️ Section 3: Secure Backend Deployment (Docker / VPS)

The backend shields your AI API keys and stores conversation history in PostgreSQL.

### Step 3.1: Environment Configuration
Create a `.env` file in the root directory:

```env
PORT=3000
NODE_ENV=production
DB_HOST=lori-db
DB_PORT=5432
DB_USER=lori_admin
DB_PASSWORD=your_super_secure_db_password_here
DB_NAME=lori_assistant_db
JWT_SECRET=your_super_secret_jwt_key_9921
JWT_REFRESH_SECRET=your_super_secret_refresh_key_9922
GEMINI_API_KEY=your_gemini_api_key_from_google_ai_studio
GEMINI_MODEL=gemini-2.5-flash
```

### Step 3.2: Launch with Docker Compose
Run the following command on your server:

```bash
docker-compose up -d --build
```

- **Healthcheck**: Verify the backend is online at `http://your-server-ip:3000/health`.
- **Database**: PostgreSQL automatically executes `backend/schema.sql` on first boot.

### Step 3.3: First-Time Master Account Initialization
Run the initialization curl command once to create your master credentials:

```bash
curl -X POST http://localhost:3000/api/v1/auth/setup \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+919876543210",
    "password": "your_secure_password",
    "pin": "1234"
  }'
```
*Note: Public registration is permanently locked after this single-user creation.*

---

## 🌐 Section 4: Companion Website

The companion website (`/companion-web`) provides browser-based access:
1. Host using Nginx or serve static files:
   ```bash
   cd companion-web && python3 -m http.server 8080
   ```
2. Access at `http://localhost:8080`.
3. Log in using your master credentials.
4. Voice recognition works directly via browser Web Speech API, with live waveform animations and emergency kill switch telemetry.

---

## 🗣️ Section 5: Voice & Command Reference Guide

### 🎵 Music & Media
- *"Lori, YouTube kholo aur Arijit Singh ka gana chalao"*
- *"Lori, play lofi chill beats on YouTube"*
- *"Lori, koi tagda gym motivation song chalao"*

### 💬 WhatsApp & Communication
- *"Lori, Rahul ko WhatsApp message bhejo 'Kal 10 baje milte hain'"*
- *"Lori, last message ka reply kar do"* (Lori drafts the message and asks for voice confirmation: *"Kya main bhej doon?"*)

### 📞 Calls & Contacts
- *"Lori, Papa ko phone lagao"*
- Incoming call announcement: *"Aapko Amit ka phone aa raha hai. Uthaun ya cut karoon?"*
- Say: *"Utha lo"* (Accepts) or *"Cut kar do"* (Rejects).

### 🔍 Internet Search & Information
- *"Lori, aaj ka weather kaisa hai?"*
- *"Lori, IPL match ka score kya chal raha hai?"*
- *"Lori, mujhe batayein Artificial Intelligence kaise kaam karta hai?"*

### ⏰ Mobile Alarms, Timer & Utilities
- *"Lori, subah 6 baje ka alarm laga do"*
- *"Lori, flashlight on karo"* / *"Flashlight band karo"*
- *"Lori, camera kholo"*
- *"Lori, India Gate ka rasta dikhao"*

---

## 🛑 Section 6: Emergency Privacy & Kill Switch
- In the Android App: Tap the **Privacy Control** card on the Home screen or in Settings.
- Tap **STOP ALL ACTIVITY**: Immediately stops the microphone, cancels speech synthesis, kills the background foreground service, and clears active draft queues.
