#!/bin/sh

# This script reproduces a representative figure.
# It installs OpenJDK JDK 21 (assuming an OS like Ubuntu 20.04), builds the project, and runs the algorithm on an example input.

sudo apt install openjdk-21-jdk
./gradlew jvmRun -DmainClass=GenerateExampleKt
