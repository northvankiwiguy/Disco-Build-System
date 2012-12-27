#!/bin/bash -e

#
# Test the "show-actions" output, after using the scan-ea-anno command to build a build.bml file.
#

echo "show-actions - with default settings"
bmladmin show-actions

echo
echo "show-actions - with restricted column width"
bmladmin -w 40 show-actions

echo
echo "show-actions - with short output"
bmladmin show-actions -s

echo
echo "show-actions - with long output"
bmladmin show-actions -l

echo
echo "show-actions - with a single action (and it's parents)"
bmladmin show-actions -f 107

echo
echo "show-actions - with all actions underneath a specific action"
bmladmin show-actions -f 3/

echo
echo "show-actions - with two levels of action"
bmladmin show-actions -f 2/2

echo
echo "show-actions - with three levels of action"
bmladmin show-actions -f 2/3

echo
echo "show-actions - a small number of actions, specified by action ID"
bmladmin show-actions -f 105:106:107

echo
echo "show-actions - a small number of actions removed from a larger set"
bmladmin show-actions -f 2/:-105:-106:-107

