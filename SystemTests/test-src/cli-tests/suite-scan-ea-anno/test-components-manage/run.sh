#!/bin/bash -e

#
# Test adding/managing component names, after using a scan-ea-anno command to build a buildstore.disco file.
#

echo "Showing components, before having defined any"
disco show-comp

echo "Remove a component that doesn't exist yet - should fail"
set +e
disco rm-comp comp1
echo "Status is $?"
set -e

echo "Add a couple of new components, then show the list again"
disco add-comp component2
disco add-comp my-comp3
disco show-comp

echo "Try to add the same component name twice"
set +e
disco add-comp my-comp3
echo "Status is $?"
set -e
disco show-comp

echo "Delete a component, making sure it no longer exists"
disco rm-comp my-comp3
disco show-comp

echo "Add a component name back, essentially reusing it"
disco add-comp my-comp3
disco show-comp

echo "Try to delete the default component"
set +e
disco rm-comp None
echo "Status is $?"
set -e
disco show-comp

