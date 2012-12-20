#!/bin/bash -e

#
# Test the "show-files" output, after using a scan-ea-anno command to build a build.bml file.
#

echo "show-files - with default settings"
bml show-files | fgrep -v SystemTests

echo
echo "show-files with a single path-spec to select .c files"
bml show-files -f "*.c" | fgrep -v SystemTests

echo
echo "show-files with path-specs to select .c and .h files"
bml show-files -f "*.c:*.h" | fgrep -v SystemTests

echo
echo "show-files selecting a single subdirectory"
bml show-files --filter /home/psmith/t/cvs-1.11.23/zlib | fgrep -v SystemTests

echo
echo "show-files selecting two subdirectories"
bml show-files --filter /home/psmith/t/cvs-1.11.23/zlib:/home/psmith/t/cvs-1.11.23/vms | fgrep -v SystemTests

echo
echo "show-files selecting a subdirectory and a pattern"
bml show-files --filter "/home/psmith/t/cvs-1.11.23/zlib:*.o" | fgrep -v SystemTests

echo
echo "show-files selecting a specific file name"
bml show-files --filter "wait.h" | fgrep -v SystemTests

