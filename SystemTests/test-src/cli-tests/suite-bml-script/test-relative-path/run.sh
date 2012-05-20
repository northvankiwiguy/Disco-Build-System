#!/bin/bash -e

#
# Test that the "bml" script can be invoked via a relative path (bug #10)
#

thisDir=`pwd`
bmlPath=`which bml`
bmlDir=`dirname $bmlPath`

#
# We cd into the same directory that the 'bml' script is in, but ask
# it to place the buildstore.bml file into the test-output directory.
# To trigger the bug, we run "scan-tree", since this sub-command needs
# to load the native libraries (but was failing).
#

cd $bmlDir && ./bml -f $thisDir/buildstore.bml scan-tree /usr/include

