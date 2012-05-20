#!/bin/bash -e

#
# Test adding/managing package names, after using a scan-ea-anno command to build a buildstore.bml file.
#

echo "Showing packages, before having defined any"
bml show-pkg

echo "Remove a package that doesn't exist yet - should fail"
set +e
bml rm-pkg pkg1
echo "Status is $?"
set -e

echo "Add a couple of new packages, then show the list again"
bml add-pkg package2
bml add-pkg my-pkg3
bml show-pkg

echo "Try to add the same package name twice"
set +e
bml add-pkg my-pkg3
echo "Status is $?"
set -e
bml show-pkg

echo "Delete a package, making sure it no longer exists"
bml rm-pkg my-pkg3
bml show-pkg

echo "Add a package name back, essentially reusing it"
bml add-pkg my-pkg3
bml show-pkg

echo "Try to delete the default package"
set +e
bml rm-pkg None
echo "Status is $?"
set -e
bml show-pkg

