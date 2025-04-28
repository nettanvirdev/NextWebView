#!/bin/bash

# Make the script executable
chmod +x gradlew

# Accept Android SDK licenses
yes | sdkmanager --licenses
