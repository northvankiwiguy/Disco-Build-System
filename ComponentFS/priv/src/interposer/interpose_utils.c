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

/*
 * This file contains a number of utilities functions that are used by the
 * glibc interposer.
 */

#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/param.h>
#include <sys/stat.h>

#include "interpose_utils.h"
#include "trace_buffer.h"
#include "trace_file_format.h"
#include "file_name_utils.h"

/*
 * The current working directory of this process.
 */
static char _cfs_saved_cwd[PATH_MAX];

/*
 * Debug level. Set this by setting the CFS_DEBUG environment variable
 * (which will be passed from each process to all its children).
 */
static int _cfs_debug_level;

/*
 * Name of the file into which log messages should be stored (defaults
 * to "cfs.log").
 */
#define DEFAULT_LOG_FILE_NAME 	"cfs.log"

static char *_cfs_log_file_name = DEFAULT_LOG_FILE_NAME;

/*======================================================================
 * _cfs_get_cwd
 *
 * Return the absolute path to this process's current working directory.
 * If use_cache is TRUE, using a cached version of the cwd is acceptable.
 *
 *======================================================================*/

char *_cfs_get_cwd(int use_cache)
{

	/*
	 * If we don't already have the cwd cached, we need to ask the OS, and then
	 * save it in a buffer. This function will be called whenever a relative
	 * path is accessed, so it must be fast.
	 */
	if ((_cfs_saved_cwd[0] == '\0') || !use_cache) {
		if (getcwd(_cfs_saved_cwd, PATH_MAX) == NULL){
			fprintf(stderr, "Error: cfs couldn't determine current working directory.\n");
			exit(1);
		}
	}
	return _cfs_saved_cwd;
}

/*======================================================================
 * _cfs_malloc
 *
 * Wrapper around the standard malloc function that terminates the
 * program if there's a shortage of memory.
 *======================================================================*/

void *_cfs_malloc(size_t size)
{
	void *ptr = malloc(size);
	if (ptr == NULL) {
		fprintf(stderr, "Error: cfs couldn't allocate memory.\n");
		exit(1);
	}

	return ptr;
}

/*======================================================================
 * _cfs_open_log
 *
 * Open the CFS log file, ensuring that we're the only process that
 * is allowed to write to it.
 *======================================================================*/

FILE *_cfs_open_log()
{
	FETCH_REAL_FN(FILE *, real_fopen, "fopen");

	trace_buffer_lock_logfile();
	return real_fopen(_cfs_log_file_name, "a");
}

/*======================================================================
 * _cfs_close_log
 *
 * Close the CFS log file, allowing other processes to write to it.
 *======================================================================*/

void _cfs_close_log(FILE *logFile)
{
	fclose(logFile);
	trace_buffer_unlock_logfile();
}

/*======================================================================
 * _cfs_get_debug_level
 *
 * Return the current debug level setting.
 *
 *======================================================================*/

int _cfs_get_debug_level()
{
	return _cfs_debug_level;
}

/*======================================================================
 * _cfs_set_debug_level
 *
 * Set the debug level for CFS debugging.
 *======================================================================*/

void _cfs_set_debug_level(int level)
{
	if (level < 0){
		level = 0;
	} else if (level > 2){
		level = 2;
	}
	_cfs_debug_level = level;
}

/*======================================================================
 * _cfs_get_log_file
 *
 * Return the name of the log file.
 *
 *======================================================================*/

char *_cfs_get_log_file()
{
	return _cfs_log_file_name;
}

/*======================================================================
 * _cfs_set_log_file
 *
 * Set the name of the log file into which debug output is stored.
 * Providing a NULL pointer resets the name back to the default.
 *======================================================================*/

void _cfs_set_log_file(char *name)
{
	if (name == NULL) {
		_cfs_log_file_name = DEFAULT_LOG_FILE_NAME;
	} else {
		_cfs_log_file_name = name;
	}
}

/*======================================================================
 * _cfs_debug
 *
 * Display debug output. Only display debug output that's within the
 * current debug_level (set via the CFS_DEBUG environment variable).
 *======================================================================*/

