#!/usr/bin/env bash

sudo apt-get install openjdk-8-jre-headless
sudo apt-get install python2.7
sudo apt-get install make
sudo apt-get install g++
sudo apt-get install icnsutils
sudo apt-get install graphicsmagick
sudo apt-get install ruby-sass
sudo apt-get install libx11-dev
sudo apt-get install libxkbfile-dev

curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.8/install.sh | bash
source $HOME/.bashrc
nvm install 8.7

npm install -g electron
npm install -g electron-builder
npm install -g electron-cli
npm install -g electron-build-env
npm install -g electron-publisher-s3
npm install -g node-gyp
npm install -g yarn
npm install -g webpack

mkdir ./bin
cd ./bin/

wget https://cdn.azul.com/zulu/bin/zulu8.27.0.7-jdk8.0.162-linux_x64.tar.gz
tar -xzf zulu8.27.0.7-jdk8.0.162-linux_x64.tar.gz
rm zulu8.27.0.7-jdk8.0.162-linux_x64.tar.gz
mv zulu8.27.0.7-jdk8.0.162-linux_x64 zulu8-linux_x64

# fix for update fail issue
chmod -R -v u+w zulu8-linux_x64

cd ..
