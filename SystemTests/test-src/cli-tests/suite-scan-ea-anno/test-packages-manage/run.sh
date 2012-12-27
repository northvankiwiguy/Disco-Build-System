#!/bin/bash -e

#
# Test adding/managing package names, after using a scan-ea-anno command to build a build.bml file.
#

echo "Showing packages, before having defined any"
bmladmin show-pkg

echo "Remove a package that doesn't exist yet - should fail"
set +e
bmladmin rm-pkg pkg1
echo "Status is $?"
set -e

echo "Add a couple of new packages, then show the list again"
bmladmin add-pkg package2
bmladmin add-pkg my-pkg3
bmladmin show-pkg

echo "Try to add the same package name twice"
set +e
bmladmin add-pkg my-pkg3
echo "Status is $?"
set -e
bmladmin show-pkg

echo "Delete a package, making sure it no longer exists"
bmladmin rm-pkg my-pkg3
bmladmin show-pkg

echo "Add a package name back, essentially reusing it"
bmladmin add-pkg my-pkg3
bmladmin show-pkg

echo "Try to delete the default package"
set +e
bmladmin rm-pkg "<import>"
echo "Status is $?"
set -e
bmladmin show-pkg

echo "Renaming a package"
bmladmin rename-pkg my-pkg3 package3
bmladmin show-pkg

echo "Rename a package to an existing name"
set +e
bmladmin rename-pkg package3 package2
echo "Status is $?"
set -e
bmladmin show-pkg

echo "Adding a two new folders"
bmladmin add-pkg -f Folder1
bmladmin add-pkg -f Folder2
bmladmin show-pkg

echo "Adding a folder with a pre-existing name"
set +e
bmladmin add-pkg -f Folder2
echo "Status is $?"
set -e
bmladmin show-pkg

echo "Moving a package into a sub-folder"
bmladmin move-pkg package2 Folder2
bmladmin show-pkg

echo "Moving a folder into a sub-folder"
bmladmin move-pkg Folder1 Folder2
bmladmin show-pkg

echo "Trying to create a folder-cycle"
set +e
bmladmin move-pkg Folder2 Folder1
echo "Status is $?"
set -e
bmladmin show-pkg

echo "Try to remove a folder that has children"
set +e
bmladmin rm-pkg Folder2
echo "Status is $?"
set -e
bmladmin show-pkg

echo "Remove a folder that doesn't have children"
bmladmin rm-pkg Folder1
bmladmin show-pkg

