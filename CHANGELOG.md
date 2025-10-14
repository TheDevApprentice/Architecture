# Changelog

All notable changes to the Cloud-Ready DevOps Platform project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.0.0] - 2025-09-26 to 2025-10-13

### 🔬 Development Phase & Exploration

#### Infrastructure Exploration
- Initial project structure setup
- Docker Compose multi-file architecture design
- Network segmentation planning
- Environment variable management

#### Database Cluster Research
- MariaDB Galera cluster implementation and testing
- PostgreSQL Patroni cluster with Etcd setup
- Load balancer configurations (ProxySQL, MaxScale, HAProxy)
- Failover and recovery testing

#### Keycloak Development
- Realm configuration and testing
- User/group/role management
- Client setup for OIDC flows
- Theme customization and branding
- Localization implementation

#### Jenkins Integration
- OIDC authentication setup
- JCasC configuration development
- Pipeline development for user management
- Shared library creation
- Service account testing

#### Observability Setup
- Prometheus/Grafana stack deployment
- Loki/Promtail log aggregation
- Dashboard creation and testing
- Exporter configuration

---

## v0.1.0 (2025-10-14)
 
### 🎉 First dev stable testable configuration

First dev stable testable configuration of the Cloud-Ready DevOps Platform with most complete Traefik Reverse Proxy + Keycloak IAM solution + Jenkins CI/CD pipeline + MinIO Object Storage + Redis Cache + MariaDB Galera Cluster + PostgreSQL Patroni Cluster.

### ✨ Added

#### Infrastructure Foundation
- **Traefik Reverse Proxy** - Automatic service discovery and routing
- **Docker Network Segmentation** - Isolated networks (proxy, dbnet, cachenet, stornet, keycloaknet)
- **Environment Configuration** - Centralized `.env` configuration management
- **Docker Compose Architecture** - Modular multi-file compose structure

#### Security & Authentication
- **Keycloak IAM Platform** (v26.x)
  - Multi-realm architecture (master, internal)
  - Pre-configured realms with users, groups, and roles
  - Custom login themes with responsive design
  - French/English localization (i18n)
  - OIDC/OAuth2 authentication flows
  - Service account for machine-to-machine authentication
  - Client configurations (jenkins, jenkins-automation)
  - Group-based authorization (IT, Jenkins groups)
  - Password policies and brute force protection
  - Audit logging and event tracking

#### Custom Themes
- **Master Realm Theme** - Custom branding for admin console
- **Internal Realm Theme** - Modern responsive login page
- **Jenkins Client Theme** - Two-column layout with gradient branding
- **Localization Files** - French translations for all themes
- **Responsive Design** - Mobile-first approach with breakpoints

#### Database Infrastructure
- **MariaDB Galera Cluster** (v11.x)
  - 3-node cluster configuration
  - ProxySQL for load balancing
  - MaxScale for advanced routing
  - Automatic failover and recovery
  - Auto-bootstrapping capability

- **PostgreSQL Patroni Cluster** (v16.x)
  - 3-node cluster with Patroni
  - Etcd for distributed consensus (3 nodes)
  - HAProxy for load balancing
  - Automatic leader election
  - Dedicated PostgreSQL for Keycloak

#### Storage & Cache
- **MinIO Object Storage**
  - S3-compatible API
  - Traefik routing integration
  - Basic configuration for development

- **Redis Cache**
  - In-memory data store
  - Internal network configuration
  - Ready for session management

#### Observability Stack
- **Prometheus** - Metrics collection and storage
- **Grafana** - Visualization dashboards
- **Loki** - Log aggregation
- **Promtail** - Log collection agent
- **Tempo** - Distributed tracing
- **OpenTelemetry Collector** - Instrumentation
- **Example Dashboards** - Pre-configured monitoring examples

#### CI/CD (Example Implementation)
- **Jenkins** (v2.x LTS)
  - Configuration as Code (JCasC)
  - OIDC authentication via Keycloak
  - Automated plugin installation
  - Pre-configured pipelines for Keycloak user management
  - Shared Groovy libraries for reusability
  - Service account integration examples
  - Automatic job creation via init scripts

#### Documentation
- **README.md** - Complete project overview
- **LICENSE** - Non-commercial license with commercial approval requirement
- **DOCS/ARCHITECTURE.md** - Infrastructure architecture diagrams
- **DOCS/KEYCLOAK&JENKINS_ARCHITECTURE_OVERVIEW.md** - Integration patterns
- **Component Documentation** - Individual docs for Traefik, Keycloak, Jenkins, Databases
- **Deployment Guides** - Quick start and installation instructions

### 🔧 Changed
- Migrated from MIT License to Non-Commercial License with Commercial Approval Requirement
- Reorganized documentation structure
- Improved README with clearer sections and status indicators

### 📝 Notes

#### Design Decisions
- **Dedicated PostgreSQL for Keycloak**: Chose dedicated instance over cluster for simplicity and maintainability
- **Galera & Patroni Available**: Both database cluster configurations remain available for services requiring HA
- **Jenkins as Example**: Included as practical demonstration of Keycloak integration patterns

#### Known Limitations
- MinIO requires additional configuration for production use
- Redis needs clustering setup for HA
- Observability stack needs custom dashboard development
- Homepage dashboard requires configuration
- Turn server planned but not implemented

#### Migration from V0
This is the first tagged release. Previous development was exploratory and included:
- Database cluster experimentation (Galera, Patroni)
- Keycloak configuration iterations
- Theme development and localization
- Jenkins integration development

---

## Future Releases (to be redefined if needed)

### Planned for v0.2.0
- [ ] Complete Jenkins CI/CD pipeline usage for Keycloak
- [ ] Complete Jenkins CI/CD pipeline integration Tests for Keycloak

### Planned for v0.3.0
- [ ] Complete MinIO integration with applications
- [ ] Complete Jenkins CI/CD pipeline integration for MinIO

### Planned for v0.4.0
- [ ] Redis cluster configuration for HA

### Planned for v0.5.0
- [ ] Homepage dashboard configuration

### Planned for v0.6.0
- [ ] Enhanced observability dashboards

### Planned for v0.7.0
- [ ] Automated backup solutions

### Planned for v0.8.0
- [ ] Disaster recovery procedures

### Planned for v0.9.0
- [ ] Automated testing suite

### Planned for v1.0.0
- [ ] Production-ready configurations
- [ ] Complete documentation

### Planned for v2.0.0
- [ ] Kubernetes migration path
- [ ] Multi-environment support (dev/staging/prod)
- [ ] Performance optimization
- [ ] Complete documentation up-to-date

---