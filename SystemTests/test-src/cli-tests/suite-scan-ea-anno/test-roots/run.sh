#!/bin/bash -e

#
# Test commands that use file roots.
#

# Note that @workspace will default to a native workspace path, which
# could be different on each native file system.
echo
echo "Show all files, with roots displayed"
bml show-files -r | fgrep -v @workspace

echo
echo "Set the @workspace root appropriately"
bml set-workspace-root /home/psmith/t/cvs-1.11.23/

echo
echo "Add a new package zlib, show the list of roots"
bml add-pkg zlib
bml show-root

echo
echo "Set the zlib_src root"
bml set-pkg-root zlib_src /home/psmith/t/cvs-1.11.23/zlib
bml show-root

echo
echo "Show just zlib_src root by itself"
bml show-root zlib_src

echo
echo "Show an invalid root name (should fail)"
set +e
bml show-root bad_root
echo Status is $?
set -e

echo
echo "Remove the zlib package, show the list"
bml rm-pkg zlib
bml show-root

echo
echo "Add the zlib back again, show the list"
bml add-pkg zlib
bml show-root

echo
echo "Show only the files within zlib_src"
bml show-files -r -f @zlib_src

echo
echo "Show only the files within zlib_gen"
bml show-files -r -f @zlib_gen

echo
echo "Rename the zlib package to compress"
bml set-pkg-root zlib_src /home/psmith/t/cvs-1.11.23/zlib
bml rename-pkg zlib compress
bml show-root

