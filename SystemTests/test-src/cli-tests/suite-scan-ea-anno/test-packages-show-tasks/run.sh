#!/bin/bash -e

#
# Test assigning packages to tasks, after using a scan-ea-anno command to build a buildstore.bml file.
#

echo "Show all tasks, with only default packages"
bml show-tasks -p

echo
echo "Create a new package"
bml add-pkg zlib

echo
echo "Add tasks 18-32 to the package"
bml set-task-pkg zlib 18:19:20:21:22:23:24:25
bml set-task-pkg zlib 26:27:28:29:30:31:32

echo
echo "Show all tasks, with their packages"
bml show-tasks -p

echo
echo "Show only those tasks in the zlib package"
bml show-tasks -p -f %p/zlib

echo
echo "Show only those tasks outside the zlib package"
bml show-tasks -p -f %np/zlib

echo
echo "Try to delete the zlib package (should fail)"
set +e
bml rm-pkg zlib
echo Status is $?
set -e

echo
echo "Set the package of all files (under task 2) to be None"
bml set-task-pkg None 2/

echo
echo "Show only those tasks in the zlib package (empty)"
bml show-tasks -p -f %p/zlib

echo
echo "Try again to delete the zlib package (should succeed)"
bml rm-pkg zlib
