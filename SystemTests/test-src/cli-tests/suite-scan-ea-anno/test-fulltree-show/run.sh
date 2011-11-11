#!/bin/bash -e

#
# Test commands that show summaries of the whole set of files.
#

echo
echo "Show write-only files"
disco show-write-only-files

echo
echo "Show write-only .Po files"
disco show-write-only-files -f "*.Po"

echo
echo "Show all files by their popularity"
disco show-popular-files

echo
echo "Show .h files by their popularity"
disco show-popular-files -f "*.h"

echo
echo "Note: with the sample emake.xml file we have, all files are used. We therefore can't test show-unused-files."


