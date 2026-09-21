# CPEN321_26W1_ProjectName

## Frontend Setup

*   **Set the Server Address:** Before building the frontend, you need to point the app to the backend server. Create or open the `local.properties` file in the root of the `Android` directory and add this exact line:
    `API_BASE_URL="http://<YOUR_BACKEND_PUBLIC_IP>:<PORT>"`

## Backend Setup

*   **Docker:** The backend runs in a container, so please ensure Docker and Docker Compose are installed and running on your machine.
*   **Google Cloud Firewall:** This backend is deployed on Google Cloud. If you are spinning up a new instance, make sure your GCP VPC firewall rules are configured to allow inbound HTTP/WS traffic on the specific port the Node.js server uses, otherwise GCP will block the connection.

## Additional Setup

*   **Deployment Scripts:** Navigate to the `scripts` directory to spin everything up. Run `.\scripts\run-backend.ps1` to start the Node.js server, and `.\scripts\run-frontend.ps1` to build and deploy the Android app.