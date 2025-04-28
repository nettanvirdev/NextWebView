#!/bin/bash

# Accept Android SDK licenses
mkdir -p "$ANDROID_HOME/licenses"
echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" > "$ANDROID_HOME/licenses/android-sdk-license"
echo "d56f5187479451eabf01fb78af6dfcb131a6481e" >> "$ANDROID_HOME/licenses/android-sdk-license"

# Install missing components if needed
echo "Preparing JitPack environment completed"
