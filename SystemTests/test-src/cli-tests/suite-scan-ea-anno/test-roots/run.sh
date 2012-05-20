#!/bin/bash -e

#
# Test commands that use file roots.
#

echo
echo "Show the initial set of roots"
bml show-root

echo
echo "Show all files, with roots displayed"
bml show-files -r

echo
echo "Remove a root that doesn't exist (should fail)"
set +e
bml rm-root psmith_root
echo Status is $?
set -e

echo
echo "Add a root (psmith_root to /home/psmith), show the list"
bml add-root psmith_root /home/psmith
bml show-root

echo
echo "Add another two roots (cvs_root at /home/psmith/t/cvs-1.11.23/ and zlib at cvs_root:/zlib), show the list"
bml add-root cvs_root /home/psmith/t/cvs-1.11.23
bml add-root zlib @cvs_root/zlib
bml show-root

echo
echo "Show just psmith_root by itself"
bml show-root psmith_root

echo
echo "Show an invalid root name (should fail)"
set +e
bml show-root bad_root
echo Status is $?
set -e

echo
echo "Try to double-add cvs_root at the same location (should fail)"
set +e
bml add-root cvs_root /home/psmith/t/cvs-1.11.23
echo Status is $?
set -e

echo
echo "Try to double-add cvs_root at a different location (should succeed)"
bml add-root cvs_root /home/psmith/t

echo
echo "Try to add a new root at a location that already has a root (should fail)"
set +e
bml add-root new_root /home/psmith
echo Status is $?
set -e

echo
echo "Remove zlib, show the list"
bml rm-root zlib
bml show-root

echo
echo "Remove zlib again (should fail)"
set +e
bml rm-root zlib
echo Status is $?
set -e

echo
echo "Add the zlib back again, show the list"
bml add-root zlib @cvs_root/cvs-1.11.23/zlib
bml show-root

echo
echo "Show all the files, with the roots showing"
bml show-files -r

echo
echo "Show only the files within psmith_root"
bml show-files -r -f @psmith_root

echo
echo "Show only the files within cvs_root"
bml show-files -r -f @cvs_root

echo
echo "Show only the files within zlib"
bml show-files -r -f @zlib

echo
echo "Show only the files within zlib, without showing root names"
bml show-files -f @zlib

