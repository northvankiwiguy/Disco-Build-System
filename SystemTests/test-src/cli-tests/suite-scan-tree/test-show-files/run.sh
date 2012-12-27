#!/bin/bash -e

#
# Test the "show-files" output, after using the scan-tree command to build a build.bml file.
# Note that we filter out some of the command output so that our expected results can be independent of
# directory in which the test cases are checked out.
#

root=`dirname $TEST_SRC`
FILTER="s#$root##g"

echo "Show all files in the build tree"
bmladmin show-files | fgrep SystemTests | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all files with suffix .c"
bmladmin show-files -f "*.c" | fgrep SystemTests | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all files with suffix .h"
bmladmin show-files -f "*.h" | fgrep SystemTests | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all files with name file3.*"
bmladmin show-files -f "file3.*" | fgrep SystemTests | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all the files in the dirA subdirectory"
bmladmin show-files -f "$root/build-tree/dirA" | fgrep SystemTests | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all the files in the dirD subdirectory"
bmladmin show-files -f "$root/build-tree/dirA/dirD" | fgrep SystemTests | sed $FILTER | fgrep -v SystemTests
