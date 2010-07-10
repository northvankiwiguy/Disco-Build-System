/*******************************************************************************
 * Copyright (c) 2010 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and
 *        implementation and/or initial documentation
 *******************************************************************************/

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
