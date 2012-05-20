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


#ifndef FILE_NAME_UTILS_H_
#define FILE_NAME_UTILS_H_

extern int _cfs_combine_paths(char const *parent_path, char const *extra_path, char *combined_path);
extern void _cfs_basename(const char *orig_path, char *base_path);

#endif /* FILE_NAME_UTILS_H_ */
