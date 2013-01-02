#!/bin/bash -e

#
# Test setting a removal of build aliases.
#

echo "Show the package definitions".
bml -l

echo "Define two package aliases"
bml -a all zlib src diff
bml -a default src
bml -l

echo "Redefine a package alias"
bml -a default diff
bml -l

echo "Remove a package alias"
bml -u default
bml -l

echo "Create an alias to invalid packages".
set +e
bml -a badalias zlib bad
echo Status $?

echo "Try to remove an alias that doesn't exist"
bml -u badaliasname
echo Status $?
set -e
