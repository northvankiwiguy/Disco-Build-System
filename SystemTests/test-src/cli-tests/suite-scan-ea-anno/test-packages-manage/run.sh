#!/bin/bash -e

#
# Test adding/managing package names, after using a scan-ea-anno command to build a build.bml file.
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
bml rm-pkg "<import>"
echo "Status is $?"
set -e
bml show-pkg

echo "Renaming a package"
bml rename-pkg my-pkg3 package3
bml show-pkg

echo "Rename a package to an existing name"
set +e
bml rename-pkg package3 package2
echo "Status is $?"
set -e
bml show-pkg

echo "Adding a two new folders"
bml add-pkg -f Folder1
bml add-pkg -f Folder2
bml show-pkg

echo "Adding a folder with a pre-existing name"
set +e
bml add-pkg -f Folder2
echo "Status is $?"
set -e
bml show-pkg

echo "Moving a package into a sub-folder"
bml move-pkg package2 Folder2
bml show-pkg

echo "Moving a folder into a sub-folder"
bml move-pkg Folder1 Folder2
bml show-pkg

echo "Trying to create a folder-cycle"
set +e
bml move-pkg Folder2 Folder1
echo "Status is $?"
set -e
bml show-pkg

echo "Try to remove a folder that has children"
set +e
bml rm-pkg Folder2
echo "Status is $?"
set -e
bml show-pkg

echo "Remove a folder that doesn't have children"
bml rm-pkg Folder1
bml show-pkg

