#!/bin/bash -e

#
# This shell script removes the test environment (left-over files, etc) after
# running each of the tests in the test-* subdirectories. It is only executed
# on successful test cases, since for failed cases we probably want to 
# examine the files.
#

rm -f build.bml

