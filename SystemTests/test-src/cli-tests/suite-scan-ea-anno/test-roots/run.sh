#!/bin/bash -e

#
# Test commands that use file roots.
#

# Note that @workspace will default to a native workspace path, which
# could be different on each native file system.
echo
echo "Show all files, with roots displayed"
bmladmin show-files -r | fgrep /home/psmith/t

echo
echo "Set the @workspace root appropriately"
bmladmin set-workspace-root /home/psmith/t/cvs-1.11.23/

echo
echo "Add a new package zlib, show the list of roots"
bmladmin add-pkg zlib
bmladmin show-root

echo
echo "Set the zlib_src root"
bmladmin set-pkg-root zlib_src /home/psmith/t/cvs-1.11.23/zlib
bmladmin show-root

echo
echo "Show just zlib_src root by itself"
bmladmin show-root zlib_src

echo
echo "Show an invalid root name (should fail)"
set +e
bmladmin show-root bad_root
echo Status is $?
set -e

echo
echo "Show only the files within zlib_src"
bmladmin show-files -r -f @zlib_src

echo
echo "Show only the files within zlib_gen"
bmladmin show-files -r -f @zlib_gen

echo
echo "Remove the zlib package, show the list"
bmladmin rm-pkg zlib
bmladmin show-root

echo
echo "Add the zlib back again, show the list"
bmladmin add-pkg zlib
bmladmin show-root

echo
echo "Rename the zlib package to compress"
bmladmin set-pkg-root zlib_src /home/psmith/t/cvs-1.11.23/zlib
bmladmin rename-pkg zlib compress
bmladmin show-root

