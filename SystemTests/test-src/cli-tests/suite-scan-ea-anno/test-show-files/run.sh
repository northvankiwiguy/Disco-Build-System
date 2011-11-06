#!/bin/bash -e

#
# Test the "show-files" output, after using a scan-ea-anno command to build a buildstore.disco file.
#

echo "show-files - with default settings"
disco show-files

echo
echo "show-files with a single path-spec to select .c files"
disco show-files -f "*.c"

echo
echo "show-files with path-specs to select .c and .h files"
disco show-files -f "*.c:*.h" 

echo
echo "show-files selecting a single subdirectory"
disco show-files --filter /home/psmith/t/cvs-1.11.23/zlib

echo
echo "show-files selecting two subdirectories"
disco show-files --filter /home/psmith/t/cvs-1.11.23/zlib:/home/psmith/t/cvs-1.11.23/vms

echo
echo "show-files selecting a subdirectory and a pattern"
disco show-files --filter "/home/psmith/t/cvs-1.11.23/zlib:*.o"

echo
echo "show-files selecting a specific file name"
disco show-files --filter "wait.h"

