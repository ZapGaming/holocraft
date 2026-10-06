#!/bin/sh
# Third-party files bundled in the release, from their official sources.
set -e
mkdir -p "$(dirname "$0")/../vendor" && cd "$(dirname "$0")/../vendor"
curl -fsSL -o PrismLauncher-Windows-MinGW-w64-Portable-11.1.1.zip https://github.com/PrismLauncher/PrismLauncher/releases/download/11.1.1/PrismLauncher-Windows-MinGW-w64-Portable-11.1.1.zip
curl -fsSL -o fabric-api-0.119.4+1.21.4.jar "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.119.4%2B1.21.4/fabric-api-0.119.4%2B1.21.4.jar"
curl -fsSL -o e4mc-fabric-6.2.3.jar https://cdn.modrinth.com/data/qANg5Jrr/versions/8wsUJ306/e4mc-fabric-6.2.3.jar
ls -la
