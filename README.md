# Campus AI Chatbot

A beginner-friendly chatbot with a plain HTML/CSS/JavaScript interface, a Java Spring Boot REST API, and Google's Gemini API. The browser talks only to this app; the Gemini API key stays on the server.

## Requirements

- Java 17 or newer (this workspace was checked with Java 25)
- Apache Maven 3.6.3 or newer
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)

Maven was not installed or available on PATH when this project was created. A Maven 3.9.11 distribution and dependency cache are available in this workspace under the ignored `.tools/` and `.maven-cache/` folders, so you can build without installing Maven system-wide.

## Configure the Gemini API key

In the VS Code PowerShell terminal, set the key for the current terminal session. Replace the example text with your own key; do not include angle brackets:

```powershell
$env:GEMINI_API_KEY = "your-real-key-here"
```

This does not persist the key system-wide. Start the app from that same terminal. Never paste your key into frontend files or commit it. `.gitignore` excludes `.env` files if you later choose to use them.

Optional settings:

```powershell
$env:GEMINI_MODEL = "gemini-2.5-flash"
$env:CHATBOT_SYSTEM_INSTRUCTION = "You are a patient tutor. Explain concepts with simple examples."
```

The default model is `gemini-2.5-flash`. Change `GEMINI_MODEL` if you want to use another model currently enabled for your Google AI Studio project.

## Build, test, and run

From the project root in the VS Code terminal, use the workspace-local Maven:

```powershell
.\.tools\apache-maven-3.9.11\bin\mvn.cmd "-Dmaven.repo.local=.maven-cache" test
.\.tools\apache-maven-3.9.11\bin\mvn.cmd "-Dmaven.repo.local=.maven-cache" spring-boot:run
```

Open **http://localhost:8080** in your browser. To stop the server, focus the terminal running it and press **Ctrl+C**.

If Maven is installed on your PATH, the shorter commands `mvn test` and `mvn spring-boot:run` work as well.

The app can start without an API key so the UI and backend are available, but sending a message will return a clear setup error until `GEMINI_API_KEY` is configured. Gemini calls need an internet connection.

### Troubleshooting Gemini errors

- **Could not authenticate the API key (401):** create or copy a valid key from Google AI Studio, set it as `$env:GEMINI_API_KEY` in the same terminal used to start the app, then restart the app. Do not include extra spaces or quote characters inside the value.
- **Access denied (403):** check that the key is allowed to use the Gemini API and that API/key restrictions permit this app.
- **Model not found (404):** check `GEMINI_MODEL`; the default is `gemini-2.5-flash`.
- The API key is sent to Gemini in the request header, not in the URL or frontend.

## Project structure

```text
campus-ai-chatbot/
├── .gitignore
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/edu/campus/chatbot/
    │   │   ├── CampusAiChatbotApplication.java
    │   │   ├── api/
    │   │   │   ├── ChatController.java
    │   │   │   ├── ChatExceptionHandler.java
    │   │   │   └── dto/
    │   │   │       ├── ChatMessage.java
    │   │   │       ├── ChatRequest.java
    │   │   │       └── ChatResponse.java
    │   │   └── service/
    │   │       ├── AiMessage.java
    │   │       ├── AiProvider.java
    │   │       ├── ChatException.java
    │   │       ├── ChatService.java
    │   │       └── GeminiAiProvider.java
    │   └── resources/
    │       ├── application.properties
    │       └── static/
    │           ├── app.js
    │           ├── index.html
    │           └── styles.css
    └── test/java/edu/campus/chatbot/service/
        └── ChatServiceTest.java
```

## How a message travels

1. `app.js` adds the message to the selected local chat and sends it with recent messages to `POST /api/chat`.
2. `ChatController` receives the JSON request. `ChatService` validates it, converts recent assistant messages to the AI provider's `model` role, and applies the general or college-assistant instructions.
3. `GeminiAiProvider` reads `GEMINI_API_KEY` from the server environment and sends the prompt/context to Gemini. The key is never sent to the browser.
4. Spring Boot returns `{ "answer": "..." }`. The browser renders the answer with escaped, lightweight Markdown-style formatting and saves the chat in local storage.

The app keeps up to 20 recent messages as model context. Chat history in the browser is local to that browser profile; it is not uploaded as a separate history store.

## Important files and methods for your viva

- `CampusAiChatbotApplication.java` — Spring Boot entry point; `main` starts the web server.
- `ChatController.java` — maps `POST /api/chat` to the Java service.
- `ChatService.java` — `chat` validates input, prepares recent conversation context, and selects the college-assistant instruction.
- `AiProvider.java` — small interface that keeps the chatbot independent of the Gemini implementation.
- `GeminiAiProvider.java` — `generate` builds the Gemini request, calls the API, and extracts the generated text.
- `ChatExceptionHandler.java` — returns safe, friendly JSON errors instead of exposing backend exception details.
- `application.properties` — port, model, and configurable system instruction; secrets come from environment variables.
- `index.html` — page structure and accessible controls.
- `styles.css` — responsive layout, colors, and light/dark themes.
- `app.js` — UI events, local chat storage, REST request, loading/error states, and safe Markdown rendering.
- `ChatServiceTest.java` — verifies prompt/context forwarding and empty-message validation without needing a Gemini key.

## Current limits and possible improvements

- College mode gives general guidance only. It does not know private college information unless the user supplies it.
- Add citations from approved college documents, streaming responses, conversation export, richer Markdown, or a database if the project later needs shared accounts or server-side history.
- Add authentication and rate limiting before exposing the app publicly. Do not deploy a personal API key in client-side code.
