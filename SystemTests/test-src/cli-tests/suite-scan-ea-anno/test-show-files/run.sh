#!/bin/bash -e

#
# Test the "show-files" output, after using a scan-ea-anno command to build a build.bml file.
#

echo "show-files - with default settings"
bmladmin show-files | fgrep -v SystemTests

echo
echo "show-files with a single path-spec to select .c files"
bmladmin show-files -f "*.c" | fgrep -v SystemTests

echo
echo "show-files with path-specs to select .c and .h files"
bmladmin show-files -f "*.c:*.h" | fgrep -v SystemTests

echo
echo "show-files selecting a single subdirectory"
bmladmin show-files --filter /home/psmith/t/cvs-1.11.23/zlib | fgrep -v SystemTests

echo
echo "show-files selecting two subdirectories"
bmladmin show-files --filter /home/psmith/t/cvs-1.11.23/zlib:/home/psmith/t/cvs-1.11.23/vms | fgrep -v SystemTests

echo
echo "show-files selecting a subdirectory and a pattern"
bmladmin show-files --filter "/home/psmith/t/cvs-1.11.23/zlib:*.o" | fgrep -v SystemTests

echo
echo "show-files selecting a specific file name"
bmladmin show-files --filter "wait.h" | fgrep -v SystemTests

