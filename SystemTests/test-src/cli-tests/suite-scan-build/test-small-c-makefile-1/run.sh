#!/bin/bash -e

. $TEST_SRC/../functions.sh

# build the small test program
make -C $TEST_SRC/../examples/small-c-makefile clean >/dev/null 2>&1
bmladmin scan-build make -C $TEST_SRC/../examples/small-c-makefile all >/dev/null 2>&1

# 
# Note that we have difficulty with writing generic tests here, since the
# exact output of commands will depend on the name of temporary files
# (which are different each time), and the absolute path of the system
# testing directory. We therefore limit out testing to grepping for
# the relative part of the file names, rather than trying to match the
# whole path. Even the version of gcc can vary from one test machine 
# to the next.

# test action 1 - the top level make
match "bmladmin show-actions -l -f 1" "small-c-makefile all"
match "bmladmin show-files-used-by -r 1" "small-c-makefile/Makefile"
match_empty "bmladmin show-files-used-by -w 1"

# test action 2 - the make that enters subdir1
match "bmladmin show-actions -l -f 2" "/bin/make -C subdir1 all"
match "bmladmin show-files-used-by -r 2" "subdir1/Makefile"

# test action 3 - the compile of file1.c. Doesn't read or write files.
match "bmladmin show-actions -l -f 3" "-c file1.c"
match_empty "bmladmin show-files-used-by -r 3"
match_empty "bmladmin show-files-used-by -w 3"

# test action4 - the cc1 operation on file1.c
match "bmladmin show-actions -l -f 4" "cc1"
match "bmladmin show-files-used-by -r 4" "subdir1/file1.c"

# test action5 - the as1 operation to produce file1.o
match "bmladmin show-actions -l -f 5" "/bin/as"
match "bmladmin show-files-used-by -w 5" "subdir1/file1.o"

# test action19 - writing to /dev/null should not log a file access
match "bmladmin show-actions -l -f 19" "echo Hello"
match_empty "bmladmin show-files-used-by 19"
