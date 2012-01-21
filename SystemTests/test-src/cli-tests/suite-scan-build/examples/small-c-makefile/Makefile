all: libs
	$(CC) -o prog main.c subdir1/lib1.a subdir2/lib2.a

libs:
	$(MAKE) -C subdir1 all
	$(MAKE) -C subdir2 all
	
	# the following command is useless
	echo Hello > /dev/null

clean:
	rm -f *.o prog
	$(MAKE) -C subdir1 clean
	$(MAKE) -C subdir2 clean
	