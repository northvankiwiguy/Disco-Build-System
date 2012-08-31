#!/bin/bash -e

. $TEST_SRC/../functions.sh

# test that scan-build returns the exit code of the command.
bml scan-build true
echo Status is $?

set +e
# test with a non-zero exit code
bml scan-build false
echo Status is $?

# without -c, it's an illegal command.
bml scan-build "true && true"
echo Status is $?
set -e

# this will be passed through the shell
bml scan-build -c "true && true"
echo Status is $?

# so will this.
bml scan-build -c "false || true"
echo Status is $?