void _cfs_debug(int level, char *string, ...)
{
	va_list args;

	va_start(args, string);
	if (level <= _cfs_debug_level) {
		FILE *out = _cfs_open_log();

		/* if we can't open it, discard data */
		if (out != NULL) {
			fprintf(out, "PID %d: ", getpid());
			vfprintf(out, string, args);
			fprintf(out, "\n");
		}
		_cfs_close_log(out);
	}
}

/*======================================================================
 * _cfs_debug_env
 *
 * Display the given set of environment variables as a debug message.
 *  - level - The debug level of the output (for filtering whether
 *            the output is displayed).
 *  - envp  - The environment to display.
 *
 *======================================================================*/

void _cfs_debug_env(int level, char * const *envp)
{
	if (level <= _cfs_debug_level) {
		char * const *ptr = envp;
		FILE *out = _cfs_open_log();

		/* if we can't open it, discard data */
		if (out != NULL) {
			fprintf(out, "Environment Variables:\n");
			while (*ptr != NULL) {
				fprintf(out, "  %s\n", *ptr);
				ptr++;
			}
		}
		_cfs_close_log(out);
	}
}

/*======================================================================
 * _cfs_get_path_of_dirfd
 *
 * Given a directory file descriptor ("dirfd"), determine the path of the
 * directory it refers to. Append to this the content of the "pathname"
 * string, and write the concatenated result into "result_path".
 *
 * Return 0 on success, or -1 on failure.
 *======================================================================*/

int _cfs_get_path_of_dirfd(char *result_path, int dirfd, const char *pathname)
{
	FETCH_REAL_FN(int, real_open, "open");
	FETCH_REAL_FN(int, real_fchdir, "fchdir");

	/* save the current directory */
	int saved_dir = real_open(".", O_RDONLY);
	if (saved_dir == -1) {
		return -1;
	}

	/* change to the "dirfd" directory */
	if (real_fchdir(dirfd) == -1){
		return -1;
	}

	/* fetch the current working directory into the user-supplied buffer */
	if (getcwd(result_path, PATH_MAX) == NULL){
		real_fchdir(saved_dir);
		close(saved_dir);
		return -1;
	}

	/* switch back to the real current directory */
	if (real_fchdir(saved_dir) == -1){
		close(saved_dir);
		return -1;
	}

	/* we're done with the saved directory fd */
	close(saved_dir);

	/* append the "pathname" onto the directory (assuming it's not too long). */
	if (strlen(result_path) + strlen(pathname) + 2 >= PATH_MAX){
		errno = ENAMETOOLONG;
		return -1;
	}

	/* TODO: make this more efficient */
	strcat(result_path, "/");
	strcat(result_path, pathname);
	return 0;
}

/*======================================================================
 * Helper - _cfs_isdirectory(char *path)
 *
 * Return TRUE if the "path" refers to a directory, or FALSE if it's
 * not a directory, or if it doesn't exist.
 *
 *======================================================================*/

int _cfs_isdirectory(const char *pathname)
{
	struct stat buf;
	if (stat(pathname, &buf) == -1){
		return FALSE;
	}
	return S_ISDIR(buf.st_mode);
}

/*======================================================================
 * Helper - _cfs_is_system_path(char *path)
 *
 * Return TRUE if the "path" refers to a system-related file or directory,
 * such as /dev/tty, /proc/self, or anything that's clearly not in our
 * build tree.
 *
 *======================================================================*/

int _cfs_is_system_path(const char *pathname)
{
	return ((strncmp(pathname, "/dev/", 5) == 0) ||
		(strncmp(pathname, "/proc/", 6) == 0) ||
		(strncmp(pathname, "/sys/", 5) == 0));
}

/*======================================================================
 * _cfs_open_common()
 *
 * A common function for tracing detail of open, open64 or related library
 * calls.
 *
 * - filename  - the path (absolute or relative) to the file/directory
 *               being accessed.
 * - flags     - the standard open flags, such as O_RDONLY or O_CREAT.
 * - normalize - TRUE if filename should be normalized (. and .. removed,
 * 			     and symlinks followed).
 *======================================================================*/

