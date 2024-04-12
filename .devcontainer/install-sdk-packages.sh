#!/bin/bash

# install required sdk packages
source "/usr/local/sdkman/bin/sdkman-init.sh"

sdk install maven 3.9.6

sdk install scala 2.13.10
sudo ln -s /usr/local/sdkman/candidates/scala/current/bin/scala /usr/local/bin/scala
