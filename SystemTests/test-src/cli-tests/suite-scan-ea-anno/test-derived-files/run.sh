#!/bin/bash -e

#
# Test commands that show the "derived" relationship between files, after using a scan-ea-anno 
# command to build a buildstore.bml file.
#

echo
echo "Show files immediately derived from trees.c"
bml show-derived-files trees.c

echo
echo "Show all files derived from tree.c"
bml show-derived-files -a trees.c

echo
echo "Show all files derived from config.h"
bml show-derived-files -a config.h

echo
echo "Show all .a files derived from config.h"
bml show-derived-files -a -f "*.a" config.h

echo
echo "Show all files derived from zlib.c"
bml show-derived-files -a zlib.c

echo
echo "Show all files derived from zlib.c (using full path name)"
bml show-derived-files -a /home/psmith/t/cvs-1.11.23/src/zlib.c

echo
echo "Show files used to create zlib.h (none)"
bml show-input-files zlib.h

echo
echo "Show files used to create zlib.o"
bml show-input-files zlib.o

echo
echo "Show files directly used to create libz.a"
bml show-input-files libz.a

echo
echo "Show all files used to create libz.a"
bml show-input-files -a libz.a

echo
echo "Show all .h files used to create libz.a"
bml show-input-files -a -f "*.h" libz.a

echo
echo "Show all .h files used to create libz.a (using full path name)"
bml show-input-files -a -f "*.h" /home/psmith/t/cvs-1.11.23/zlib/libz.a

echo
echo "Show tasks that use zlib.o"
bml show-tasks-that-use zlib.o

echo
echo "Show tasks that read zlib.o"
bml show-tasks-that-use --read zlib.o

echo
echo "Show tasks that write zlib.o"
bml show-tasks-that-use --write zlib.o

echo
echo "Show tasks that use trees.c"
bml show-tasks-that-use trees.c

echo
echo "Show tasks that read trees.c"
bml show-tasks-that-use -r trees.c

echo
echo "Show tasks that write trees.c (none)"
bml show-tasks-that-use -w trees.c

echo
echo "Show tasks that use libdiff.a"
bml show-tasks-that-use libdiff.a

echo
echo "Show tasks that read libdiff.a"
bml show-tasks-that-use -r libdiff.a

echo
echo "Show tasks that write libdiff.a"
bml show-tasks-that-use -w libdiff.a

echo
echo "Show files used by task 1 (none)"
bml show-files-used-by 1

echo
echo "Show files used by task 4"
bml show-files-used-by 4

echo
echo "Show files read by task 4"
bml show-files-used-by -r 4

echo
echo "Show files written by task 4"
bml show-files-used-by -w 4

echo
echo "Show files used by task 4, 5 or 6"
bml show-files-used-by 4:5:6

echo
echo "Show files written by task 2's children"
bml show-files-used-by -w 2/

echo
echo "Show .h files read by task 2's children"
bml show-files-used-by --read -f "*.h" 2/

echo
echo "Show .o files read by task 102"
bml show-files-used-by --read -f "*.o" 102

echo
echo "Show .o files read by task 102, with components"
bml show-files-used-by --read -f "*.o" -c 102
