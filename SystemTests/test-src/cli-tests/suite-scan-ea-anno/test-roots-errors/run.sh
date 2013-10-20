#!/bin/bash -e

#
# Test managing package roots, after using a scan-ea-anno command to build a build.bml file.
#

echo "Set the workspace root appropriately"
bmladmin set-workspace-root /
bmladmin set-pkg-root Main_src /home/psmith/t/cvs-1.11.23
bmladmin set-pkg-root Main_gen /home/psmith/t/cvs-1.11.23
bmladmin set-workspace-root /home/psmith/t/cvs-1.11.23

echo "Add a couple of packages"
bmladmin add-pkg lib
bmladmin add-pkg zlib
bmladmin show-root

echo "Set roots appropriately"
bmladmin set-pkg-root zlib_src @workspace/zlib
bmladmin set-pkg-root zlib_gen @workspace/zlib
bmladmin set-pkg-root lib_src @workspace/lib
bmladmin set-pkg-root lib_gen @workspace/lib
bmladmin show-root

echo "Try to set the workspace root lower that lib_src - error"
set +e
bmladmin set-workspace-root @lib_src/.deps
set -e

echo "Try to set the lib_src root above the workspace root - error"
set +e
bmladmin set-pkg-root lib_src /home/psmith
set -e

echo "Add a file in the zlib directory into the zlib package"
bmladmin set-file-pkg zlib @zlib_src/crc32.c
bmladmin show-files -p -f @zlib_src

echo "Try to add a file from lib_src into zlib - error"
set +e
bmladmin set-file-pkg zlib @lib_src/getdate.c
set -e

echo "Removing the lib package"
bmladmin rm-pkg lib
bmladmin show-root
bmladmin show-pkg







