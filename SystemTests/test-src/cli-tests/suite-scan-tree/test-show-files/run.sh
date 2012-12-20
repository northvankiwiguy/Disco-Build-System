#!/bin/bash -e

#
# Test the "show-files" output, after using the scan-tree command to build a build.bml file.
# Note that we filter out some of the command output so that our expected results can be independent of
# directory in which the test cases are checked out.
#

root=`dirname $TEST_SRC`
FILTER="s#$root##g"

echo "Show all files in the build tree"
bml show-files | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all files with suffix .c"
bml show-files -f "*.c" | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all files with suffix .h"
bml show-files -f "*.h" | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all files with name file3.*"
bml show-files -f "file3.*" | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all the files in the dirA subdirectory"
bml show-files -f "$root/build-tree/dirA" | sed $FILTER | fgrep -v SystemTests

echo
echo "Show all the files in the dirD subdirectory"
bml show-files -f "$root/build-tree/dirA/dirD" | sed $FILTER | fgrep -v SystemTests