int
_cfs_open_common(const char *filename, int flags, int normalize)
{
	extern int errno;
	int tmp_errno = errno; 				/* save errno, in case we change it */

	/* compute the absolute and normalized path */
	char *new_path = (char *)filename;
	char normalized_path[PATH_MAX];
	if (normalize) {
		if (_cfs_combine_paths(_cfs_get_cwd(TRUE), filename, normalized_path) == -1) {
			return -1;
		}
		new_path = normalized_path;
	}

	/* ignore system directories, such as /dev, /proc, etc */
	if (_cfs_is_system_path(new_path)) {
		errno = tmp_errno;
		return 0;
	}

	/* determine if we're opening a directory */
	int isdir = _cfs_isdirectory(new_path);

	/* if there's a trace buffer, write the file name to it */
	if (trace_buffer_lock() == 0){
		if ((flags & (O_APPEND|O_CREAT|O_WRONLY)) != 0) {
			trace_buffer_write_byte(isdir ? TRACE_DIR_WRITE : TRACE_FILE_WRITE);
		} else if (flags & O_RDWR) {
			trace_buffer_write_byte(isdir ? TRACE_DIR_MODIFY : TRACE_FILE_MODIFY);
		} else {
			trace_buffer_write_byte(isdir ? TRACE_DIR_READ : TRACE_FILE_READ);
		}
		trace_buffer_write_int(_cfs_my_process_number);
		trace_buffer_write_string(new_path);
		trace_buffer_unlock();
	}
	errno = tmp_errno;					/* restore original errno value */

	/* all is good */
	return 0;
}

/*======================================================================
 * _cfs_delete_common()
 *
 * A common function for tracing details of unlink, remove, rmdir and
 * other deletion-related system calls.
 *======================================================================*/

int
_cfs_delete_common(const char *filename, int is_dir)
{
	extern int errno;
	int tmp_errno = errno; 				/* save errno, in case we change it */

	/* compute the absolute and normalized path */
	char new_path[PATH_MAX];
	if (_cfs_combine_paths(_cfs_get_cwd(TRUE), filename, new_path) == -1) {
		return -1;
	}

	/* ignore system directories, such as /dev, /proc, etc */
	if (_cfs_is_system_path(new_path)) {
		errno = tmp_errno;
		return 0;
	}

	/* if there's a trace buffer, write the file name to it */
	if (trace_buffer_lock() == 0){
		trace_buffer_write_byte(is_dir ? TRACE_DIR_DELETE : TRACE_FILE_DELETE);
		trace_buffer_write_int(_cfs_my_process_number);
		trace_buffer_write_string(new_path);
		trace_buffer_unlock();
	}
	errno = tmp_errno;					/* restore original errno value */

	/* all is good */
	return 0;
}

/*======================================================================
 * Helper: _cfs_modify_envp
 *
 * This function is a helper function for all execXX() functions. It
 * modifies the set of environment variables to ensure that they
 * contain suitable values for:
 *   LD_PRELOAD - must refer to this interceptor library.
 *   CFS_ID - provides the shared memory ID for our trace buffer.
 *   CFS_PARENT_ID - provides our parent process ID.
 *   CFS_DEBUG - provides our debug level.
 *   CFS_LOG_FILE - provides the name of the debug log file.
 * For numerous reasons, these variable could be removed or overwritten,
 * but the success of CFS depends on them being set.
 *======================================================================*/

