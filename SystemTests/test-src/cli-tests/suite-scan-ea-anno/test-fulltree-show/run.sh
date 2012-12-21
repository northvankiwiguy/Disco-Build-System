#!/bin/bash -e

#
# Test commands that show summaries of the whole set of files.
#

echo
echo "Show write-only files"
bml show-write-only-files

echo
echo "Show write-only .Po files"
bml show-write-only-files -f "*.Po"

echo
echo "Show all files by their popularity"
bml show-popular-files

echo
echo "Show .h files by their popularity"
bml show-popular-files -f "*.h"

echo
echo "Show .h files by their popularity, with packages"
bml set-workspace-root /home/psmith/t/cvs-1.11.23
bml add-pkg src_files
bml set-file-pkg src_files/private "*.c"
bml set-file-pkg src_files/public "*.h"
bml show-popular-files -f "*.h" -p

echo
echo "Show all files by popularity, with packages"
bml show-popular-files -p

echo
echo "Note: with the sample emake.xml file we have, all files are used. We therefore can't test show-unused-files."


