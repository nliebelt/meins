# OS detection adapted from: https://gist.github.com/sighingnow/deee806603ec9274fd47
OSFLAG 	:=
JLINK := $(shell command -v jlink 2> /dev/null)
LEIN := $(shell command -v lein 2> /dev/null)
SHADOW := $(shell command -v shadow-cljs 2> /dev/null)

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
	OSFLAG := -l
endif
ifeq ($(UNAME_S),Darwin)
	OSFLAG := -m
endif
ifeq ($(UNAME_S),CYGWIN_NT-10.0)
	OSFLAG := -w
endif
ifeq ($(UNAME_S),MSYS_NT-10.0)
	OSFLAG := -w
endif

package: install package-only

deps-mac:
	npm install -g electron
	npm install -g electron-builder
	npm install -g electron-cli
	npm install -g electron-build-env
	npm install -g node-gyp
	npm install -g yarn
	npm install -g webpack
	npm install -g shadow-cljs
	mkdir ./bin

deps-ubuntu:
	sudo apt-get update
	sudo apt-get install python2.7
	sudo apt-get install make
	sudo apt-get install g++
	sudo apt-get install icnsutils
	sudo apt-get install graphicsmagick
	sudo apt-get install libx11-dev
	sudo apt-get install libxkbfile-dev
	sudo apt-get install libgconf-2-4
	npm install -g electron
	npm install -g electron-builder@20.38.5
	npm install -g electron-cli
	npm install -g electron-build-env
	npm install -g node-gyp
	npm install -g yarn
	npm install -g webpack
	npm install -g shadow-cljs
	mkdir ./bin

clean:
	@echo Cleaning up...
	@rm -rf ./bin
	@rm -rf ./dist
	@lein clean
	@rm -f ./yarn.lock

deps: clean
	@echo Fetching Leiningen dependencies...
	@lein deps

npm-deps: clean
	@echo Fetching NPM dependencies...
	@nvm use 12
	@yarn install
	@npm install -g electron-builder
	@electron-rebuild -v 6.0.3 -w keytar

test: deps
	@echo Running Clojure tests...
	@lein test

cljs-shared-tests: npm-deps
	@echo Running ClojureScript tests...
	@shadow-cljs compile shared-tests
	@node out/shared-tests.js

sass:
	@echo Building CSS...
	@lein sass4clj once

cljs: deps npm-deps
	@echo Building ClojureScript for main electron process...
	@shadow-cljs release main
	@echo Building ClojureScript for electron renderer process...
	@shadow-cljs release renderer

figwheel:
	@lein cljs-figwheel

electron: clean deps test cljs-shared-tests sass cljs

directories:
	@echo Preparing target directories...
	@mkdir -p bin
	@chmod -R +w bin/
	@rm -rf ./dist

jlink: clean test directories
	@echo Assembling UberJAR...
	@lein jlink assemble

# replace symlinks, they lead to problems with electron-packager
# from: https://superuser.com/questions/303559/replace-symbolic-links-with-files
symlinks:
	@echo Fixing symlinks...
	tar -hcf - target/jlink | tar xf - -C bin/
	rm -rf bin/jlink
	mv bin/target/jlink/ bin/
	chmod -R ugo+w bin/jlink/legal/

install: jlink electron symlinks

package-only:
	@echo Building executable...
	@electron-builder $(OSFLAG)

publish-github:
	@echo Publishing to GitHub Releases - requires GH_TOKEN in ENV...
	@electron-builder -c electron-builder.yml --publish always $(OSFLAG)

release: install publish-github
