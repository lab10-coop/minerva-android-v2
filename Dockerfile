FROM openjdk:11-jdk
USER root

COPY . .

# Install updates and required dependencies
RUN apt-get update --yes && apt-get install -y --no-install-recommends \
    apt-utils \
    wget \
    tar \
    unzip \
    lib32stdc++6 \
    lib32z1 \
    software-properties-common

# Install RVM
# import keys from gitlab snippet instead of flaky keyservers
RUN curl -s https://gitlab.binarapps.com/snippets/30/raw | gpg --import
RUN curl -L https://get.rvm.io | bash -s stable

# Install Firebase CLI
RUN wget -qO /usr/local/bin/firebase https://firebase.tools/bin/linux/latest && \
    chmod +x /usr/local/bin/firebase

# Download and install Android command line tools
RUN ANDROID_LINK="https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip" && \
    wget --quiet --output-document=commandlinetools.zip $ANDROID_LINK && \
    unzip -d /android-sdk-linux commandlinetools.zip && \
    rm -rf commandlinetools.zip

# Set environment variables
ENV ANDROID_COMPILE_SDK=31
ENV ANDROID_BUILD_TOOLS=30.0.3
ENV ANDROID_SDK_ROOT="/android-sdk-linux"
ENV PATH="$PATH:/android-sdk-linux/cmdline-tools/latest/bin"

# Install and configure Fastlane
RUN BuildScripts/build_fastlane.sh

VOLUME /var/app
WORKDIR /var/app

# Set the default command to run when starting the container
CMD ["bash", "-c", "./gradlew", "assembleDebug"]
