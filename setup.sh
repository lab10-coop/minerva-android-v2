#!/bin/bash
# This script lays here as an imitaion of before_script
# of gitlab pipeline to be reasurred that local env is same as pipieline

export ANDROID_BUILD_TOOLS=28.0.2
export ANDROID_HOME=/android-sdk-linux

yes | /android-sdk-linux/tools/bin/sdkmanager "platform-tools" >/dev/null 2>&1 || true
yes | /android-sdk-linux/tools/bin/sdkmanager "build-tools;$ANDROID_BUILD_TOOLS" >/dev/null 2>&1 || true
yes | /android-sdk-linux/tools/bin/sdkmanager --licenses >/dev/null 2>&1 || true