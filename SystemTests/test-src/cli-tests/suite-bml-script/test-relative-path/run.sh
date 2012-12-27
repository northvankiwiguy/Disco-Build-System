#!/bin/bash -e

#
# Test that the "bmladmin" script can be invoked via a relative path (bug #10)
#

thisDir=`pwd`
bmlPath=`which bmladmin`
bmlDir=`dirname $bmlPath`

#
# We cd into the same directory that the 'bmladmin' script is in, but ask
# it to place the build.bml file into the test-output directory.
# To trigger the bug, we run "scan-tree", since this sub-command needs
# to load the native libraries (but was failing).
#

cd $bmlDir && ./bmladmin -f $thisDir/build.bml scan-tree /usr/include

