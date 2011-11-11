#!/bin/bash -e

#
# Test the "show-files" output, after using the scan-tree command to build a buildstore.disco file.
# Note that we filter out some of the command output so that our expected results can be independent of
# directory in which the test cases are checked out.
#

root=`dirname $TEST_SRC`
FILTER="s#$root##g"

echo "Show all files in the build tree"
disco show-files | sed $FILTER

echo
echo "Show all files with suffix .c"
disco show-files -f "*.c" | sed $FILTER

echo
echo "Show all files with suffix .h"
disco show-files -f "*.h" | sed $FILTER

echo
echo "Show all files with name file3.*"
disco show-files -f "file3.*" | sed $FILTER

echo
echo "Show all the files in the dirA subdirectory"
disco show-files -f "$root/build-tree/dirA" | sed $FILTER

echo
echo "Show all the files in the dirD subdirectory"
disco show-files -f "$root/build-tree/dirA/dirD" | sed $FILTER
