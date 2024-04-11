#!/bin/bash

# install_sdk_package () {
#    sdk install $1 $2
#    sudo ln -s "/usr/local/sdkman/candidates/$1/current/bin/$1" "/usr/local/bin/$1"
# }

source "/usr/local/sdkman/bin/sdkman-init.sh"

sdk install maven 3.9.6

sdk install scala 2.13.10
sudo ln -s /usr/local/sdkman/candidates/scala/current/bin/scala /usr/local/bin/scala
