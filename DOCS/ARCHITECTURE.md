 # 🏗️ Infrastructure Architecture

> **Version:** 0.1.0  
> **Last Updated:** 2025-10-14  
> **Deployment:** Local or Cloud VPS

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Deployment Options](#-deployment-options)
- [Network Architecture](#-network-architecture)
- [Service Layers](#-service-layers)
- [Component Details](#-component-details)
- [Data Flow](#-data-flow)
- [High Availability Options](#-high-availability-options)

---

## 🎯 Overview

This platform implements a **modular, cloud-agnostic infrastructure** designed to run on any VPS (Virtual Private Server) - whether hosted in the cloud (AWS, GCP, Azure, OVH, etc.) or locally on a physical server.

### Key Architectural Principles

1. **Container-Based** - All services run in Docker containers
2. **Network Segmentation** - Isolated networks for security and performance
3. **Modular Design** - Services are independent and can be deployed separately
4. **Optional HA** - Database clusters available but not required
5. **Cloud Agnostic** - Runs anywhere Docker is supported

---

## 🌐 Network Architecture

### Network Topology

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HOST OS                                     │
│                    (Linux / Windows / macOS)                        │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                        Docker Engine
                             │
        ┌────────────────────┴────────────────────┐
        │         Docker Bridge Networks          │
        └────────────────────┬────────────────────┘
                             │
     ┌───────────────────────┼───────────────────────┐
     │                       │                       │
     ▼                       ▼                       ▼
┌──────────┐         ┌──────────┐           ┌──────────┐
│  proxy   │         │  dbnet   │           │ stornet  │
│172.30.0/24│         │172.31.10/24│         │172.31.30/24│
└──────────┘         └──────────┘           └──────────┘
     │                       │                       │
     ▼                       ▼                       ▼
┌──────────┐         ┌──────────┐           ┌──────────┐
│keycloaknet│        │ cachenet │           │  [...]   │
│172.31.40/24│        │172.31.20/24│         │          │
└──────────┘         └──────────┘           └──────────┘
```

### Network Segmentation Details

| Network         | Subnet           | Purpose                    | Connected Services                                    |
|-----------------|------------------|----------------------------|-------------------------------------------------------|
| **proxy**       | `172.30.0.0/24`  | External-facing services   | Traefik, Keycloak, Jenkins, Grafana, MinIO UI         |
| **dbnet**       | `172.31.10.0/24` | Database clusters          | MariaDB Galera, PostgreSQL Patroni, ProxySQL, HAProxy |
| **cachenet**    | `172.31.20.0/24` | Caching layer              | Redis (standalone or cluster)                         |
| **stornet**     | `172.31.30.0/24` | Object storage             | MinIO (standalone or distributed)                     |
| **keycloaknet** | `172.31.40.0/24` | Authentication services    | Keycloak + dedicated PostgreSQL                       |

### Security Benefits
- **Isolation**: Services on different networks cannot communicate unless explicitly connected
- **Segmentation**: Database traffic isolated from public-facing services
- **Defense in Depth**: Multiple network layers provide security boundaries
- **Flexibility**: Services can join multiple networks as needed

---

## 📦 Service Layers

### Layer 1: Gateway & Routing

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                🌐 INTERNET / LOCAL NETWORK                      
│                                                                 │
└────────────────────────────┬────────────────────────────────────┘
                             │
                      HTTP/HTTPS (80/443)
                             │
                             ▼
        ┌────────────────────────────────────────────┐
        │          Traefik Reverse Proxy             │
        │                                            │
        │  • Automatic Service Discovery             │
        │  • Dynamic Routing (Labels)                │
        │  • SSL/TLS Termination                     │
        │  • Load Balancing                          │
        │  • Middleware (Auth, Rate Limit, etc.)     │
        └────────────────────┬───────────────────────┘
                             │
                    Routes to backend services
                             │
     ┌───────────────────────┼───────────────────────┐
     │                       │                       │
     ▼                       ▼                       ▼
```

**Key Points:**
- Single entry point for all HTTP/HTTPS traffic
- Handles routing to appropriate backend services
- Manages SSL certificates (Let's Encrypt support)
- Connected to `proxy` network

---

### Layer 2: Application Services

```
┌──────────────────────────────────────────────────┐
│                APPLICATION LAYER                 │
└──────────────────────────────────────────────────┘

┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Keycloak   │    │   Jenkins   │    │   Grafana   │
│    (SSO)    │    │   (CI/CD)   │    │ (Monitoring)│
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                  │                  │
       ├──────────────────┼──────────────────┤
       │                  │                  │

Connected to: proxy + keycloaknet | proxy + dbnet | proxy

Purpose:
• Keycloak:  Identity & Access Management (SSO)
• Jenkins:   CI/CD automation (optional example)
• Grafana:   Metrics visualization and dashboards
```

**Key Points:**
- Services exposed via Traefik (connected to `proxy` network)
- Each service connected to additional networks as needed
- Independent services - can be deployed separately
- Jenkins included as integration example, not required

---

### Layer 3: Data & Storage Services

```
┌────────────────────────────────────────────────────────────┐
│                  DATA & STORAGE LAYER                      │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│  📊 DATABASES (Optional HA Clusters)                       
│                                                            
│  ┌─────────────────────┐      ┌─────────────────────┐      │
│  │  MariaDB Galera     │      │ PostgreSQL Patroni  │      │
│  │                     │      │                     │      │
│  │  • 3-node cluster   │      │  • 3-node cluster   │      │
│  │  • ProxySQL LB      │      │  • Etcd consensus   │      │
│  │  • MaxScale router  │      │  • HAProxy LB       │      │
│  │                     │      │                     │      │
│  │  Network: dbnet     │      │  Network: dbnet     │      │
│  └─────────────────────┘      └─────────────────────┘      │
│                                                            │
│  Note: Available if needed, not required for basic setup   │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│  🗄️ DEDICATED DATABASES (Standalone)                       
│                                                            
│  ┌─────────────────────┐                                   
│  │  PostgreSQL         │   Used by: Keycloak              
│  │  (Keycloak)         │   Network: keycloaknet           
│  └─────────────────────┘                                   
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│  💾 OBJECT STORAGE                                         
│                                                            
│  ┌─────────────────────┐                                   
│  │      MinIO          │   S3-compatible storage           
│  │  (Standalone/HA)    │   Network: stornet + proxy        
│  └─────────────────────┘                                    
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│  ⚡ CACHE LAYER                                              
│                                                            
│  ┌─────────────────────┐                                   
│  │       Redis         │   In-memory cache                 
│  │  (Standalone/HA)    │   Network: cachenet               
│  └─────────────────────┘                                    
└────────────────────────────────────────────────────────────┘
```

**Key Points:**
- **Database Clusters**: Optional, available for applications requiring HA
- **Dedicated Databases**: Simpler setup, sufficient for most use cases
- **Storage & Cache**: Independent services, used by applications as needed
- **Network Isolation**: Each service type on dedicated network

---

### Layer 4: Observability & Monitoring

```
┌─────────────────────────────────────────────────┐
│             OBSERVABILITY STACK                 │
└─────────────────────────────────────────────────┘

  Metrics                Logs                Traces
     │                    │                    │
     ▼                    ▼                    ▼
┌───────────┐      ┌───────────┐      ┌───────────┐
│Prometheus │      │   Loki    │      │   Tempo   │
│           │      │           │      │           │
│  + Node   │      │ + Promtail│      │           │
│  Exporter │      │           │      │           │
└─────┬─────┘      └─────┬─────┘      └─────┬─────┘
      │                  │                  │
      └──────────────────┼──────────────────┘
                         │
                         ▼
                  ┌─────────────┐
                  │   Grafana   │  ← Visualization
                  └─────────────┘

Network: proxy (Grafana), internal monitoring network
Purpose: Monitor infrastructure health, logs, and traces
```

**Key Points:**
- Observability services monitor all other services
- Grafana provides unified dashboard interface
- Optional component - can be disabled in minimal deployments
- Network: Connected to multiple networks for monitoring

---

## 🔧 Component Details

### Core Required Services

#### Traefik (Reverse Proxy)
- **Purpose**: Gateway for all HTTP/HTTPS traffic
- **Required**: ✅ Yes
- **Networks**: `proxy`
- **Depends On**: None
- **Deployment**: Single container, lightweight

#### Keycloak (IAM)
- **Purpose**: Authentication and authorization (SSO)
- **Required**: ⚠️ Optional (but recommended)
- **Networks**: `proxy`, `keycloaknet`
- **Depends On**: PostgreSQL (dedicated instance)
- **Deployment**: Single container + database

#### PostgreSQL (Keycloak DB)
- **Purpose**: Dedicated database for Keycloak
- **Required**: ✅ If using Keycloak
- **Networks**: `keycloaknet`
- **Depends On**: None
- **Deployment**: Single container (not clustered)

---

### Optional Application Services

#### Jenkins (CI/CD)
- **Purpose**: CI/CD automation example
- **Required**: ❌ No (example implementation)
- **Networks**: `proxy`, optionally others
- **Depends On**: Keycloak (for auth), optional database
- **Deployment**: Single container

#### Grafana (Monitoring UI)
- **Purpose**: Visualization for observability data
- **Required**: ❌ No
- **Networks**: `proxy`
- **Depends On**: Prometheus, Loki, Tempo
- **Deployment**: Single container

---

### Optional HA Database Clusters

#### MariaDB Galera Cluster
- **Purpose**: High-availability relational database
- **Required**: ❌ No (available if needed)
- **Networks**: `dbnet`
- **Depends On**: None
- **Deployment**: 3 nodes + ProxySQL + MaxScale
- **Use Case**: Applications requiring MySQL/MariaDB with HA

#### PostgreSQL Patroni Cluster
- **Purpose**: High-availability PostgreSQL database
- **Required**: ❌ No (available if needed)
- **Networks**: `dbnet`
- **Depends On**: Etcd cluster (3 nodes)
- **Deployment**: 3 PostgreSQL nodes + 3 Etcd nodes + HAProxy
- **Use Case**: Applications requiring PostgreSQL with HA

**Important Notes:**
- These clusters are **standalone services**
- They are **not connected** to any specific application
- Applications can use them **if they need HA database**
- For simpler deployments, use single database instances

---

### Storage & Cache Services

#### MinIO (Object Storage)
- **Purpose**: S3-compatible object storage
- **Required**: ❌ No
- **Networks**: `stornet`, `proxy` (for UI)
- **Depends On**: None
- **Deployment**: Single node or distributed (4+ nodes)
- **Use Case**: Store files, images, backups, artifacts

#### Redis (Cache)
- **Purpose**: In-memory data store and cache
- **Required**: ❌ No
- **Networks**: `cachenet`
- **Depends On**: None
- **Deployment**: Single node or cluster (3+ nodes)
- **Use Case**: Session storage, caching, pub/sub messaging

---

### Observability Services

#### Prometheus + Node Exporter
- **Purpose**: Metrics collection and storage
- **Required**: ❌ No
- **Networks**: Internal monitoring + `proxy`
- **Depends On**: None
- **Deployment**: Single container each

#### Loki + Promtail
- **Purpose**: Log aggregation and collection
- **Required**: ❌ No
- **Networks**: Internal monitoring
- **Depends On**: None
- **Deployment**: Single container each

#### Tempo
- **Purpose**: Distributed tracing
- **Required**: ❌ No
- **Networks**: Internal monitoring
- **Depends On**: None
- **Deployment**: Single container

---

## 🔄 Data Flow

### User Authentication Flow (via Keycloak)

```
1. User → Traefik → Application
                      │
                      │ Needs auth?
                      ▼
2. Application → Keycloak (OIDC)
                      │
                      │ Validate
                      ▼
3. Keycloak → PostgreSQL (user data)
                      │
                      │ Return token
                      ▼
4. User ← Token ← Keycloak
                      │
                      │ Access with token
                      ▼
5. User → Application (authenticated)
```

### Application → Database Flow

```
Application Service
        │
        ├─ Option 1: Direct single DB
        │      │
        │      ▼
        │  Single PostgreSQL/MariaDB
        │
        ├─ Option 2: HA Cluster
        │      │
        │      ▼
        │  Load Balancer (HAProxy/ProxySQL)
        │      │
        │      ▼
        │  Database Cluster (3 nodes)
        │
        └─ Choose based on requirements
```

### Observability Data Flow

```
All Services
    │
    ├─ Metrics  → Prometheus → Grafana
    │
    ├─ Logs     → Promtail → Loki → Grafana
    │
    └─ Traces   → Tempo → Grafana
```

---

## 🏆 High Availability Options

### Minimal Setup (Single VPS)
```
✅ Traefik (single)
✅ Keycloak (single)
✅ PostgreSQL (single, for Keycloak)
✅ Application services (single)

Resources: 4 vCPU, 8GB RAM, 50GB disk
Downtime: Service restart required for maintenance
```

### Standard Setup (Single VPS with redundancy)
```
✅ Traefik (single)
✅ Keycloak (single)
✅ PostgreSQL (single, for Keycloak)
✅ Application services (single)
✅ Redis (single)
✅ MinIO (single)
✅ Observability stack

Resources: 8 vCPU, 16GB RAM, 100GB disk
Downtime: Service restart required for maintenance
```

### High Availability Setup (Multi-VPS)
```
✅ Traefik (load balanced across VPS)
✅ Keycloak (clustered)
✅ PostgreSQL Patroni Cluster (3 nodes across VPS)
✅ MariaDB Galera Cluster (3 nodes across VPS)
✅ Application services (scaled)
✅ Redis Cluster (3+ nodes)
✅ MinIO Distributed (4+ nodes)
✅ Observability stack (redundant)

Resources: 3+ VPS, 8 vCPU each, 16GB RAM each, 100GB+ disk each
Downtime: Near-zero (rolling updates, automatic failover)
```

---

## 📊 Resource Requirements

### Minimal Development (Single VPS)
- **vCPU**: 4 cores
- **RAM**: 8GB
- **Storage**: 50GB SSD
- **Network**: 100 Mbps
- **Services**: Traefik, Keycloak, 1-2 applications

### Standard Production (Single VPS)
- **vCPU**: 8 cores
- **RAM**: 16GB
- **Storage**: 100GB SSD
- **Network**: 1 Gbps
- **Services**: All core services + observability

### Enterprise HA (Multi-VPS)
- **VPS Count**: 3-5 nodes
- **vCPU**: 8-16 cores per VPS
- **RAM**: 32GB per VPS
- **Storage**: 200GB+ SSD per VPS
- **Network**: 1 Gbps+ with low latency
- **Services**: Full stack with HA clusters

---

## 🎯 Deployment Strategies

### Strategy 1: Incremental
```
Phase 1: Core infrastructure
  → Traefik + networks

Phase 2: Authentication
  → Keycloak + PostgreSQL

Phase 3: Applications
  → Add applications as needed

Phase 4: Data services
  → Add databases, storage, cache as required

Phase 5: Observability
  → Add monitoring stack
```

### Strategy 2: All-in-One
```
Deploy complete stack at once using docker-compose files:
  → 11-docker-compose.Infra.dev.yml (base)
  → 15-docker-compose.Infra.dev.security.yml (Keycloak)
  → 16-docker-compose.Infra.dev.cicd.yml (Jenkins)
  → [...other services...]
```

---

## 🔐 Security Considerations

### Network Security
- **Segmentation**: Services isolated by network
- **Firewall**: Only necessary ports exposed
- **TLS/SSL**: All external traffic encrypted (via Traefik)

### Application Security
- **SSO**: Centralized authentication via Keycloak
- **RBAC**: Role-based access control
- **Secrets**: Managed via environment variables or Docker secrets

### Database Security
- **Internal Networks**: Databases not exposed externally
- **Authentication**: Password-protected access
- **Encryption**: Data at rest and in transit (in production)

---

## 📈 Scalability Path

### Vertical Scaling (Single VPS)
```
1. Start: 4 vCPU, 8GB RAM
2. Scale: 8 vCPU, 16GB RAM
3. Max: 16 vCPU, 32GB RAM
```

### Horizontal Scaling (Multi-VPS)
```
1. Deploy database clusters (Galera, Patroni)
2. Add application nodes
3. Use external load balancer (or Traefik cluster)
4. Scale storage (MinIO distributed)
5. Scale cache (Redis cluster)
```

---

## 🎓 Summary

This infrastructure is designed to be:

1. **Flexible**: Deploy what you need, when you need it
2. **Scalable**: Start small, scale to multi-node clusters
3. **Cloud Agnostic**: Works on any VPS or local server
4. **Modular**: Services are independent and optional
5. **Production Ready**: HA options available when needed

The **database clusters** (Galera, Patroni) are **standalone services** that applications can use if they require high availability. They are not tied to any specific application and remain available for any service that needs them.

---

**Next Steps:**
- Review [Component Documentation](../README.md#-documentation)
- Check [Deployment Guide](../README.md#-quick-start)
- Explore [Keycloak Integration](KEYCLOAK&JENKINS_ARCHITECTURE_OVERVIEW.md)