#!/bin/bash

# Skrypt do uruchomienia Qdrant w Docker
# Przygotowany na podstawie: https://www.youtube.com/watch?v=vcZK6B61teY

set -e

if ! command -v docker &> /dev/null; then
   echo "Docker nie jest zainstalowany lub niedostępny"
   exit 1
fi

if ! docker info &> /dev/null; then
   echo "Docker daemon nie działa"
   exit 1
fi

STORAGE_DIR="$PWD/storage"
mkdir -p "$STORAGE_DIR"

echo "Uruchamianie Qdrant..."
echo "Storage directory: $STORAGE_DIR"

docker run -d \
   -p 6333:6333 \
   -v "$STORAGE_DIR:/qdrant/storage" \
   qdrant/qdrant

echo "Qdrant uruchomiony na porcie 6333"
echo "Web UI dostępne pod: http://localhost:6333/dashboard"