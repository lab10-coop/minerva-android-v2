FROM openjdk:8-jdk
USER root

COPY . .

#Install updates
RUN apt-get --quiet update --yes; \
    apt-get install -y --no-install-recommends apt-utils; \
    apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1;

#Get gradle dist into container
RUN GRADLE_FILE_NAME=gradle-5.4.1-all.zip; \
    GRADLE_LINK="https://services.gradle.org/distributions/$GRADLE_FILE_NAME"; \
    GRADLE_DIST_DIR="/gradleDist"; \
    GRADLE_FILE_PATH="$GRADLE_DIST_DIR/$GRADLE_FILE_NAME"; \
    mkdir -p $GRADLE_DIST_DIR; \
    wget --quiet $GRADLE_LINK -O $GRADLE_FILE_PATH;

#Get android SDK
RUN ANDROID_COMPILE_SDK=28; \
    ANDROID_BUILD_TOOLS=28.0.2; \
    ANDROID_SDK_TOOLS=4333796; \
    ANDROID_LINK="https://dl.google.com/android/repository/sdk-tools-linux-$ANDROID_SDK_TOOLS.zip"; \
    wget --quiet --output-document=android-sdk.zip $ANDROID_LINK; \
    unzip -d /android-sdk-linux android-sdk.zip; \
    echo y | /android-sdk-linux/tools/bin/sdkmanager "platform-tools" >/dev/null; \
    echo y | /android-sdk-linux/tools/bin/sdkmanager "build-tools;$ANDROID_BUILD_TOOLS" >/dev/null; \
    echo "export ANDROID_HOME=/android-sdk-linux" >> ~/.bashrc; \
    PATH=$PATH:/android-sdk-linux/platform-tools/; \
    echo "export PATH=$PATH" >> ~/.bashrc; \
    rm -rf android-sdk.zip .idea .gradle; \
    chmod +x ./gradlew;

VOLUME /var/app
WORKDIR /var/app

CMD ["bash", "-c", "./gradlew", "assembleDebug"]
