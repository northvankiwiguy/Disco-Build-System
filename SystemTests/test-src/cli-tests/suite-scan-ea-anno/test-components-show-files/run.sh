#!/bin/bash -e

#
# Test assigning components to files, after using a scan-ea-anno command to build a buildstore.disco file.
#

echo "Show all files, with only default components"
disco show-files -c

echo
echo "Add a component to the zlib files, and show all files again"
disco add-comp zlib
disco set-file-comp zlib /home/psmith/t/cvs-1.11.23/zlib
disco show-files -c

echo
echo "Show only the files in the new component"
disco show-files -c -f %c/zlib

echo
echo "Show the files that are outside the new component"
disco show-files -c -f %nc/zlib

echo
echo "Add a single file to zlib/public, and show it"
disco set-file-comp zlib/public /home/psmith/t/cvs-1.11.23/zlib/libz.a
disco show-files -c -f %c/zlib/public

echo
echo "Show only the private files in zlib"
disco show-files -c -f %c/zlib/private

echo
echo "Now show all the files in zlib"
disco show-files -c -f %c/zlib

echo
echo "Try to remove zlib (should fail)"
set +e
disco rm-comp zlib
echo "Status is $?"
set -e

echo
echo "Add a new component, and set all .o files to that component"
disco add-comp objects
disco set-file-comp objects "*.o"
disco show-files -c

echo
echo "Show the files that are still in zlib"
disco show-files -c -f %comp/zlib

echo
echo "Create a source component, assign it to all .c and .h files, then show what's left"
disco add-comp source
disco set-file-comp source "*.c"
disco set-file-comp source "*.h"
disco show-files -c -f %comp/zlib

echo
echo "Try to remove zlib (should fail)"
set +e
disco rm-comp zlib
echo "Status is $?"
set -e

echo
echo "Remove the final file from the zlib component, then try to delete the component"
disco set-file-comp None/None /home/psmith/t/cvs-1.11.23/zlib/libz.a
set +e
disco rm-comp zlib
echo "Status is $?"
set -e

echo
echo "Remove the zlib directory from the component too, then try to delete the component"
disco set-file-comp None/None /home/psmith/t/cvs-1.11.23/zlib
disco rm-comp zlib





