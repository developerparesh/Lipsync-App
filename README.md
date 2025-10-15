# Lipsync-App

A cross-platform lipsync application that lets users select an image, record audio, and generate a lipsync video using [Wav2Lip](https://github.com/Rudrabha/Wav2Lip) on a Python backend.  
The Android client is built with Kotlin, MVVM architecture, and Google Material Design.

---

## 🖼️ Features

- **Android App**
  - Select or capture an image (face/photo)
  - Record audio
  - Upload image and audio to backend server
  - Receive generated lipsync video
  - Beautiful UI with Google Material themes
  - Modern MVVM architecture

- **Python Server**
  - Receives image and audio from client
  - Uses Wav2Lip for lipsync video generation
  - REST API for communication with Android app
  - Returns processed video to client

---

## 📲 Screenshots

<!-- Add your app screenshots here -->
<p align="center">
  <img src="assets/app_screenshot_1.png" width="250px"/>
  <img src="assets/app_screenshot_2.png" width="250px"/>
</p>

---

## 🚀 Tech Stack

- **Android:** Kotlin, MVVM, Google Material Design, Retrofit, CameraX, Jetpack Compose (optional)
- **Backend:** Python, Flask/FastAPI, Wav2Lip, REST API, Docker (optional)
- **Other:** FFmpeg, OpenCV (for video processing)

---

## 📦 Setup

### Android App

1. Clone the repo:
   ```bash
   git clone https://github.com/developerparesh/Lipsync-App.git
   ```
2. Open in Android Studio
3. Build and Run on device/emulator

### Python Server

1. Clone the repo and set up a virtual environment:
   ```bash
   git clone https://github.com/developerparesh/Lipsync-App-Backend.git
   cd Lipsync-App-Backend
   python -m venv venv
   source venv/bin/activate
   ```
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Download Wav2Lip model weights as per [Wav2Lip instructions](https://github.com/Rudrabha/Wav2Lip#download-pretrained-models)
4. Run the server:
   ```bash
   python app.py
   ```
5. The server will start at `http://localhost:5000`

---

## 🎬 How It Works

1. User selects or captures a face image in the app
2. Records an audio clip
3. App sends image and audio to Python backend via REST API
4. Backend runs Wav2Lip and returns the lipsync video
5. App displays/downloads the generated video

---

## 📝 Contributing

Contributions, issues, and feature requests are welcome!  
Feel free to [open an issue](https://github.com/developerparesh/Lipsync-App/issues) or submit a pull request.

---

## 📄 License

This project is [MIT licensed](LICENSE).

---

## 🙋‍♂️ Contact

- **Paresh (developerparesh)**
- [Instagram](https://instagram.com/yourusername)
- [LinkedIn](https://linkedin.com/in/yourusername)
- [Email](mailto:developerparesh.dev@gmail.com)

---

**Powered by [Wav2Lip](https://github.com/Rudrabha/Wav2Lip) and open source!**