char * const *
_cfs_modify_envp(char *const * envp)
{
	extern int errno;

	/* if we're not tracing, we don't need to do any of this */
	if (_cfs_id == 0) {
		return envp;
	}

	int tmp_errno = errno; 		/* take care not to disrupt errno */

	/*
	 * First, make a new copy of the original array, but add four extra
	 * pointers to the array, just in case we need to add each of the
	 * four new environment variables (however, if they're already there,
	 * we won't need to). The first step is to figure out how many
	 * entries there are in the environment.
	 */

	/* make sure we account for the extra four, plus the terminating NULL. */
	int env_size = 5;
	char * const *ptr = envp;
	while (*ptr != NULL) {
		env_size++;
		ptr++;
	}

	/* allocate a replacement envp array */
	char **new_envp = (char **)_cfs_malloc(env_size * sizeof(char *));

	/*
	 * For each of the pointers in the existing envp array, check the name of the
	 * variable to see if it's one of those we're interested in (CFS_ID, etc). If
	 * so, we'll allocate a replacement string that has the correct value in it
	 * (even though it may already be correct). If it's not one of our variables,
	 * just re-use the same string.
	 */

	/* location in envp of the previous definition - -1 means not found (yet). */
	int pos_cfs_id = -1;
	int pos_cfs_parent_id = -1;
	int pos_cfs_debug = -1;
	int pos_cfs_log_file = -1;
	int pos_ld_preload = -1;

	/* loop through the existing envp, copying the values over to the new array */
	int index = 0;
	char *str;
	while ((str = envp[index]) != NULL) {
		/*
		 * For each of the variables we care about, look for an existing definition
		 * and take note of its index. Later on we'll put the correct value (if
		 * necessary) into this location.
		 */
		if (strncmp("CFS_ID=", str, strlen("CFS_ID=")) == 0){
			pos_cfs_id = index;
		} else if (strncmp("CFS_PARENT_ID=", str, strlen("CFS_PARENT_ID=")) == 0){
			pos_cfs_parent_id = index;
		} else if (strncmp("CFS_DEBUG=", str, strlen("CFS_DEBUG")) == 0){
			pos_cfs_debug = index;
		} else if (strncmp("CFS_LOG_FILE=", str, strlen("CFS_LOG_FILE")) == 0){
			pos_cfs_log_file = index;
		} else if (strncmp("LD_PRELOAD=", str, strlen("LD_PRELOAD=")) == 0){
			pos_ld_preload = index;
		}

		new_envp[index] = str;
		index++;
	}

	/*
	 * For any variables that weren't already in envp, we need to add them. They'll
	 * go at the end of new_envp (we left space for four additional variables).
	 */
	if (pos_cfs_id == -1) {
		pos_cfs_id = index++;
	}
	if (pos_cfs_parent_id == -1) {
		pos_cfs_parent_id = index++;
	}
	if (pos_cfs_debug == -1) {
		pos_cfs_debug = index++;
	}
	if (pos_cfs_log_file == -1) {
		pos_cfs_log_file = index++;
	}
	if (pos_ld_preload == -1) {
		pos_ld_preload = index++;
	}

	/* don't forget the terminating NULL */
	new_envp[index] = NULL;

	/*
	 * Now we have a location for each of the four environment variables.
	 * Either we're replacing an existing string, or adding it for the
	 * first time. In either case, we malloc a new string and insert it
	 * into new_envp.
	 */
	char *new_str = (char *)_cfs_malloc(strlen("CFS_ID=NNNNNNNNNNNNNNNNNNNNNN\0"));
	sprintf(new_str, "CFS_ID=%ld", _cfs_id);
	new_envp[pos_cfs_id] = new_str;

	new_str = (char *)_cfs_malloc(strlen("CFS_DEBUG=NNN\0"));
	sprintf(new_str, "CFS_DEBUG=%d", _cfs_get_debug_level());
	new_envp[pos_cfs_debug] = new_str;

	new_str = (char *)_cfs_malloc(strlen("CFS_PARENT_ID=NNNNNNNNNNN\0"));
	sprintf(new_str, "CFS_PARENT_ID=%d", _cfs_my_process_number);
	new_envp[pos_cfs_parent_id] = new_str;

	char *cfs_log_file = _cfs_get_log_file();
	new_str = (char *)_cfs_malloc(strlen("CFS_LOG_FILE=\0") + strlen(cfs_log_file));
	sprintf(new_str, "CFS_LOG_FILE=%s", cfs_log_file);
	new_envp[pos_cfs_log_file] = new_str;

	/*
	 * For LD_PRELOAD, we want to give a warning if the new value is different
	 * from the existing value that's already in the environment. This suggests
	 * that the user program has modified this variable (not an uncommon thing),
	 * but if we overwrite it with our own LD_PRELOAD, we could be breaking
	 * the program's functionality.
	 */
	char *existing_ld_preload = new_envp[pos_ld_preload];
	if ((existing_ld_preload != NULL) &&
		(strcmp(existing_ld_preload, _cfs_ld_preload) != 0)){
		_cfs_debug(0, "WARNING: LD_PRELOAD has been modified - program may malfunction.");
	}
	new_envp[pos_ld_preload] = _cfs_ld_preload;

	errno = tmp_errno;			/* restore errno */
	return new_envp;
}

