#!/bin/bash -e

#
# Test assigning components to files, after using a scan-ea-anno command to build a buildstore.bml file.
#

echo "Show all files, with only default components"
bml show-files -c

echo
echo "Add a component to the zlib files, and show all files again"
bml add-comp zlib
bml set-file-comp zlib /home/psmith/t/cvs-1.11.23/zlib
bml show-files -c

echo
echo "Show only the files in the new component"
bml show-files -c -f %c/zlib

echo
echo "Show the files that are outside the new component"
bml show-files -c -f %nc/zlib

echo
echo "Add a single file to zlib/public, and show it"
bml set-file-comp zlib/public /home/psmith/t/cvs-1.11.23/zlib/libz.a
bml show-files -c -f %c/zlib/public

echo
echo "Show only the private files in zlib"
bml show-files -c -f %c/zlib/private

echo
echo "Now show all the files in zlib"
bml show-files -c -f %c/zlib

echo
echo "Try to remove zlib (should fail)"
set +e
bml rm-comp zlib
echo "Status is $?"
set -e

echo
echo "Add a new component, and set all .o files to that component"
bml add-comp objects
bml set-file-comp objects "*.o"
bml show-files -c

echo
echo "Show the files that are still in zlib"
bml show-files -c -f %comp/zlib

echo
echo "Create a source component, assign it to all .c and .h files, then show what's left"
bml add-comp source
bml set-file-comp source "*.c"
bml set-file-comp source "*.h"
bml show-files -c -f %comp/zlib

echo
echo "Try to remove zlib (should fail)"
set +e
bml rm-comp zlib
echo "Status is $?"
set -e

echo
echo "Remove the final file from the zlib component, then try to delete the component"
bml set-file-comp None/None /home/psmith/t/cvs-1.11.23/zlib/libz.a
set +e
bml rm-comp zlib
echo "Status is $?"
set -e

echo
echo "Remove the zlib directory from the component too, then try to delete the component"
bml set-file-comp None/None /home/psmith/t/cvs-1.11.23/zlib
bml rm-comp zlib





