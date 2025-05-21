# KotlinTestGenAI

KotlinTestGenAI is an IntelliJ (JetBrains) plugin built in Kotlin that enables developers to generate JUnit5 unit tests for Kotlin functions using Claude (Anthropic LLM).

## Overview

When the user selects a Kotlin function in their IntelliJ IDEA and triggers the plugin action:
1.  The plugin extracts the source code of the selected function.
2.  It sends this code to a Python FastAPI backend.
3.  The backend constructs a prompt and queries the Anthropic Claude API.
4.  Claude generates the unit test code.
5.  The backend returns this generated test code to the plugin.
6.  The plugin inserts the test code into the appropriate test file in the user's project.

## 🧱 Key Components

### 🧩 IntelliJ Plugin (Kotlin)
-   **`src/main/kotlin/io/github/senang/kotlinunittestgeneration/actions/GenerateTestAction.kt`**: Handles the user action, PSI interaction, and coordinates with other services.
-   **`src/main/kotlin/io/github/senang/kotlinunittestgeneration/services/ClaudeService.kt`**: Manages HTTP communication with the FastAPI backend.
-   **`src/main/kotlin/io/github/senang/kotlinunittestgeneration/utils/PsiUtils.kt`**: Contains helper functions for working with IntelliJ's PSI (Program Structure Interface) to analyze and manipulate Kotlin code.
-   **`src/main/kotlin/io/github/senang/kotlinunittestgeneration/TestGenAIPlugin.kt`**: (Optional) Plugin lifecycle, initialization.
-   **`src/main/resources/META-INF/plugin.xml`**: The plugin descriptor file (ID: `io.github.senang.Kotlin-Unit-Test-Generation`).
-   **`build.gradle.kts`**: Gradle build script for the plugin (group ID: `io.github.senang`).

### 🐍 FastAPI Backend (Python)
-   **`backend/app.py`**: Defines the FastAPI server and the `/generate` endpoint.
-   **`backend/prompt_templates.py`**: Stores prompt templates for Claude.
-   **`backend/requirements.txt`**: Python dependencies.

## 🚀 Getting Started

### Prerequisites
-   IntelliJ IDEA (Community or Ultimate), version 2023.3 or newer.
-   Java JDK 17 or newer.
-   Python 3.8 or newer.
-   An Anthropic API Key (for Claude).

### 🛠️ Setup

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd KotlinTestGenAI
    ```

2.  **Configure Backend:**
    *   Navigate to the `backend` directory:
        ```bash
        cd backend
        ```
    *   Create a Python virtual environment and activate it:
        ```bash
        python3 -m venv venv
        source venv/bin/activate 
        # On Windows: venv\Scripts\activate
        ```
    *   Install dependencies:
        ```bash
        pip install -r requirements.txt
        ```
    *   Create a `.env` file in the `backend` directory (`backend/.env`) and add your Anthropic API key:
        ```env
        ANTHROPIC_API_KEY="sk-ant-your-api-key-here"
        ```

3.  **Configure IntelliJ Plugin:**
    *   Open the root `KotlinTestGenAI` project in IntelliJ IDEA.
    *   It should automatically recognize it as a Gradle project. Allow Gradle to sync and download dependencies.
    *   **Important**: If you changed the `group` or `plugin.xml` ID from the defaults (`io.github.senang`), ensure your package names in `src/main/kotlin/` reflect these changes. The current code uses `io.github.senang.kotlinunittestgeneration`.

### ▶️ Running the Plugin & Backend

1.  **Start the FastAPI Backend:**
    *   In your terminal, ensure you are in the `backend` directory and your virtual environment is active.
    *   Run the FastAPI server:
        ```bash
        uvicorn app:app --reload
        ```
    *   It should be running at `http://localhost:8000`.

2.  **Run the IntelliJ Plugin:**
    *   In IntelliJ IDEA, open the Gradle tool window (View -> Tool Windows -> Gradle).
    *   Find the `runIde` task under `Tasks -> intellij`.
    *   Double-click `runIde` to start a new IntelliJ IDEA instance with your plugin loaded.
    *   Alternatively, you can run it from the command line in the project root:
        ```bash
        ./gradlew runIde
        ```

### 🧪 Using the Plugin

1.  In the new IntelliJ IDEA instance (the one launched by `runIde`):
    *   Open any Kotlin project (or create a simple one with a Kotlin function).
    *   Write or open a Kotlin file with a function you want to test.
    *   Select the entire function code in the editor OR place your cursor within the function name.
    *   Right-click to open the context menu.
    *   You should see an action like "Generate Unit Tests with Claude (KotlinTestGenAI)". Click it.
    *   The plugin will communicate with your local backend, which will then call Claude.
    *   A notification will appear indicating success or failure.
    *   If successful, the generated test code will be created/inserted into a corresponding test file (e.g., `src/test/kotlin/.../MyClassTest.kt`).

## ⚙️ Development Notes

*   **Plugin ID:** `io.github.senang.Kotlin-Unit-Test-Generation`
*   **Group ID:** `io.github.senang`
*   **Main Kotlin Package:** `io.github.senang.kotlinunittestgeneration`
*   Ensure the backend is running at `http://localhost:8000` for the plugin to connect.
*   The plugin uses `java.net.http.HttpClient` for HTTP requests.
*   `kotlinx.serialization` is used for JSON parsing.

## 📝 TODO / Future Enhancements

*   [ ] More sophisticated PSI analysis to extract class/file context for better prompts.
*   [ ] User configuration for backend URL and API key (if not using .env for backend).
*   [ ] Option to choose different Claude models.
*   [ ] Better error handling and user feedback.
*   [ ] More robust test file finding/creation logic (e.g., handling different source set layouts).
*   [ ] Actual unit tests for the plugin itself.
*   [ ] Custom icon.

## 🤝 Contributing

Contributions are welcome! Please open an issue or submit a pull request.
