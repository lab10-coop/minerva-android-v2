# This docker image should be only used to build applications in CI!!!

FROM openjdk:8-jdk

WORKDIR /usr/src/builder

# Copy only necessary files
COPY BuildScripts/build_fastlane.sh ./
COPY BuildScripts/install_android_sdk.sh ./
COPY Gemfile ./
COPY Gemfile.lock ./

# Install Firebase CLI required by Fastlanes
RUN wget -qO /usr/local/bin/firebase https://firebase.tools/bin/linux/latest && \
    chmod +x /usr/local/bin/firebase

# Install RVM
# Import keys from gitlab snippet instead of flaky keyservers
RUN [ "/bin/bash", "-c", "gpg --import <(curl -s https://gitlab.binarapps.com/snippets/30/raw)" ]
RUN curl -L https://get.rvm.io | bash -s stable

# Install and configure Android SDK
RUN ./install_android_sdk.sh

# Configure RVM and fastlane
RUN ./build_fastlane.sh
