/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and
 *        implementation and/or initial documentation
 *******************************************************************************/

#include <dlfcn.h>

/*
 * A useful macro for fetching a pointer to the real version of the function
 * We search the list of dynamic libraries to find the next occurrence of this symbol,
 * which should be the "real" version of the function. Each interposed function can
 * use this macro to efficiently find the location of the real function. Note that we
 * only need to initialize the variable once.
 * For example:
 * 		FETCH_REAL_FN(FILE *, real_fopen, "fopen");
 * assigns the memory address of the real "fopen" function to the real_fopen variable.
 */
#define FETCH_REAL_FN(type, fn_var, fn_name) \
		static type (*(fn_var))() = NULL; \
		if (!(fn_var)){ \
			(fn_var) = dlsym(RTLD_NEXT, (fn_name)); \
		}

/*
 * Prototypes for functions defined in interpose_utils.c
 */
char *cfs_get_cwd(int use_cache);
void *cfs_malloc(size_t size);
void cfs_debug(int level, char *string, ...);
void cfs_debug_env(int level, char * const *envp);
int cfs_get_debug_level();
void cfs_set_debug_level(int level);
char *cfs_get_log_file();
void cfs_set_log_file(char *name);
int cfs_get_path_of_dirfd(char *result_path, int dirfd, const char *pathname);
int cfs_isdirectory(const char *pathname);
int cfs_is_system_path(const char *pathname);
