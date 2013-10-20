#!/bin/bash -e

#
# Test commands that show summaries of the whole set of files.
#

echo
echo "Show write-only files"
bmladmin show-write-only-files

echo
echo "Show write-only .Po files"
bmladmin show-write-only-files -f "*.Po"

echo
echo "Show all files by their popularity"
bmladmin show-popular-files

echo
echo "Show .h files by their popularity"
bmladmin show-popular-files -f "*.h"

echo
echo "Show .h files by their popularity, with packages"
bmladmin set-workspace-root /
bmladmin add-pkg src_files
bmladmin set-file-pkg src_files/private "*.c"
bmladmin set-file-pkg src_files/public "*.h"
bmladmin show-popular-files -f "*.h" -p

echo
echo "Show all files by popularity, with packages"
bmladmin show-popular-files -p

echo
echo "Note: with the sample emake.xml file we have, all files are used. We therefore can't test show-unused-files."


