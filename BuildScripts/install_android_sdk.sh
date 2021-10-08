#!/bin/bash

# Exit on any errors
set -e

# The script was written based on https://developer.android.com/studio "Command line tools only"

CURRENT_DIR=$(pwd)
ANDROID_BUILD_TOOLS=30.0.3
ANDROID_SDK_DIR=android-sdk-linux
SDKMANAGER=$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager 
ANDROID_LINK="https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip"
ANDROID_SDK_ROOT=$CURRENT_DIR/$ANDROID_SDK_DIR
PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools

# Download Android cmdline-tools
wget --output-document=android-sdk.zip $ANDROID_LINK

# Unpack Android cmdline-tools
mkdir $ANDROID_SDK_DIR
unzip -d $ANDROID_SDK_DIR android-sdk.zip

# Modify .bashrc 
echo "export ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT" >> ~/.bashrc
echo "export PATH=$PATH" >> ~/.bashrc
source /root/.bashrc

# https://stackoverflow.com/questions/65262340/cmdline-tools-could-not-determine-sdk-root
mv $ANDROID_SDK_DIR/cmdline-tools $ANDROID_SDK_DIR/latest
mkdir $ANDROID_SDK_DIR/cmdline-tools
mv $ANDROID_SDK_DIR/latest $ANDROID_SDK_DIR/cmdline-tools 

# Configure Android SDK 
yes | $SDKMANAGER "platform-tools"
yes | $SDKMANAGER "build-tools;$ANDROID_BUILD_TOOLS"
yes | $SDKMANAGER --licenses

# Docker image is based on alpine distribution which use ash as the default shell so
# we need to setup additional file with ANDROID_SDK_ROOT because .bashrc will not work in CI
# https://en.wikipedia.org/wiki/Almquist_shell
touch local.properties
echo "sdk.dir = $ANDROID_SDK_ROOT" > local.properties

# Remove zip
rm -rf android-sdk.zip
