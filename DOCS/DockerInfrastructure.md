# Docker Infrastructure Documentation (Index)
 
 This page is the entry point to all infrastructure documentation for the Real‑Time Chat project. Use it to navigate to the official Dev/Prod architecture guides and the up‑to‑date diagrams.
 
 ## Quick Links
 
 - Dev Infrastructure (V4, official): [InfraDevV4.md](./V4/InfraDevV4.md)
 - Dev Infrastructure Diagram (Mermaid): [infraDevV4.mmd](./V4/infraDevV4.mmd)
 - Prod Infrastructure (V4, official): [InfraProdV4.md](./V4/InfraProdV4.md)
 - Prod Infrastructure Diagram (Mermaid): [infraProdV4.mmd](./V4/infraProdV4.mmd)
 
 ## What You’ll Find in Each Document
 
 - Dev Infrastructure V4 (`InfraDevV4.md`)
   - Purpose and rationale for each component: Traefik, Express/Socket.IO (scaled), Redis, MinIO, ProxySQL, MariaDB Galera (3 nodes), MaxScale, and the MariaDB Backup node.
   - Networks, request/data flow, scaling approach, environment variables, security considerations.
   - Operations guidance: bring‑up, scaling, monitoring, backups/restores, and troubleshooting tips.
   - A built‑in high‑level Mermaid diagram to visualize the stack.
 
 - Dev Infrastructure Diagram (`infraDevV4.mmd`)
   - A standalone Mermaid source of the development topology for editors and CI pages that render Mermaid.
   - Shows networks (`proxy`, `dbnet`, `cachenet`, `stornet`), key ports, and the relationships among services (Traefik, app replicas, Redis, MinIO, ProxySQL, Galera, MaxScale, Backup, TURN).
   - Use this when you need to quickly understand wiring or present the diagram independently.
 
 - Prod Infrastructure V4 (`InfraProdV4.md`)
   - Production‑focused version of the infrastructure guide.
   - Will mirror the Dev guide but emphasize hardening, HTTPS/ACME with Traefik, Redis and DB credential policies, ProxySQL/MaxScale production settings, TURN deployment on public networks, and backup/restore procedures with retention.
 
 ## When to Use Which Doc
 
 - Start here (this index) to choose the right guide.
 - Use `InfraDevV4.md` for local development and internal testing environments.
 - Use `InfraProdV4.md` for staging/production deployments and security‑hardened settings.
 - Use `infraDevV4.mmd` when you need the raw diagram to embed or modify.
 
 ## Related Project Docs
 
 - Root README: [README.md](../README.md) (project overview and quick start)
 - Server README: [server/Express/Readme.md](../server/Express/Readme.md) (server run, environment variables)
 - Server Architecture: [server/Express/ARCHITECTURE.md](../server/Express/ARCHITECTURE.md) (layers, flows, and persistence model)
 
 ## Contributing to Infra Docs
 
 - Keep Dev and Prod docs in sync when topology changes.
 - Update the Mermaid diagram (`infraDevV4.mmd`) whenever services, networks, or ports change.
 - Cross‑link new sections between `InfraDevV4.md`, `InfraProdV4.md`, and the root `README.md` where relevant.
 
 ---
 
 For questions or proposals, open an issue or PR. Ensure changes align with the latest Compose files:
 - `docker-compose.yml` (latest topology reference)
 - `docker-compose.devV4.yml` (development)
 - `docker-compose.prodV4.yml` (production)
 