# Golf Concierge 🏌️‍♂️

Golf Concierge is an Android application designed to streamline the golf course experience. The app allows users to get personalized advice, check course conditions, and get real-time assistance using the power of generative AI.

---

## Key Features

* **AI-Powered Concierge**: Get instant, tailored advice on everything from golf rules to course strategy by interacting with a conversational AI.
* **Real-time Assistance**: Receive immediate help with any questions you have while on the course.
* **Seamless Integration**: The app provides a user-friendly interface that integrates complex AI technology into a simple and intuitive mobile experience.

---

## Technologies Used

This project is built using a modern Android development stack.

* **Kotlin**: The entire app is written in **Kotlin**, Google's preferred language for Android development, known for its conciseness and safety features.
* **Coroutines**: Asynchronous operations, particularly network calls to the AI model, are managed using **Kotlin Coroutines**. This ensures the app's UI remains responsive and doesn't freeze during long-running tasks.
* **Generative AI**: The core of the app's functionality is powered by the **Google GenAI SDK** (`com.google.genai:google-genai`), which provides a client for interacting with the Gemini API.
* **Android Jetpack**: The project uses various libraries from the **Android Jetpack** suite, including `androidx.core`, `androidx.appcompat`, and `androidx.constraintlayout` for building a robust and modern user interface.
* **Material Design**: The app's design follows the latest **Material Design** guidelines to ensure a clean, intuitive, and consistent user experience across different devices.

---

## How to Get Started

1.  **Clone the repository**:
    ```sh
    git clone git@github.com:niklasatframna/golf-concierge.git
    ```
2.  **Open in Android Studio**: Open the cloned project in Android Studio.
3.  **Set up the Gemini API Key**:
    * Create a `local.properties` file in your project's root directory.
    * Add your API key to this file in the format:
        ```properties
        GEMINI_API_KEY=YOUR_API_KEY_HERE
        ```
    * Your project is configured to use this key via `BuildConfig.GEMINI_API_KEY`.
4.  **Run the app**: Build and run the app on an Android emulator or a physical device.
