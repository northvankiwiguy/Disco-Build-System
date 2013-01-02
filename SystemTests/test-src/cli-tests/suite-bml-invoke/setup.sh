#!/bin/bash -e

#
# This shell script sets up the initial test environment for all the tests in the test-*
# sub-directories. It is executed once per test-* directory.
#

cp $TEST_SRC/../build.bml .
bmladmin upgrade



