#!/bin/bash -e

. $TEST_SRC/../functions.sh

# test that scan-build returns the exit code of the command.
bmladmin scan-build true
echo Status is $?

set +e
# test with a non-zero exit code
bmladmin scan-build false
echo Status is $?

# without -c, it's an illegal command.
bmladmin scan-build "true && true"
echo Status is $?
set -e

# this will be passed through the shell
bmladmin scan-build -c "true && true"
echo Status is $?

# so will this.
bmladmin scan-build -c "false || true"
echo Status is $?