/*======================================================================
 * Helper: _cfs_cleanup_envp
 *
 * This function cleans up any memory allocated by modify_envp.
 *======================================================================*/

void
_cfs_cleanup_envp(char * const *envp)
{
	extern int errno;

	/* if we're not tracing, we don't need to do any of this */
	if (_cfs_id == 0) {
		return;
	}

	int tmp_errno = errno; 		/* take care not to disrupt errno */

	/*
	 * Technically this function would normally just be called if
	 * an exec() call failed, which is rare. However, it's not
	 * totally impossible, so we should deallocate any memory that
	 * we allocated in cfs_modify_envp(). Search through the envp
	 * and look for the strings we added, then free them.
	 */
	char * const *ptr = envp;
	char *str;
	while ((str = *ptr) != NULL) {
		if ((strncmp("CFS_ID=", str, strlen("CFS_ID=")) == 0) ||
			(strncmp("CFS_PARENT_ID=", str, strlen("CFS_PARENT_ID=")) == 0) ||
			(strncmp("CFS_DEBUG=", str, strlen("CFS_DEBUG")) == 0)){
				free(*ptr);
		}
		ptr++;
	}

	/* finally, free our copied envp array */
	free((void *)envp);

	errno = tmp_errno;			/* restore errno */
}

/*======================================================================
 * Helper - _cfs_execve_common()
 *
 * This function is a helper for many of the interposed exec functions.
 *
 *======================================================================*/

int
_cfs_execve_common(const char *filename, char *const argv[], char *const envp[])
{
	FETCH_REAL_FN(int, real_execve, "execve");

	/*
	 * Before calling the real execve, make sure our custom environment
	 * variables are in place.
	 */
	_cfs_debug_env(2, envp);
	char * const *new_envp = _cfs_modify_envp(envp);

	int rc = real_execve(filename, argv, new_envp);

	/* if we get here, the execve failed */
	_cfs_cleanup_envp(new_envp);
	return rc;
}

/*======================================================================
 * Helper - _cfs_execvpe_common()
 *======================================================================*/

int
_cfs_execvpe_common(const char *file, char *const argv[], char *const envp[])
{
	FETCH_REAL_FN(int, real_execvpe, "execvpe");

	/*
	 * Before calling the real execvpe, make sure our custom environment
	 * variables are in place.
	 */
	char * const *new_envp = _cfs_modify_envp(envp);
	int rc = real_execvpe(file, argv, new_envp);

	/* if we get here, the execvpe failed */
	_cfs_cleanup_envp(new_envp);
	return rc;
}

/*======================================================================
 * Helper: _cfs_fopen_common()
 *
 * A common function for tracing the arguments of fopen, fopen64 and
 * other related commands.
 *
 * Returns 0 on success, or -1 on failure (with errno set).
 *======================================================================*/

