#!/bin/bash

# Mise ?? jour des paquets
apt-get update

# Installer la version la plus r??cente d'OpenSSL disponible
echo "Installation de la version la plus r??cente d'OpenSSL"
apt-get install -y --no-install-recommends openssl

rm -rf /var/lib/apt/lists/*

# V??rification de la version
echo "OpenSSL version:"
openssl version

# Ex??cution de la commande originale
exec "$@"
