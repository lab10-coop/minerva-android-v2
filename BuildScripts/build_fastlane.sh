#!/bin/bash

# Exit on any errors
set -e

export RUBY_VERSION=2.6 

cat > ~/.rvmrc << __EOF__
rvm_install_on_use_flag=1
rvm_gemset_create_on_use_flag=1
rvm_quiet_curl_flag=1
__EOF__

echo bundler >> /usr/local/rvm/gemsets/global.gems;

sed -i '3i . /etc/profile.d/rvm.sh\n' ~/.profile;

source /etc/profile.d/rvm.sh;

# Install and use specific ruby version
rvm install $RUBY_VERSION
rvm use $RUBY_VERSION 

/usr/local/rvm/bin/rvm cleanup all;

sed -i 's/mesg.*$/tty -s \&\& mesg n || true/' ~/.profile

bundle install

fastlane env
