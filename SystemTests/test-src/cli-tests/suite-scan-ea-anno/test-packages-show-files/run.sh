#!/bin/bash -e

#
# Test assigning packages to files, after using a scan-ea-anno command to build a buildstore.bml file.
#

echo "Show all files, with only default packages"
bml show-files -p

echo
echo "Add a package to the zlib files, and show all files again"
bml add-pkg zlib
bml set-file-pkg zlib /home/psmith/t/cvs-1.11.23/zlib
bml show-files -p

echo
echo "Show only the files in the new package"
bml show-files -p -f %p/zlib

echo
echo "Show the files that are outside the new package"
bml show-files -p -f %np/zlib

echo
echo "Add a single file to zlib/public, and show it"
bml set-file-pkg zlib/public /home/psmith/t/cvs-1.11.23/zlib/libz.a
bml show-files -p -f %p/zlib/public

echo
echo "Show only the private files in zlib"
bml show-files -p -f %p/zlib/private

echo
echo "Now show all the files in zlib"
bml show-files -p -f %p/zlib

echo
echo "Try to remove zlib (should fail)"
set +e
bml rm-pkg zlib
echo "Status is $?"
set -e

echo
echo "Add a new package, and set all .o files to that package"
bml add-pkg objects
bml set-file-pkg objects "*.o"
bml show-files -p

echo
echo "Show the files that are still in zlib"
bml show-files -p -f %pkg/zlib

echo
echo "Create a source package, assign it to all .c and .h files, then show what's left"
bml add-pkg source
bml set-file-pkg source "*.c"
bml set-file-pkg source "*.h"
bml show-files -p -f %pkg/zlib

echo
echo "Try to remove zlib (should fail)"
set +e
bml rm-pkg zlib
echo "Status is $?"
set -e

echo
echo "Remove the final file from the zlib package, then try to delete the package"
bml set-file-pkg None/None /home/psmith/t/cvs-1.11.23/zlib/libz.a
set +e
bml rm-pkg zlib
echo "Status is $?"
set -e

echo
echo "Remove the zlib directory from the package too, then try to delete the package"
bml set-file-pkg None/None /home/psmith/t/cvs-1.11.23/zlib
bml rm-pkg zlib





