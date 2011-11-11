#!/bin/bash -e

#
# Test commands that use file roots.
#

echo
echo "Show the initial set of roots"
disco show-root

echo
echo "Show all files, with roots displayed"
disco show-files -r

echo
echo "Remove a root that doesn't exist (should fail)"
set +e
disco rm-root psmith_root
echo Status is $?
set -e

echo
echo "Add a root (psmith_root to /home/psmith), show the list"
disco add-root psmith_root /home/psmith
disco show-root

echo
echo "Add another two roots (cvs_root at /home/psmith/t/cvs-1.11.23/ and zlib at cvs_root:/zlib), show the list"
disco add-root cvs_root /home/psmith/t/cvs-1.11.23
disco add-root zlib @cvs_root/zlib
disco show-root

echo
echo "Show just psmith_root by itself"
disco show-root psmith_root

echo
echo "Show an invalid root name (should fail)"
set +e
disco show-root bad_root
echo Status is $?
set -e

echo
echo "Try to double-add cvs_root at the same location (should fail)"
set +e
disco add-root cvs_root /home/psmith/t/cvs-1.11.23
echo Status is $?
set -e

echo
echo "Try to double-add cvs_root at a different location (should succeed)"
disco add-root cvs_root /home/psmith/t

echo
echo "Try to add a new root at a location that already has a root (should fail)"
set +e
disco add-root new_root /home/psmith
echo Status is $?
set -e

echo
echo "Remove zlib, show the list"
disco rm-root zlib
disco show-root

echo
echo "Remove zlib again (should fail)"
set +e
disco rm-root zlib
echo Status is $?
set -e

echo
echo "Add the zlib back again, show the list"
disco add-root zlib @cvs_root/cvs-1.11.23/zlib
disco show-root

echo
echo "Show all the files, with the roots showing"
disco show-files -r

echo
echo "Show only the files within psmith_root"
disco show-files -r -f @psmith_root

echo
echo "Show only the files within cvs_root"
disco show-files -r -f @cvs_root

echo
echo "Show only the files within zlib"
disco show-files -r -f @zlib

echo
echo "Show only the files within zlib, without showing root names"
disco show-files -f @zlib

