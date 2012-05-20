#!/bin/bash -e

#
# Test assigning components to tasks, after using a scan-ea-anno command to build a buildstore.bml file.
#

echo "Show all tasks, with only default components"
bml show-tasks -c

echo
echo "Create a new component"
bml add-comp zlib

echo
echo "Add tasks 18-32 to the component"
bml set-task-comp zlib 18:19:20:21:22:23:24:25
bml set-task-comp zlib 26:27:28:29:30:31:32

echo
echo "Show all tasks, with their components"
bml show-tasks -c

echo
echo "Show only those tasks in the zlib component"
bml show-tasks -c -f %c/zlib

echo
echo "Show only those tasks outside the zlib component"
bml show-tasks -c -f %nc/zlib

echo
echo "Try to delete the zlib component (should fail)"
set +e
bml rm-comp zlib
echo Status is $?
set -e

echo
echo "Set the component of all files (under task 2) to be None"
bml set-task-comp None 2/

echo
echo "Show only those tasks in the zlib component (empty)"
bml show-tasks -c -f %c/zlib

echo
echo "Try again to delete the zlib component (should succeed)"
bml rm-comp zlib
