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
 # Si la variable d'environnement REDIS_PASSWORD est définie, on ajoute --requirepass dynamiquement.
 ENV REDIS_PASSWORD=${REDIS_PASSWORD}
 CMD ["sh","-lc","exec redis-server --appendonly yes ${REDIS_PASSWORD:+--requirepass \"$REDIS_PASSWORD\"}"]