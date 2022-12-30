FROM openjdk:11-jdk
USER root

COPY . .

# Install updates
RUN apt-get --quiet update --yes; \
    apt-get install -y --no-install-recommends apt-utils; \
    apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1 software-properties-common; \
    apt-get update --yes;

# Install RVM
# import keys from gitlab snippet instead of flaky keyservers
RUN [ "/bin/bash", "-c", "gpg --import <(curl -s https://gitlab.binarapps.com/snippets/30/raw)" ]
RUN curl -L https://get.rvm.io | bash -s stable

# Install firebase
RUN wget -qO /usr/local/bin/firebase https://firebase.tools/bin/linux/latest; \
    chmod +x /usr/local/bin/firebase;

# Get android SDK
RUN ANDROID_COMPILE_SDK=31; \
    ANDROID_BUILD_TOOLS=30.0.3; \
    ANDROID_SDK_TOOLS=7583922; \
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

# Config RVM and fastlane
RUN BuildScripts/build_fastlane.sh

VOLUME /var/app
WORKDIR /var/app

CMD ["bash", "-c", "./gradlew", "assembleDebug"]
