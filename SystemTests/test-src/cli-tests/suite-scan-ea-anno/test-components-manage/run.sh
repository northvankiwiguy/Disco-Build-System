#!/bin/bash -e

#
# Test adding/managing component names, after using a scan-ea-anno command to build a buildstore.bml file.
#

echo "Showing components, before having defined any"
bml show-comp

echo "Remove a component that doesn't exist yet - should fail"
set +e
bml rm-comp comp1
echo "Status is $?"
set -e

echo "Add a couple of new components, then show the list again"
bml add-comp component2
bml add-comp my-comp3
bml show-comp

echo "Try to add the same component name twice"
set +e
bml add-comp my-comp3
echo "Status is $?"
set -e
bml show-comp

echo "Delete a component, making sure it no longer exists"
bml rm-comp my-comp3
bml show-comp

echo "Add a component name back, essentially reusing it"
bml add-comp my-comp3
bml show-comp

echo "Try to delete the default component"
set +e
bml rm-comp None
echo "Status is $?"
set -e
bml show-comp

