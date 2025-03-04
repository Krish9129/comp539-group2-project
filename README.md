# URL Shortener Service - Group2

## Project Overview

This project is a highly scalable and distributed **URL Shortener Service** deployed on **Google Cloud Platform (GCP)**. The service efficiently generates and resolves short URLs while ensuring high availability, low latency, and optimized caching. The system is designed to handle thousands of requests seamlessly.

## Team Members & Roles

| Role                      | Team Member(s)                |
| ------------------------- | ----------------------------- |
| **Team Leader**           | Zilong Xue                    |
| **Backend Developers**    | Krishna Esanakula, Zilong Xue |
| **Frontend Developers**   | Haotian Zheng, Shen Gao       |
| **Tester & Tech Support** | Sid Wang                      |
| **Deployer**              | Tong Wu                       |
| **Documentation**         | All Members                   |

## System Architecture

<img src="architecture.png" alt="System Architecture" width="800">
Our URL Shortener service follows a **distributed microservices architecture** with the following components:

1. **Frontend (Web Application UI)**: Built by frontend developers, allowing users to enter URLs for shortening and retrieving analytics.
2. **API Gateway & Load Balancer**: Handles incoming requests, routes them efficiently to backend services, and ensures system scalability.
3. **Application Service (Backend Logic)**:
   - Processes URL shortening and resolution requests.
   - Includes the **URL Cleanup Service**, which periodically removes expired links.
4. **Database Storage (GCP Bigtable)**: Optimized for fast read/write operations, efficiently mapping short URLs to long URLs.
5. **Cache Servers**: Store frequently accessed URLs to enhance performance since read operations outnumber writes.
6. **Logging & Monitoring**: Uses **Cloud Logging and Monitoring** to track system health and performance metrics.

## Tech Stack

- **Frontend**: React.js (UI components), CSS, JavaScript
- **Backend**: Java with Spring Boot
- **Database**: Google Cloud Bigtable (NoSQL)
- **Caching**: Redis (for frequently accessed URLs)
- **Deployment & Cloud Services**: Google Cloud Platform (GCP) - Cloud Run, Cloud Functions, Cloud Load Balancing
- **Monitoring & Logging**: Google Cloud Logging & Monitoring
- **Testing**: Unit tests, Postman, Jmeter

## Setup and Running Instructions

### Prerequisites

- Java 17 or higher
- Node.js 14 or higher and npm
- Git
- Access to GCP (for production deployment)
- Google Cloud SDK (for deployment)

### Backend Setup

1. **Clone the repository**:

   ```bash
   git clone https://github.com/ZilongXue/comp539-group2-project.git
   cd urlshortener-backend
   ```

2. **Configure Google Cloud credentials**:

   - Place your Google Cloud service account key file (`team2-service-account-key.json`) in the `src/main/resources` directory
   - Ensure the service account has appropriate permissions for Bigtable

3. **Configure application.properties**:

   - Review and update `src/main/resources/application.properties` if needed
   - Update the following properties if necessary:
     ```properties
     # OAuth2 Google configuration
     spring.security.oauth2.client.registration.google.client-id=your-google-client-id
     spring.security.oauth2.client.registration.google.client-secret=your-google-client-secret
     spring.security.oauth2.client.registration.google.redirect-uri=your-redirect-uri
     ```

4. **Build the project**:

   ```bash
   ./mvnw clean package
   ```

5. **Run the backend server**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend server will start on http://localhost:8080

### Frontend Setup

1. **Navigate to the frontend directory**:

   ```bash
   cd ../url-shortener-frontend
   ```

2. **Install dependencies**:

   ```bash
   npm install
   ```

3. **Start the development server**:
   ```bash
   npm start
   ```
   The frontend will be available at http://localhost:3000

## Deploy Instructions

### Prerequisites

- Access to GCP (for production deployment)
- Google Cloud SDK (for deployment)

Run the following commands to set up your GCP environment:

1. **Authenticate with Google Cloud**:

   ```bash
   gcloud auth login
   ```

2. **Set the target GCP project**:
   ```bash
   gcloud config set project [YOUR_PROJECT_ID]
   ```

### Backend Deployment

1. **Configure app.yaml**

   - Review and update `app.yaml` if needed
   - Update the following properties if necessary:
     ```properties
     service_account: your-service-account
     env_variables:
       GOOGLE_APPLICATION_CREDENTIALS: your-service-account-key.json
     ```

2. **Package the application using Maven**:

   - This will generate `target/urlshortener-backend-0.0.1-SNAPSHOT.jar`

3. **Deploy the backend server**:
   ```bash
   cd url-shortner-backend
   gcloud app deploy
   ```

### Frontend Deployment

1. **Build the project**:

   ```bash
   npm run build
   ```

2. **Deploy the frontend server**:
   ```bash
   cd url-shortner-frontend
   gcloud app deploy
   ```

## Timelines & Milestones

| Date Range      | Milestones                                              |
| --------------- | ------------------------------------------------------- |
| Jan 31 – Feb 20 | ✅ Set up infrastructure & core functionality           |
| Feb 21 – Mar 20 | 🔗 Add project features                                 |
| Mar 21 – Apr 10 | 📊 Implement Custom Alias, User Account System, Testing |
| Apr 11 – Apr 15 | 🚀 Performance Tuning, Security Enhancements            |
| Apr 11 – Apr 15 | 🏁 Deployment & Maintenance                             |

## Contributions

- Fork the repository, create a feature branch, and submit a PR.
- Follow the project coding standards and guidelines.
