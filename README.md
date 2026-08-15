### Custom NAS Application

A custom Network Attached Storage (NAS) application developed with a Java Spring Boot backend, a React frontend, and a PostgreSQL database. 

### Key Features

* **File Management:** Secure file uploading, downloading, and storage management.
* **User Authentication:** Secure login and user access control via Spring Security.
* **REST API:** Modular backend services communicating with a responsive React UI.
* **Database Storage:** Relational data management using PostgreSQL for indexing files and user metadata.

### Architecture & Production Deployment

The application is fully dockerized and deployed in a production-ready home lab environment: 

* **Infrastructure:** Dedicated local server running Debian GNU/Linux.
* **Orchestration:** Managed via Docker Compose for easy service management and uptime stability.
* **Network Integration:** Operating seamlessly within a local area network (LAN) for private, high-speed cloud storage.

### Production Stack Configuration

To deploy or replicate this environment locally, use the provided docker-compose.yml file: 

```bash
docker compose up -d
```
