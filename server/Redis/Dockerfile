 # server/Redis/Dockerfile
 # Image Redis légère pour dev/prod simple. Les données critiques applicatives ne doivent pas
 # reposer uniquement sur Redis. Ici, on active l'AOF pour une meilleure durabilité.
 
 FROM redis:7-alpine
 
 # Expose le port Redis par défaut
 EXPOSE 6379
 
 # Volume persistant pour les données Redis
 VOLUME ["/data"]
 
 # Démarrer Redis avec AOF activé (appendonly yes)
 # Vous pouvez ajuster la politique d'AOF selon vos besoins (appendfsync everysec par défaut)
 CMD ["redis-server", "--appendonly", "yes"]
 
 # Healthcheck basique: ping le serveur Redis
 HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
   CMD redis-cli ping | grep -q PONG || exit 1