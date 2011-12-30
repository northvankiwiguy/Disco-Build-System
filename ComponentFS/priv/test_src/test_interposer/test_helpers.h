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

#ifndef TRUE
#define TRUE 1
#endif

#ifndef FALSE
#define FALSE 0
#endif

/*
 * Prototypes for functions defined in test-helpers.c
 */
extern char *th_getcwd();
extern int th_create_empty_file(char *name, int perms);
extern int th_create_nonempty_file(char *name, int perms, char *content);
extern int th_get_file_perms(char *name);
extern int th_get_file_size(char *name);

/*======================================================================*/
