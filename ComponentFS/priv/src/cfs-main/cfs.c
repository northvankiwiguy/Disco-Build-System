/*
 * cfs.c
 *
 *  Created on: Jul 8, 2010
 *      Author: psmith
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int argc, char *argv[], char *envp[])
{

	printf("Starting ComponentFS shell\n");

	// find the preload library by searching either the CFS_ROOT/bin directory, or searching the standard path.
	// this *must* be an absolute path name
	setenv("LD_PRELOAD", "./libcfs.so", 1);

	// create a shared memory segment used for logging of trace information
	// fork a consumer process that dumps the trace log to disk periodically. Use semaphores to control access to shared memory.

	// pass in the arguments that were passed to cfs, unless there were none in which case we create a default shell.
	char *default_shell = getenv("SHELL");
	execl(default_shell, default_shell, NULL);
}
