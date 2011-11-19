#!/bin/bash -e

#
# Test that the "disco" script can be invoked via a relative path (bug #10)
#

thisDir=`pwd`
discoPath=`which disco`
discoDir=`dirname $discoPath`

#
# We cd into the same directory that the 'disco' script is in, but ask
# it to place the buildstore.disco file into the test-output directory.
# To trigger the bug, we run "scan-tree", since this sub-command needs
# to load the native libraries (but was failing).
#

cd $discoDir && ./disco -f $discoDir/buildstore.disco scan-tree /usr/include

