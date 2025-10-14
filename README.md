# 🏗️ Cloud-Ready DevOps Platform - Infrastructure as Code

> **Version:** 0.1.0
> **Status:** In Development
> **Ready for Production:** No
> **License:** MIT

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Project Goals](#-project-goals)
- [Key Features](#-key-features)
- [Quick Start](#-quick-start)
- [Documentation](#-documentation)
- [Roadmap](#-roadmap)

---

## 🎯 Overview

A complete, containerized, and highly available infrastructure platform designed to deploy robust web solutions on any VPS server. Built with Infrastructure as Code principles, this platform provides a reusable foundation for modern DevOps practices.

**Key Characteristics:**
- 🐳 **Fully Containerized** - Docker-based architecture
- 🔄 **Highly Available**    - Built-in HA for critical components
- 📦 **Modular Design**      - Compose-based separation of concerns
- 🚀 **Production Ready**    - Security, monitoring, and scalability included
- ♻️ **Reusable**            - Template for future projects

---

## 🎯 Project Goals

### Primary Objective
Create a **reusable cornerstone infrastructure** that enables rapid deployment of robust web solutions on any VPS server, with the ability to scale to cluster mode without refactoring.

### Core Principles
1. **Infrastructure as Code** - Everything defined in version control
2. **Cloud Agnostic**         - Works on AWS, GCP, Azure, or bare metal
3. **Developer Experience**   - Fast setup, clear documentation, easy maintenance
4. **Production Grade**       - Security, monitoring, and HA from day one
5. **Scalability**            - Seamless transition from single-node to cluster

## ✨ Key Features

---

### 🌐 Reverse Proxy & Routing

**Traefik:**
- Automatic service discovery
- Dynamic routing with labels
- SSL/TLS termination
- HTTP/2 and WebSocket support
- Middleware for authentication, rate limiting
- Dashboard for monitoring

**Documentation:** [`DOCS/Traefik/`](DOCS/Traefik/)

---

### 🔐 Security & Authentication

**Keycloak Identity & Access Management:**
- Single Sign-On (SSO) with OIDC/OAuth2
- Multi-realm architecture (master, internal)
- Custom themes with localization (EN/FR)
- Group-based authorization
- Service accounts for M2M authentication

**Documentation:** [`DOCS/Keycloak/`](DOCS/Keycloak/)

---

### 🔄 CI/CD Pipeline

**Jenkins Automation Server:**
- Configuration as Code (JCasC)
- Automated plugin installation
- OIDC authentication via Keycloak
- GitHub/SSH integrations
- Docker-in-Docker support

**Documentation:** [`DOCS/Jenkins/`](DOCS/Jenkins/)

---

### 🗄️ High Availability Databases

**MariaDB Cluster:**
- Galera 3-node cluster
- ProxySQL for load balancing
- MaxScale for advanced routing
- Automatic failover

**PostgreSQL Cluster:**
- Patroni 3-node cluster
- Etcd for distributed consensus
- HAProxy for load balancing
- Automatic leader election

**Documentation:** [`DOCS/Databases/`](DOCS/Databases/)

---

### 💾 Storage & Cache

**MinIO Object Storage:**
- S3-compatible API
- Distributed mode support
- Versioning and lifecycle policies

**Redis Cache:**
- In-memory data store
- Session management
- Distributed caching
- Pub/Sub messaging

---

### 📊 Observability Stack

**Monitoring & Alerting:**
- **Prometheus**                - Metrics collection and storage
- **Grafana**                   - Visualization and dashboards
- **Loki**                      - Log aggregation
- **Promtail**                  - Log collection
- **Tempo**                     - Distributed tracing
- **OpenTelemetry Collector**   - Standardized instrumentation

---

### 🛠️ Developer Experience

**Homepage Dashboard:**
- Centralized access to all services
- Service status monitoring
- Quick links and bookmarks

**Private Docker Registry:**
- Store custom images
- Web UI for image management
- Integration with CI/CD pipelines

---

## 🚀 Quick Start

### Prerequisites

- Docker Engine 24.0+
- Docker Compose v2.20+
- 4GB RAM minimum (8GB recommended)
- 20GB disk space

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/TheDevApprentice/Architecture.git
cd Architecture

# 2. Copy environment template
cp .env.example .env

# 3. Configure environment variables
nano .env

# 4. Start base infrastructure (Traefik + networks)
docker compose -f 11-docker-compose.Infra.dev.yml up -d

# 5. Start Keycloak (authentication)
docker compose -f 15-docker-compose.Infra.dev.security.yml up -d

# 6. Access services
# - Keycloak: http://auth.localhost
# - Traefik Dashboard: http://localhost:8080
```

### Verify Installation

```bash
# Check all containers are running
docker ps

# Check Keycloak is ready
curl http://auth.localhost/realms/internal/.well-known/openid-configuration

# View logs
docker compose -f 15-docker-compose.Infra.dev.security.yml logs -f
```

---

## 📚 Documentation

### Architecture & Design
- [Architecture Overview](DOCS/ARCHITECTURE.md)
- [Keycloak & Jenkins Architecture Overview](DOCS/KEYCLOAK&JENKINS_ARCHITECTURE_OVERVIEW.md)

### Component Documentation
- [Traefik Configuration](DOCS/Traefik/README.md)
- [Keycloak Setup](DOCS/Keycloak/README.md)
- [Jenkins Configuration](DOCS/Jenkins/README.md)
- [Database Clusters](DOCS/Databases/README.md)

---

## 🗺️ Roadmap

### ✅ Completed (v0.1.0)
- [x] Base infrastructure with Traefik
- [x] Network segmentation
- [x] Base Minio storage implementation with traefik routing
- [x] Base Redis implementation for internal use
- [x] Base MariaDB cluster
- [x] Base PostgreSQL cluster
- [x] Observability base stack setup with dashboard exemple
- [x] Keycloak SSO with 
-     Realms configuration
-     Clients configuration
-     Users configuration
-     Custom themes for realms and clients (Master, Internal & Jenkins client)
-     Service account examples
-     Internalization (EN/FR)
- [x] Jenkins CI/CD integration as example with Keycloak

### 🚧 In Progress
- [x] Jenkins CI/CD pipelines integration with available services
- [ ] MinIO storage integration

### 📋 Planned
- [ ] Database cluster configurations
- [ ] Automated backup solutions
- [ ] Disaster recovery procedures
- [ ] Kubernetes migration path
- [ ] Multi-environment support (dev/staging/prod)

---

## 💡 Use Cases

This infrastructure platform is ideal for:

- **SaaS Applications** - Multi-tenant web applications
- **Internal Tools** - Company dashboards and admin panels
- **Development Environments** - Consistent dev/staging/prod

---

## 📊 Project Status

| Component          | Use Case         | Status         | Version | Documentation                      |
|--------------------|------------------|----------------|---------|------------------------------------|
| Traefik            | Reverse Proxy    | ✅ Stable      | 3.x     | [Docs](DOCS/Traefik/)              |
| Keycloak           | SSO              | ✅ Stable      | 26.x    | [Docs](DOCS/Keycloak/)             |
| Jenkins            | CI/CD            | 🚧 In Progress | 2.x LTS | [Docs](DOCS/Jenkins/)              |
| MariaDB Galera     | Database         | ✅ Stable      | 11.x    | [Docs](DOCS/Databases/MariaDB/)    |
| PostgreSQL Patroni | Database         | ✅ Stable      | 16.x    | [Docs](DOCS/Databases/PostgreSQL/) |
| Redis              | Cache            | ✅ Need Work   | Latest  | -                                  |
| MinIO              | Storage          | ✅ Need Work   | Latest  | -                                  |
| Observability      | Observability    | 📋 Need Work   | -       | -                                  |
| Homepage           | -                | 📋 Need Work   | -       | -                                  |
| Turn               | -                | 📋 Planned     | -       | -                                  |

---

## 📄 License

This project is licensed under a **Non-Commercial License with Commercial Approval Requirement**.

- ✅ **Free for non-commercial use** (personal, educational)
- 📧 **Commercial use requires written approval** - Contact: menuwebservice@gmail.com

See the [LICENSE](LICENSE) file for complete terms and conditions.

---

## 👤 Author

**Hugo Abric (TheDevApprentice)**
- GitHub: [@TheDevApprentice](https://github.com/TheDevApprentice)
- Project: [Architecture](https://github.com/TheDevApprentice/Architecture)

---

## 📈 Impact & Value

✅ **Fast Deployment** - Infrastructure ready in < 10 minutes  
✅ **Cloud Agnostic** - Works on AWS, GCP, Azure, or bare metal  
✅ **Production Ready** - HA, security, and observability included  
✅ **Scalable** - Seamless single-node to cluster transition  
✅ **Reusable** - Template for all future web/SaaS projects  

---

**Built with ❤️ for the DevOps community**