#!/bin/bash -e

#
# Test managing package roots, after using a scan-ea-anno command to build a build.bml file.
#

echo "Set the workspace root appropriately"
bml set-workspace-root /home/psmith/t/cvs-1.11.23

echo "Add a couple of packages"
bml add-pkg lib
bml add-pkg zlib
bml show-root

echo "Set roots appropriately"
bml set-pkg-root zlib_src @workspace/zlib
bml set-pkg-root zlib_gen @workspace/zlib
bml set-pkg-root lib_src @workspace/lib
bml set-pkg-root lib_gen @workspace/lib
bml show-root

echo "Try to set the workspace root lower that lib_src - error"
set +e
bml set-workspace-root @lib_src/.deps
set -e

echo "Try to set the lib_src root above the workspace root - error"
set +e
bml set-pkg-root lib_src /home/psmith
set -e

echo "Add a file in the zlib directory into the zlib package"
bml set-file-pkg zlib @zlib_src/crc32.c
bml show-files -p -f @zlib_src

echo "Try to add a file from lib_src into zlib - error"
set +e
bml set-file-pkg zlib @lib_src/getdate.c
set -e

echo "Removing the lib package"
bml rm-pkg lib
bml show-root
bml show-pkg







