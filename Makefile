#--------------------------------------------------------------------------
#   Copyright 2011 Taro L. Saito
# 
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#--------------------------------------------------------------------------*/
#--------------------------------------
# Genome Weaver Project
#
# Makefile
# Since: 2011/02/04
#
#--------------------------------------

PREFIX:=${HOME}/local
JVM_OPT:=
SBT:=bin/sbt 
INSTALL:=install

.PHONY: compile test package dist idea debug

all: dist

doc:
	$(SBT) doc

compile:
	$(SBT) compile

test:
	$(SBT) test -Dloglevel=debug

package:
	$(SBT) package

# This file will be generated after 'make dist'
VERSION_FILE:=target/dist/VERSION

dist: $(VERSION_FILE)

SRC:=$(shell find . \( -name "*.scala" -or -name "*.java" \))
$(VERSION_FILE): $(SRC)
	$(SBT) package-dist

PROG:=genome-weaver
# Use '=' to load the current version number when VERSION is referenced
VERSION=$(shell cat $(VERSION_FILE))
GW_BASE_DIR=$(PREFIX)/$(PROG)
GW_DIR=$(GW_BASE_DIR)/$(PROG)-$(VERSION)
install: $(VERSION_FILE)
	if [ -d "$(GW_DIR)" ]; then rm -rf "$(GW_DIR)"; fi
	$(INSTALL) -d "$(GW_DIR)"
	chmod 755 target/dist/bin/$(PROG)
	cp -r target/dist/* $(GW_DIR)
	ln -sfn "genome-weaver-$(VERSION)" "$(GW_BASE_DIR)/current"
	$(INSTALL) -d "$(PREFIX)/bin"
	ln -sf "../$(PROG)/current/bin/$(PROG)" "$(PREFIX)/bin/$(PROG)"


local:
	$(SBT) publish-local

# Create IntelliJ project files
idea:
	$(SBT) gen-idea

clean:
	$(SBT) clean

ifndef test
TESTCASE:=
else 
TESTCASE:="~test-only *$(test)"
endif

debug:
	$(SBT) -Dloglevel=debug $(TESTCASE)

trace:
	$(SBT) -Dloglevel=trace $(TESTCASE)

