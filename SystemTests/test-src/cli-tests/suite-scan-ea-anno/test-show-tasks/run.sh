#!/bin/bash -e

#
# Test the "show-tasks" output, after using the scan-ea-anno command to build a buildstore.disco file.
#

echo "show-tasks - with default settings"
disco show-tasks

echo
echo "show-tasks - with restricted column width"
disco -w 40 show-tasks

echo
echo "show-tasks - with short output"
disco show-tasks -s

echo
echo "show-tasks - with long output"
disco show-tasks -l

echo
echo "show-tasks - with a single task (and it's parents)"
disco show-tasks -f 107

echo
echo "show-tasks - with all tasks underneath a specific task"
disco show-tasks -f 3/

echo
echo "show-tasks - with two levels of task"
disco show-tasks -f 2/2

echo
echo "show-tasks - with three levels of task"
disco show-tasks -f 2/3

echo
echo "show-tasks - a small number of tasks, specified by task ID"
disco show-tasks -f 105:106:107

echo
echo "show-tasks - a small number of tasks removed from a larger set"
disco show-tasks -f 2/:-105:-106:-107

