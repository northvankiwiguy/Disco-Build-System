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
echo "Show .h files by their popularity, with components"
bml add-comp src_files
bml set-file-comp src_files/private "*.c"
bml set-file-comp src_files/public "*.h"
bml show-popular-files -f "*.h" -c

echo
echo "Show all files by popularity, with components"
bml show-popular-files -c

echo
echo "Note: with the sample emake.xml file we have, all files are used. We therefore can't test show-unused-files."