int
_cfs_fopen_common(const char *filename, const char *opentype)
{
	extern int errno;
	int tmp_errno = errno;

	/*
	 * Normalize the path before writing the trace information. That is,
	 * . and .. should be removed, and so should symlinks. Also, convert
	 * relative path names to absolute paths. On failure, return our own
	 * error number.
	 */
	char new_path[PATH_MAX];
	if (_cfs_combine_paths(_cfs_get_cwd(TRUE), filename, new_path) == -1) {
		return -1;
	}

	/* ignore system directories, such as /dev, /proc, etc */
	if (_cfs_is_system_path(new_path)) {
		errno = tmp_errno;
		return 0;
	}

	/* determine if we're opening a directory */
	int isdir = _cfs_isdirectory(new_path);

	/*
	 * Grab a lock on the trace buffer. If it hasn't been initialized yet,
	 * just return and don't trace anything.
	 */
	if (trace_buffer_lock() == 0){
		/*
		 * for 'r' and 'rb' modes, the operation is a read, for "r+" and "rb+",
		 * or "r+b" it's modify, else it's a write.
		 */
		if ((strcmp(opentype, "r") == 0) ||
			(strcmp(opentype, "rb") == 0)){
			trace_buffer_write_byte(isdir ? TRACE_DIR_READ : TRACE_FILE_READ);
		} else if ((strcmp(opentype, "r+") == 0) ||
				   (strcmp(opentype, "rb+") == 0) ||
				   (strcmp(opentype, "r+b") == 0)) {
			trace_buffer_write_byte(isdir ? TRACE_DIR_MODIFY : TRACE_FILE_MODIFY);
		} else {
			trace_buffer_write_byte(isdir ? TRACE_DIR_WRITE : TRACE_FILE_WRITE);
		}
		trace_buffer_write_int(_cfs_my_process_number);
		trace_buffer_write_string(new_path);
		trace_buffer_unlock();
	}
	errno = tmp_errno;
	return 0;
}

/*======================================================================
 * Helper: _cfs_convert_pathat_to_path
 *
 * Function for translating a dirfd/path combination, as would typically
 * be passed to system calls such as openat() or mkdirat(), and
 * returning a regular pathname (either relative or absolute). Note that
 * this function does not make any attempt to normalize the path, so you
 * should still pass it through _cfs_combined_paths() before using it.
 *
 * - dirfd - The file descriptor parameter, as passed to openat() etc.
 * - pathname - The absolute or relative (to 'dirfd') path name.
 * - combined_path - A pointer to the return buffer, into which the
 *            returned path is written.
 *
 * Returns 0 on success, or -1 on error. Note that the returned path
 * may either be absolute or relative to the CWD.
 *======================================================================*/

int
_cfs_convert_pathat_to_path(int dirfd, const char *pathname, char *combined_path)
{
	int tmp_errno = errno;

	/*
	 * Case 1: pathname is absolute.
	 * Case 2: dirfd is AT_FDCWD - relative to current directory.
	 */
	if ((pathname[0] == '/') || (dirfd == AT_FDCWD)) {
		strncpy(combined_path, pathname, PATH_MAX);
	}

	/* case 3: dirfd is a valid directory descriptor */
	else {

		/*
		 * Figure out the directory that dirfd refers to, then concatenate
		 * pathname onto it.
		 */
		if (_cfs_get_path_of_dirfd(combined_path, dirfd, pathname) == -1){
			/* couldn't determine directory for this dirfd. */
			errno = tmp_errno;
			return -1;
		}
	}
	errno = tmp_errno;
	return 0;
}


/*======================================================================
 * Helper: _cfs_get_path_of_fd
 *
 * Return the file system path associated with the specific file
 * descriptor. If the path that was used to open the fd is known, that
 * path will be return via the "path" array, and 0 is returned. On
 * failure to get the path, -1 is return.
 *
 * Note that not all OS's will support this feature, so a return code
 * of -1 might just mean that the information wasn't available.
 *
 * - fd - the open file descriptor we're querying.
 * - path - a buffer into which we'll write the fd's associate path.
 *          Must be PATH_MAX bytes long.
 *
 * Returns 0 on success, or -1 on error. Note that the returned path
 * may either be absolute or relative to the CWD.
 *======================================================================*/


int
_cfs_get_path_of_fd(int fd, char *path)
{
	int tmp_errno = errno;
	char procName[strlen("/proc/self/fd/NNNNNNN\0")];

	sprintf(procName, "/proc/self/fd/%d", fd);
	int len = readlink(procName, path, PATH_MAX - 1);
	if (len == -1){
		/* for some reason, /proc/self/fd/%d is not available */
		return -1;
	}
	path[len] = '\0';

	/*
	 * Sanity check - that path should start with '/', rather than
	 * something like "pipe:"
	 */
	if (path[0] != '/') {
		return -1;
	}

	/* OK, the "path" array should contain the fd's path */
	errno = tmp_errno;
	return 0;
}

/*======================================================================*/
