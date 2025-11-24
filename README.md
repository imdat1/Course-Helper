BackEnd in Spring Boot, FrontEnd in React, Python LangChain Agentic Chatbot

Just put Google Studio API key in application-properties in SpringBoot BackEnd app and '.env' in Python service.

Start Python Service with "docker compose up"

Make Config for BackEnd [text](backend-service/src/main/java/com/example/demo/DemoApplication.java) & start

Start FrontEnd service with npm start

Best current dev setup to start the project is with Mise, but the commands are made specifically for Windows right now. Install Mise from:

https://mise.jdx.dev/installing-mise.html

First trust mise tools:

```powershell
mise trust
```

Then install tools:

```powershell
mise install
```

Then check for installed tools:

```powershell
mise ls
```

Then run mise tasks to start python service, backend, and then frontend:

```powershell
mise run
```
Please keep in mind for local development, the upload Word/DOCX files for the Python service
on Windows will not work, only through a dockerized environment or on Linux. You'll also need
to install tesseract, poppler, and libgl libraries stated in the README of the Python service. 

