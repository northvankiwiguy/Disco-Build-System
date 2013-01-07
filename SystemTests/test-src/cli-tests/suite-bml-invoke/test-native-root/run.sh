#!/bin/bash -e

#
# Test setting a removal of package root mappings to native paths.
#
root=`realpath $TEST_SRC/../../../..`
FILTER="s#$root#ROOT#g"

echo "Show the package definitions".
bml -l

echo "Show the default package root mappings"
bml -r | sed $FILTER

echo "Override one of the roots with a new native path"
bml -s diff_src /tmp
bml -r | sed $FILTER

echo "Override a second root with another native path"
bml -s zlib_gen /etc
bml -r | sed $FILTER

echo "Delete the second root mapping"
bml -d zlib_gen
bml -r | sed $FILTER

echo "Try to set an invalid root name (invalid suffix)"
set +e
bml -s zlib_gon /etc
echo Status $?
bml -r | sed $FILTER
set -e

echo "Try to set another invalid root name (invalid package name)"
set +e
bml -s foo_src /etc
echo Status $?
bml -r | sed $FILTER
set -e

echo "Try to set an invalid native path (non existent)"
set +e
bml -s zlib_gen /bad/path
echo Status $?
bml -r | sed $FILTER
set -e

echo "Try to set an invalid native path (not a directory)"
set +e
bml -s zlib_gen /etc/passwd
echo Status $?
bml -r | sed $FILTER
set -e

echo "Try to delete a mapping that wasn't set"
set +e
bml -d src_src
echo Status $?
bml -r | sed $FILTER
set -e

echo "Try to delete the mapping for a non-existent root".
set +e
bml -d foo_src
echo Status $?
bml -r | sed $FILTER
set -e
