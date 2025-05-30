#!/bin/bash

# Skrypt do uruchomienia Neo4j w Docker

set -e

if ! command -v docker &> /dev/null; then
   echo "Docker nie jest zainstalowany lub niedostępny"
   exit 1
fi

if ! docker info &> /dev/null; then
   echo "Docker daemon nie działa"
   exit 1
fi

DATA_DIR="$PWD/data"

mkdir -p "$DATA_DIR"

echo "Uruchamianie Neo4j..."
echo "Data directory: $DATA_DIR"

docker run -d \
   -p 7474:7474 \
   -p 7687:7687 \
   -v "$DATA_DIR:/data" \
   -e NEO4J_AUTH=neo4j/password \
   neo4j:latest

echo "Neo4j uruchomiony:"
echo "Browser UI: http://localhost:7474"
echo "Bolt: bolt://localhost:7687"
echo "Login: neo4j / password"