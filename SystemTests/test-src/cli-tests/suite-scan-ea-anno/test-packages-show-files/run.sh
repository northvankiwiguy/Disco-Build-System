#!/bin/bash -e

#
# Test assigning packages to files, after using a scan-ea-anno command to build a build.bml file.
#

echo "Show all files, with only default packages"
bmladmin show-files -p | fgrep /home/psmith/t

echo
echo "Add a package to the zlib files, and show all files again"
bmladmin set-workspace-root /
bmladmin add-pkg zlib
bmladmin set-file-pkg zlib /home/psmith/t/cvs-1.11.23/zlib
bmladmin show-files -p | fgrep /home/psmith/t

echo
echo "Show only the files in the new package"
bmladmin show-files -p -f %p/zlib | fgrep /home/psmith/t

echo
echo "Show the files that are outside the new package"
bmladmin show-files -p -f %np/zlib | fgrep /home/psmith/t

echo
echo "Add a single file to zlib/public, and show it"
bmladmin set-file-pkg zlib/public /home/psmith/t/cvs-1.11.23/zlib/libz.a
bmladmin show-files -p -f %p/zlib/public | fgrep /home/psmith/t

echo
echo "Show only the private files in zlib"
bmladmin show-files -p -f %p/zlib/private | fgrep /home/psmith/t

echo
echo "Now show all the files in zlib"
bmladmin show-files -p -f %p/zlib | fgrep /home/psmith/t

echo
echo "Try to remove zlib (should fail)"
set +e
bmladmin rm-pkg zlib
echo "Status is $?"
set -e

echo
echo "Add a new package, and set all .o files to that package"
bmladmin add-pkg objects
bmladmin set-file-pkg objects "*.o"
bmladmin show-files -p | fgrep /home/psmith/t

echo
echo "Show the files that are still in zlib"
bmladmin show-files -p -f %pkg/zlib | fgrep /home/psmith/t

echo
echo "Create a source package, assign it to all .c and .h files, then show what's left"
bmladmin add-pkg source
bmladmin set-file-pkg source "*.c"
bmladmin set-file-pkg source "*.h"
bmladmin show-files -p -f %pkg/zlib | fgrep /home/psmith/t

echo
echo "Try to remove zlib (should fail)"
set +e
bmladmin rm-pkg zlib
echo "Status is $?"
set -e

echo
echo "Remove the final file from the zlib package, then try to delete the package"
bmladmin set-file-pkg "<import>/None" /home/psmith/t/cvs-1.11.23/zlib/libz.a
set +e
bmladmin rm-pkg zlib
echo "Status is $?"
set -e

echo
echo "Remove the zlib directory from the package too, then try to delete the package"
bmladmin set-file-pkg "<import>/None" /home/psmith/t/cvs-1.11.23/zlib
bmladmin rm-pkg zlib
