FROM openjdk:8-jdk
USER root

COPY . .

#Install updates
RUN apt-get --quiet update --yes; \
    apt-get install -y --no-install-recommends apt-utils; \
    apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1 software-properties-common; \
    apt-get update --yes;

# Install RVM
RUN gpg --keyserver hkp://pool.sks-keyservers.net --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3 7D2BAF1CF37B13E2069D6956105BD0E739499BDB; \
    \curl -L https://get.rvm.io | bash -s stable --ruby

# Install firebase
RUN wget -qO /usr/local/bin/firebase https://firebase.tools/bin/linux/latest; \
    chmod +x /usr/local/bin/firebase;

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
    PATH=$PATH:/usr/local/rvm/bin; \
    PATH=$PATH:/usr/local/rvm/gems/ruby-2.6.3/bin; \
    echo "export PATH=$PATH" >> ~/.bashrc; \
    rm -rf android-sdk.zip .idea .gradle; \
    chmod +x ./gradlew;

#Config RVM and fastlane
RUN BuildScripts/build_fastlane.sh

VOLUME /var/app
WORKDIR /var/app

CMD ["bash", "-c", "./gradlew", "assembleDebug"]
