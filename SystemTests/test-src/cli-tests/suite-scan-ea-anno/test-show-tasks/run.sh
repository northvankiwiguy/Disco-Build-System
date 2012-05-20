#!/bin/bash -e

#
# Test the "show-tasks" output, after using the scan-ea-anno command to build a buildstore.bml file.
#

echo "show-tasks - with default settings"
bml show-tasks

echo
echo "show-tasks - with restricted column width"
bml -w 40 show-tasks

echo
echo "show-tasks - with short output"
bml show-tasks -s

echo
echo "show-tasks - with long output"
bml show-tasks -l

echo
echo "show-tasks - with a single task (and it's parents)"
bml show-tasks -f 107

echo
echo "show-tasks - with all tasks underneath a specific task"
bml show-tasks -f 3/

echo
echo "show-tasks - with two levels of task"
bml show-tasks -f 2/2

echo
echo "show-tasks - with three levels of task"
bml show-tasks -f 2/3

echo
echo "show-tasks - a small number of tasks, specified by task ID"
bml show-tasks -f 105:106:107

echo
echo "show-tasks - a small number of tasks removed from a larger set"
bml show-tasks -f 2/:-105:-106:-107

