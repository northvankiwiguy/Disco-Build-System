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

#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <libgen.h>
#include <limits.h>
#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ipc.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

/* include helper macros */
#include "cunit_helper.h"

/* include our test helper functions */
#include "test_helpers.h"

/*
 * This test suite validates that system calls keep their normal functionality, even
 * when we're monitoring their file-access behavior.
 */

/*
 * The "environ" global is used by a number of functions that modify the environment
 * with setenv.
 */
extern char **environ;

/*
 * Name of our temporary working directory - used by any test cases
 * that need file storage.
 */
static char tmp_dir[] = "/tmp/buildml-cunitXXXXXX";

/*======================================================================
 * setup - Set-up function for this suite
 *
 * In the setup phase, we create a temporary directory which all the
 * test cases in this suite are welcome to use for storing files. Test
 * cases may create a sub-directory of this temporary directory, if
 * necessary. Note that a single temporary directory is shared between
 * all test cases, so they must take care to use non-conflicting names.
 *======================================================================*/

static int setup(void)
{
	fprintf(stderr, "\n");
	mkdtemp(tmp_dir);
	if (tmp_dir == NULL) {
		perror("Failed to create temporary directory.");
		return 1;
	}
	if (chdir(tmp_dir) != 0){
		fprintf(stderr, "Failed to set current working directory to %s.", tmp_dir);
		return 1;
	}
	printf("\n\nUsing temporary directory: %s\n", tmp_dir);
	return 0;
}

/*======================================================================
 * teardown - Tear-down function for this suite
 *======================================================================*/

static int teardown(void)
{
	char cmd[PATH_MAX];

	chdir("/");
	sprintf(cmd, "rm -r %s", tmp_dir);
	if (system(cmd) != 0){
		perror("Failed to remove temporary directory.");
		return 1;
	}
	printf("\n\nRemoved temporary directory: %s\n", tmp_dir);
	return 0;
}

/*======================================================================
 * Helper function - test_access_cmn(). Shared between test_access,
 * test_eaccess and test_euidaccess.
 *======================================================================*/

static void
test_access_cmn(int (*func)(const char *, int))
{
	/* check that the file doesn't exist yet */
	CU_ASSERT_EQUAL(func("access-file", F_OK), -1);

	/* create the file, and check that it's ONLY readable */
	CU_ASSERT(th_create_empty_file("access-file", 0444));
	CU_ASSERT_EQUAL(func("access-file", F_OK), 0);
	CU_ASSERT_EQUAL(func("access-file", R_OK), 0);
	errno = 0;
	CU_ASSERT_EQUAL(func("access-file", W_OK), -1);
	CU_ASSERT_EQUAL(errno, EACCES);
	errno = 0;
	CU_ASSERT_EQUAL(func("access-file", X_OK), -1);
	CU_ASSERT_EQUAL(errno, EACCES);

	/* check access on a missing file - should fail */
	errno = 0;
	CU_ASSERT_EQUAL(func("bad-access-file", F_OK), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);

	/* remove the access-file */
	CU_ASSERT_EQUAL(unlink("access-file"), 0);
}

/*======================================================================
 * Test Case - test_access()
 *======================================================================*/

static void
test_access(void)
{
	test_access_cmn(access);
}

/*======================================================================
 * Test Case - test_chdir()
 *======================================================================*/

static void
test_chdir(void)
{
	char *top_cwd = th_getcwd();

	CU_ASSERT_EQUAL(mkdir("mysubdir", 0755), 0);
	char subdir_name[PATH_MAX];
	sprintf(subdir_name, "%s/mysubdir", top_cwd);

	/* test relative chdir */
	CU_ASSERT_EQUAL(chdir("mysubdir"), 0);
	CU_ASSERT_STRING_EQUAL(th_getcwd(), subdir_name);

	/* test chdir to parent */
	CU_ASSERT_EQUAL(chdir(".."), 0);
	CU_ASSERT_STRING_EQUAL(th_getcwd(), top_cwd);

	/* test chdir to a non-existent directory */
	errno = 0;
	CU_ASSERT_EQUAL(chdir("doesnt-exist"), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
	CU_ASSERT_STRING_EQUAL(th_getcwd(), top_cwd);

	/* test chdir with an absolute path */
	CU_ASSERT_EQUAL(chdir(subdir_name), 0);
	CU_ASSERT_STRING_EQUAL(th_getcwd(), subdir_name);

	/* create a temporary file (not directory) and try to chdir to it */
	CU_ASSERT(th_create_empty_file("chdir-temp-file", 0666));
	errno = 0;
	CU_ASSERT_EQUAL(chdir("chdir-temp-file"), -1);
	CU_ASSERT_EQUAL(errno, ENOTDIR);
	CU_ASSERT_STRING_EQUAL(th_getcwd(), subdir_name);

	/*
	 * NOTE: this is for testing, we don't bother freeing memory from
	 * the get_current_dir_name() calls.
	 */
}

/*======================================================================
 * Test Case - test_chmod()
 *======================================================================*/

static void
test_chmod(void)
{
	/* set umask, to ensure that bits aren't masked */
	umask(0);

	/* create a new file, with 0666 permissions */
	CU_ASSERT(th_create_empty_file("chmod-temp-file", 0666));

	/* check the permissions are 0666 */
	CU_ASSERT_EQUAL(th_get_file_perms("chmod-temp-file"), 0666);

	/* chmod the permissions to 0755, and check them again */
	CU_ASSERT_EQUAL(chmod("chmod-temp-file", 0755), 0);
	CU_ASSERT_EQUAL(th_get_file_perms("chmod-temp-file"), 0755);

	/* try to chmod a non-existent file */
	errno = 0;
	CU_ASSERT_EQUAL(chmod("chmod-non-existent", 0755), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_chown()
 *
 * Not implemented: Can only be executed as superuser.
 *======================================================================*/

/*======================================================================
 * Test Case - test_creat()
 *======================================================================*/

static void
test_creat(void)
{
	/* check that our temp file doesn't yet exist */
	CU_ASSERT_EQUAL(access("creat-test-file", F_OK), -1);

	/* create the file, and check it again */
	int fd = creat("creat-test-file", 0444);
	CU_ASSERT_NOT_EQUAL(fd, -1);
	close(fd);
	CU_ASSERT_EQUAL(access("creat-test-file", F_OK), 0);

	/* try to creat an invalid file - should fail */
	errno = 0;
	CU_ASSERT_EQUAL(creat("/bad-file", 0444), -1);
	CU_ASSERT_EQUAL(errno, EACCES);
}

/*======================================================================
 * Test Case - test_creat64()
 *======================================================================*/

static void
test_creat64(void)
{
	/* check that our temp file doesn't yet exist */
	CU_ASSERT_EQUAL(access("creat64-test-file", F_OK), -1);

	/* create the file, and check it again */
	int fd = creat64("creat64-test-file", 0444);
	CU_ASSERT_NOT_EQUAL(fd, -1);
	close(fd);
	CU_ASSERT_EQUAL(access("creat64-test-file", F_OK), 0);

	/* try to creat an invalid file - should fail */
	errno = 0;
	CU_ASSERT_EQUAL(creat64("/bad-file", 0444), -1);
	CU_ASSERT_EQUAL(errno, EACCES);
}

/*======================================================================
 * Test Case - test_dlopen()
 *======================================================================*/

#if DISABLED

/*
 * NOTE: this dlopen function is easy to interpose, but a lot harder
 * to test. We'll avoid writing the test case, for now.
 */
static void
test_dlopen(void)
{
	CU_FAIL("Not implemented.");
}

#endif /* DISABLED */

/*======================================================================
 * Test Case - test_eaccess()
 *======================================================================*/

static void
test_eaccess(void)
{
	test_access_cmn(eaccess);
}

/*======================================================================
 * Test Case - test_euidaccess()
 *======================================================================*/

static void
test_euidaccess(void)
{
	test_access_cmn(euidaccess);
}

/*======================================================================
 * Helper - compile_child
 *
 * Compile a small child program that does nothing but validate its
 * command line arguments and a couple of environment variables. The
 * exit code from the program is:
 *    == 123 - indicates that the argv/envp array is valid.
 *    != 123 - error.
 * We use an odd return code (123), just to be convinced we're not
 * just getting a spurious value (as might happen with a 0 or 1 value).
 *
 * This function returns the name of the compiled program or NULL on
 * error.
 *======================================================================*/

static char *
compile_child()
{
	char *program_text =
			"#include <stdlib.h>\n"
			"int main(int argc, char *argv[]) {\n"
			"char *v1 = getenv(\"MY_TEST_VAR\");\n"
			"char *v2 = getenv(\"SECOND_VAR\");\n"
			"if (!v1 || !v2){ return -1; }\n"
			"if (strcmp(v1, \"Hello\") || strcmp(v2, \"42\")){ return -2; }\n"
			"if (argc != 4){ return -3; }\n"
			"if (strcmp(argv[1], \"dog\") || strcmp(argv[2], \"camel\") || strcmp(argv[3], \"bat\")){ return -4; }\n"
			"return 123;\n"
			"}";

	/* write out the source code */
	int fd = open("test-args.c", O_CREAT|O_WRONLY|O_TRUNC, 0755);
	if (fd == -1) {
		return NULL;
	}
	write(fd, program_text, strlen(program_text));
	close(fd);

	/* now compile the program into an executable program */
	int rc = system("gcc -o test-args test-args.c");
	if (rc != 0) {
		return NULL;
	}
	return "test-args";
}

/*======================================================================
 * Helper - test_exec_helper()
 *
 * This is a helper function that compiles a program to be executed,
 * creates a child process, then invokes a caller-supplied callback
 * function (func) that performs the appropriate exec() operation.
 * This will be one of execl, execv, etc.
 *
 * All of the test_exec*() functions take advantage of this helper.
 *======================================================================*/

static void
test_exec_helper(void (*func)(char *))
{
	int status;

	/* compile test program, returning path of the executable */
	char *prog = compile_child();
	CU_ASSERT_NOT_EQUAL(prog, NULL);

	/* fork/exec the test program */
	int child_pid = fork();
	switch (child_pid) {
	case 0:
		/* set some environment variables, which the "prog" will look for */
		setenv("MY_TEST_VAR", "Hello", 1);
		setenv("SECOND_VAR", "42", 1);

		/* child - invoke the callback function that will do the exec(). */
		func(prog);
		CU_FAIL("Returned from execl.");
		exit(0);

	case -1:
		/* error */
		CU_FAIL("Failed to fork off child process.");
		break;

	default:
		/* parent - wait for child's return code */
		CU_ASSERT_EQUAL(wait(&status), child_pid);
		CU_ASSERT_EQUAL(WEXITSTATUS(status), 123);
		break;
	}
}

/*======================================================================
 * Test Case - test_execl()
 *======================================================================*/

static void
test_execl_callback(char *prog)
{
	execl(prog, prog, "dog", "camel", "bat", (char *)0);
}


static void
test_execl(void)
{
	test_exec_helper(test_execl_callback);

	/* now try to invoke an invalid program, should fail */
	errno = 0;
	CU_ASSERT_EQUAL(execl("/bad-program", "/bad-program", "dog", "camel", "bat", (char *)0), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_execle()
 *======================================================================*/

static void
test_execle_callback(char *prog)
{
	execle(prog, prog, "dog", "camel", "bat", (char *)0, environ);
}

static void
test_execle(void)
{
	test_exec_helper(test_execle_callback);

	/* now try to invoke an invalid program, should fail */
	errno = 0;
	CU_ASSERT_EQUAL(execle("/bad-program", "/bad-program", "dog", "camel",
			"bat", (char *)0, environ), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_execlp()
 *======================================================================*/

static void
test_execlp_callback(char *prog)
{
	char *dir = dirname(prog);
	char *file = basename(prog);
	setenv("PATH", dir, 1);

	execlp(file, file, "dog", "camel", "bat", (char *)0);
}

static void
test_execlp(void)
{
	test_exec_helper(test_execlp_callback);

	/* now try to invoke an invalid program, should fail */
	errno = 0;
	CU_ASSERT_EQUAL(execlp("bad-program", "bad-program", "dog", "camel",
			"bat", (char *)0), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_execv()
 *======================================================================*/

static void
test_execv_callback(char *prog)
{
	char *argv[5];
	argv[0] = prog;
	argv[1] = "dog";
	argv[2] = "camel";
	argv[3] = "bat";
	argv[4] = 0;

	execv(prog, argv);
}

static void
test_execv(void)
{
	test_exec_helper(test_execv_callback);

	/* now try to invoke an invalid program, should fail */
	char *argv[2];
	argv[0] = "/bad-program";
	argv[1] = 0;
	errno = 0;
	CU_ASSERT_EQUAL(execv("/bad-program", argv), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_execve()
 *======================================================================*/

static void
test_execve_callback(char *prog)
{
	char *argv[5];
	argv[0] = prog;
	argv[1] = "dog";
	argv[2] = "camel";
	argv[3] = "bat";
	argv[4] = 0;

	execve(prog, argv, environ);
}

static void
test_execve(void)
{
	test_exec_helper(test_execve_callback);

	/* now try to invoke an invalid program, should fail */
	char *argv[2];
	argv[0] = "/bad-program";
	argv[1] = 0;
	errno = 0;
	CU_ASSERT_EQUAL(execve("/bad-program", argv, environ), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_execvp()
 *======================================================================*/

static void
test_execvp_callback(char *prog)
{
	char *argv[5];
	argv[0] = prog;
	argv[1] = "dog";
	argv[2] = "camel";
	argv[3] = "bat";
	argv[4] = 0;

	char *dir = dirname(prog);
	char *file = basename(prog);
	setenv("PATH", dir, 1);

	execvp(file, argv);
}

static void
test_execvp(void)
{
	test_exec_helper(test_execvp_callback);

	/* now try to invoke an invalid program, should fail */
	char *argv[2];
	argv[0] = "/bad-program";
	argv[1] = 0;
	errno = 0;
	CU_ASSERT_EQUAL(execvp("bad-program", argv), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_execvpe()
 *======================================================================*/

/* Note: execvpe is non-standard, so may not exist everywhere */
extern int execvpe(const char *file, char *const argv[], char *const envp[]);

void
test_execvpe_callback(char *prog)
{
	char *argv[5];
	argv[0] = prog;
	argv[1] = "dog";
	argv[2] = "camel";
	argv[3] = "bat";
	argv[4] = 0;

	char *dir = dirname(prog);
	char *file = basename(prog);
	setenv("PATH", dir, 1);

	execvpe(file, argv, environ);
}

static void
test_execvpe(void)
{
	test_exec_helper(test_execvpe_callback);

	/* now try to invoke an invalid program, should fail */
	char *argv[2];
	argv[0] = "/bad-program";
	argv[1] = 0;
	errno = 0;
	CU_ASSERT_EQUAL(execvpe("bad-program", argv, environ), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_exit()
 * Test Case - test__exit()
 * Test Case - test__Exit()
 *
 * Won't test, since it causes the program to exit, and we can't validate
 * the correctness of that.
 *======================================================================*/

/*======================================================================
 * Test Case - test_faccessat()
 *======================================================================*/


static void
test_faccessat(void)
{
	int dirfd = open(".", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(dirfd, -1);

	/* check that the file doesn't exist yet */
	CU_ASSERT_EQUAL(faccessat(dirfd, "access-file", F_OK, 0), -1);

	/* create the file, and check that it's ONLY readable */
	CU_ASSERT(th_create_empty_file("access-file", 0444));
	CU_ASSERT_EQUAL(faccessat(dirfd, "access-file", F_OK, 0), 0);
	CU_ASSERT_EQUAL(faccessat(dirfd, "access-file", R_OK, 0), 0);
	errno = 0;
	CU_ASSERT_EQUAL(faccessat(dirfd, "access-file", W_OK, 0), -1);
	CU_ASSERT_EQUAL(errno, EACCES);
	errno = 0;
	CU_ASSERT_EQUAL(faccessat(dirfd, "access-file", X_OK, 0), -1);
	CU_ASSERT_EQUAL(errno, EACCES);

	/* check access on a missing file - should fail */
	errno = 0;
	CU_ASSERT_EQUAL(faccessat(dirfd, "bad-access-file", F_OK, 0), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);

	/* remove the access-file */
	CU_ASSERT_EQUAL(unlink("access-file"), 0);
}

/*======================================================================
 * Test Case - test_fchdir()
 *======================================================================*/

static void
test_fchdir(void)
{
	/* create a subdirectory, and open it to get a file descriptor */
	CU_ASSERT_EQUAL(mkdir("fchdir-dir", 0755), 0);
	int dirfd = open("fchdir-dir", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(dirfd, -1);

	/* compute the full path name to that sub directory */
	char *top_cwd = th_getcwd();
	char expected_path[PATH_MAX];
	sprintf(expected_path, "%s/fchdir-dir", top_cwd);

	/* perform the fchdir operation */
	CU_ASSERT_EQUAL(fchdir(dirfd), 0);
	close(dirfd);

	/* check that our new cwd is what we expected */
	char *new_cwd = th_getcwd();
	CU_ASSERT_STRING_EQUAL(new_cwd, expected_path);

	/* try to fchdir to an invalid (0 == stdin) - should fail */
	errno = 0;
	CU_ASSERT_EQUAL(fchdir(0), -1);
	CU_ASSERT_EQUAL(errno, ENOTDIR);
}

/*======================================================================
 * Test Case - test_fchmod()
 *======================================================================*/

static void
test_fchmod(void)
{
	/* create an empty file, then open an fd to it */
	CU_ASSERT(th_create_empty_file("fchmod-empty-file", 0400));
	int fd = open("fchmod-empty-file", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(fd, -1);

	/* execute fchmod on the file */
	CU_ASSERT_EQUAL(fchmod(fd, 0644), 0);
	close(fd);

	/* check the permissions again, they should have changed */
	CU_ASSERT_EQUAL(th_get_file_perms("fchmod-empty-file"), 0644);

	/* try to fchmod an invalid fd (-1) */
	errno = 0;
	CU_ASSERT_EQUAL(fchmod(-1, 0644), -1);
	CU_ASSERT_EQUAL(errno, EBADF);
}

/*======================================================================
 * Test Case - test_fchmodat()
 *======================================================================*/

static void
test_fchmodat(void)
{
	/* create a new sub-directory, and an empty file within it */
	CU_ASSERT_EQUAL(mkdir("fchmodat-dir", 0755), 0);
	CU_ASSERT(th_create_empty_file("fchmodat-dir/file", 0444));

	/* test fchmodat of the file via an absolute path name */
	char abs_path[PATH_MAX];
	sprintf(abs_path, "%s/fchmodat-dir/file", th_getcwd());
	CU_ASSERT_EQUAL(fchmodat(0, abs_path, 0644, 0), 0);
	CU_ASSERT_EQUAL(th_get_file_perms("fchmodat-dir/file"), 0644);

	/* test fchmodat relative to current directory */
	CU_ASSERT_EQUAL(fchmodat(AT_FDCWD, "fchmodat-dir/file", 0664, 0), 0);
	CU_ASSERT_EQUAL(th_get_file_perms("fchmodat-dir/file"), 0664);

	/* test fchmodat relative to a dirfd */
	int dirfd = open("fchmodat-dir", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(dirfd, -1);
	CU_ASSERT_EQUAL(fchmodat(dirfd, "file", 0666, 0), 0);
	CU_ASSERT_EQUAL(th_get_file_perms("fchmodat-dir/file"), 0666);
	close(dirfd);

	/* test fchmodat on fd which is not a directory */
	errno = 0;
	CU_ASSERT_EQUAL(fchmodat(0, "file", 0667, 0), -1);
	CU_ASSERT_EQUAL(errno, ENOTDIR);
}

/*======================================================================
 * Test Case - test_fchown()
 * Test Case - test_fchownat()
 *
 * Not implemented. Can only be executed by the superuser.
 *======================================================================*/

/*======================================================================
 * Test Case - test_fexecve()
 *======================================================================*/

static void
test_fexecve_callback(char *prog)
{
	char *argv[5];
	argv[0] = prog;
	argv[1] = "dog";
	argv[2] = "camel";
	argv[3] = "bat";
	argv[4] = 0;

	int fd = open(prog, O_RDONLY);
	CU_ASSERT_NOT_EQUAL(fd, -1);

	fexecve(fd, argv, environ);
}

static void
test_fexecve(void)
{
	test_exec_helper(test_fexecve_callback);

	/* now try to invoke an invalid program (fd == 0), should fail */
	char *argv[2];
	argv[0] = "/bad-program";
	argv[1] = 0;
	errno = 0;
	CU_ASSERT_EQUAL(fexecve(0, argv, environ), -1);
	CU_ASSERT_EQUAL(errno, EACCES);
}

/*======================================================================
 * Helper Function - test_fopen_cmn
 *
 * A common test function for any fopen-like function call.
 *======================================================================*/

static void
test_fopen_cmn(FILE * (*func)(const char *, const char *))
{
	static char *tmp_file = "fopen-non-existent-file";

	/* open a non-existent temporary file for read - should fail */
	errno = 0;
	CU_ASSERT_EQUAL(func(tmp_file, "r"), NULL);
	CU_ASSERT_EQUAL(errno, ENOENT);

	/* open a non-existent temporary file for write|create - should succeed */
	FILE *file1 = func(tmp_file, "w+");
	CU_ASSERT_NOT_EQUAL(file1, NULL);
	fclose(file1);

	/* open the newly created file in read mode - should succeed */
	FILE *file2 = func(tmp_file, "r");
	CU_ASSERT_NOT_EQUAL(file2, NULL);
	fclose(file2);

	/* remove the temporary file. */
	unlink(tmp_file);
}

/*======================================================================
 * Test Case - test_fopen()
 *======================================================================*/

static void
test_fopen(void)
{
	test_fopen_cmn(fopen);
}

/*======================================================================
 * Test Case - test_fopen64()
 *======================================================================*/

/*
 * fopen64 is only defined in header files if large files are enabled.
 * We therefore need to explicitly define it here.
 */
extern FILE *fopen64 (__const char *__restrict __filename,
                      __const char *__restrict __modes);

static void
test_fopen64(void)
{
	test_fopen_cmn(fopen64);
}

/*======================================================================
 * Test Case - test_fork()
 *
 * Won't test this explicitly. The exec() testing already covers this
 * feature.
 *======================================================================*/

/*======================================================================
 * Helper Function - test_freopen_cmn
 *
 * A common test function for any freopen-like function call.
 *======================================================================*/

#if DISABLED

/*
 * Note that for some reason this test function crashes on fclosing the
 * stream. I believe this is due to their being multiple versions of
 * the fopen/fclose function in the glibc library, and I'm picking up
 * the wrong version. However, it does appear that fopen/freopen/fclose
 * works correctly when running "bml scan-build", so we may just need
 * to live without a unit test.
 */

static void
test_freopen_cmn(FILE * (*func)(const char *, const char *, FILE *))
{
	static char *tmp_file1 = "freopen-non-existent-file";
	static char *tmp_file2 = "freopen-second-file";

	/*
	 * Open a non-existent temporary file for write|create, using the
	 * normal open function - should succeed.
	 */
	FILE *file1 = fopen(tmp_file1, "w+");
	CU_ASSERT_NOT_EQUAL(file1, NULL);
	if (file1 == NULL) {
		return;
	}

	/* now use freopen to open another file */
	FILE *file2 = freopen(tmp_file2, "w+", file1);
	if (file2 == NULL) {
		return;
	}

	CU_ASSERT_EQUAL(file1, file2);

	CU_ASSERT_EQUAL(fclose(file2), 0);
	CU_ASSERT_EQUAL(fclose(file1), -1);

	/* remove the temporary file. */
	unlink(tmp_file1);
	unlink(tmp_file2);
}

/*======================================================================
 * Test Case - test_freopen()
 *======================================================================*/

static void
test_freopen(void)
{
	test_freopen_cmn(freopen);
}

/*======================================================================
 * Test Case - test_freopen64()
 *======================================================================*/

/*
 * freopen64 is only defined in header files if large files are enabled.
 * We therefore need to explicitly define it here.
 */
extern FILE *freopen64 (__const char *__restrict __filename,
                      __const char *__restrict __modes,
                      FILE *__restrict __stream);

static void
test_freopen64(void)
{
	test_freopen_cmn(freopen64);
}

#endif /* DISABLED */

/*======================================================================
 * Test Case - test_ftok()
 *======================================================================*/

static void
test_ftok(void)
{
	/* create a temporary file */
	CU_ASSERT(th_create_empty_file("ftok-file", 0644));

	/* create a key for that file, and ID 1 - should return a key. */
	key_t k1 = ftok("ftok-file", 1);
	CU_ASSERT_NOT_EQUAL(k1, -1);

	/* create a key for that file, and ID 2 - should return a different key. */
	key_t k2 = ftok("ftok-file", 2);
	CU_ASSERT_NOT_EQUAL(k2, -1);
	CU_ASSERT_NOT_EQUAL(k1, k2);

	/* create a key for the file, and ID1 again - should return same key */
	key_t k3 = ftok("ftok-file", 1);
	CU_ASSERT_EQUAL(k1, k3);

	/* create a key for an invalid file - should fail */
	errno = 0;
	key_t k4 = ftok("ftok-file-missing", 1);
	CU_ASSERT_EQUAL(k4, -1);
	CU_ASSERT_EQUAL(errno, ENOENT);

}

/*======================================================================
 * Test Case - test_lchown()
 *
 * Not implemented. Can only be executed by the superuser.
 *======================================================================*/

/*======================================================================
 * Test Case - test_link()
 *======================================================================*/

static void
test_link(void)
{
	const char *test_string = "testdata";

	/* create a file, with a small amount of content in it */
	int fd = open("link-tofile", O_CREAT|O_RDWR, 0444);
	CU_ASSERT_NOT_EQUAL(fd, -1);
	write(fd, test_string, strlen(test_string) + 1);
	CU_ASSERT_EQUAL(close(fd), 0);

	/* create a link to that file */
	CU_ASSERT_EQUAL(link("link-tofile", "link-thelink"), 0);

	/* read the linked file, and we should see the same content */
	fd = open("link-thelink", O_RDONLY);
	char buffer[strlen(test_string) + 1];
	read(fd, buffer, sizeof(buffer) + 1);
	CU_ASSERT_STRING_EQUAL(buffer, test_string);
	close(fd);

	/* try to create a link to an invalid path */
	errno = 0;
	CU_ASSERT_EQUAL(link("link-bad-file", "link-thelink2"), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_linkat()
 *======================================================================*/

static void
test_linkat(void)
{
	CU_ASSERT_EQUAL(mkdir("link-subdir1", 0755), 0);

	/* make a link, relative to link-subdir1 */
	int dirfd = open("link-subdir1", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(dirfd, -1);
	th_create_empty_file("link-subdir1/file", 0755);
	CU_ASSERT_EQUAL(access("link", R_OK | X_OK), -1);
	CU_ASSERT_EQUAL(linkat(dirfd, "file", AT_FDCWD, "link", 0), 0);
	CU_ASSERT_EQUAL(access("link", R_OK | X_OK), 0);
	CU_ASSERT_EQUAL(close(dirfd), 0);

	/* Create a link in a directory that we can't write into - should fail */
	int rootfd = open("/", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(rootfd, -1);
	errno = 0;
	CU_ASSERT_EQUAL(linkat(AT_FDCWD, "link-subdir1/file", rootfd, "link", 0), -1);
	CU_ASSERT_EQUAL(errno, EACCES);
	CU_ASSERT_EQUAL(close(rootfd), 0);
}

/*======================================================================
 * Test Case - test_mkdir()
 *======================================================================*/

static void
test_mkdir(void)
{
	/* try to cd into a directory that doesn't exist */
	CU_ASSERT_EQUAL(chdir("mkdir-dir"), -1);

	/* make the directory, and cd into it */
	CU_ASSERT_EQUAL(mkdir("mkdir-dir", 0755), 0);
	CU_ASSERT_EQUAL(chdir("mkdir-dir"), 0);

	/* cd back out again, then try to make the directory again */
	CU_ASSERT_EQUAL(chdir(".."), 0);
	errno = 0;
	CU_ASSERT_EQUAL(mkdir("mkdir-dir", 0755), -1);
	CU_ASSERT_EQUAL(errno, EEXIST);
}

/*======================================================================
 * Test Case - test_mkdirat()
 *======================================================================*/

static void
test_mkdirat(void)
{
	CU_ASSERT_EQUAL(mkdir("sub-dir1", 0755), 0);

	/* make a directory, relative to sub-dir1 */
	int dirfd = open("sub-dir1", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(dirfd, -1);
	CU_ASSERT_EQUAL(access("sub-dir1/sub-dir2", R_OK | X_OK), -1);
	CU_ASSERT_EQUAL(mkdirat(dirfd, "sub-dir2", 0755), 0);
	CU_ASSERT_EQUAL(access("sub-dir1/sub-dir2", R_OK | X_OK), 0);
	CU_ASSERT_EQUAL(close(dirfd), 0);

	/* make a directory, relative to the current directory */
	CU_ASSERT_EQUAL(access("sub-dir3", R_OK | X_OK), -1);
	CU_ASSERT_EQUAL(mkdirat(AT_FDCWD, "sub-dir3", 0755), 0);
	CU_ASSERT_EQUAL(access("sub-dir3", R_OK | X_OK), 0);

	/* try to make the same directory again - should fail */
	errno = 0;
	CU_ASSERT_EQUAL(mkdirat(AT_FDCWD, "sub-dir3", 0755), -1);
	CU_ASSERT_EQUAL(errno, EEXIST);
}

/*======================================================================
 * Helper Function - test_open_cmn
 *
 * A common test function for any open-like function call.
 *======================================================================*/

static void test_open_cmn(int (*func)(char *, int, ...))
{
	static char *tmp_file = "non-existent-file";

	/* open a non-existent temporary file for read - should fail */
	errno = 0;
	CU_ASSERT_EQUAL(func(tmp_file, O_RDONLY), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);

	/* open a non-existent temporary file for write|create - should succeed */
	int fd1 = func(tmp_file, O_CREAT|O_WRONLY, 0644);
	CU_ASSERT_NOT_EQUAL(fd1, -1);
	close(fd1);

	/* try to recreate the same file with O_EXCL set - should fail. */
	errno = 0;
	int fd2 = func(tmp_file, O_CREAT|O_WRONLY|O_EXCL, 0644);
	CU_ASSERT_EQUAL(fd2, -1);
	CU_ASSERT_EQUAL(errno, EEXIST);

	/* open the newly created file in read mode - should succeed */
	int fd3 = func(tmp_file, O_RDONLY, 0644);
	CU_ASSERT_NOT_EQUAL(fd3, -1);
	close(fd3);

	/* Check file access bits - should be 0644 */
	CU_ASSERT_EQUAL(th_get_file_perms(tmp_file), 0644);

	/* remove the temporary file. */
	unlink(tmp_file);
}

/*======================================================================
 * Test Case - test_open
 *======================================================================*/

static void
test_open(void)
{
	/* simply defer to a common function for all open-like calls */
	test_open_cmn((void *)open);
}

/*======================================================================
 * Test Case - test_open64
 *======================================================================*/

static void
test_open64(void)
{
	/* simply defer to a common function for all open-like calls */
	test_open_cmn((void *)open64);
}

/*======================================================================
 * Helper Function - test_openat_cmn
 *
 * A common test function for any openat-like function call.
 *======================================================================*/

static void test_openat_cmn(int (*func)(int, const char *, int, ...))
{
	system("rm -fr openat-subdir");
	CU_ASSERT_EQUAL(mkdir("openat-subdir", 0755), 0);
	int dirfd = open("openat-subdir", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(dirfd, -1);

	/* Create a file in a sub-directory, then open it */
	CU_ASSERT_EQUAL(access("openat-subdir/file", R_OK | X_OK), -1);
	int newfd1 = openat(dirfd, "file", O_CREAT|O_RDWR, 0755);
	CU_ASSERT_NOT_EQUAL(newfd1, -1);
	CU_ASSERT_EQUAL(access("openat-subdir/file", R_OK | X_OK), 0);
	CU_ASSERT_EQUAL(close(newfd1), 0);

	/* open the same file, relative to the CWD */
	int newfd2 = openat(AT_FDCWD, "openat-subdir/file", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(newfd2, -1);
	CU_ASSERT_EQUAL(close(newfd2), 0);

	/* try to open a file that doesn't exist */
	errno = 0;
	int newfd3 = openat(AT_FDCWD, "openat-subdir/non-file", O_RDONLY);
	CU_ASSERT_EQUAL(newfd3, -1);
	CU_ASSERT_EQUAL(errno, ENOENT);

	CU_ASSERT_EQUAL(close(dirfd), 0);
}

/*======================================================================
 * Test Case - test_openat()
 *======================================================================*/

static void
test_openat(void)
{
	test_openat_cmn(openat);
}

/*======================================================================
 * Test Case - test_openat64()
 *======================================================================*/

static void
test_openat64(void)
{
	test_openat_cmn(openat64);
}

/*======================================================================
 * Test Case - test_popen()
 *======================================================================*/

static void
test_popen(void)
{
	/* popen a simple command */
	FILE *finput = popen("echo Hi", "r");
	CU_ASSERT_NOT_EQUAL(finput, NULL);

	/* check that we receive the correct stdout from the process */
	char buffer[10];
	CU_ASSERT_EQUAL(fread(buffer, 1, sizeof(buffer), finput), 3);
	buffer[2] = '\0';
	CU_ASSERT_STRING_EQUAL(buffer, "Hi");
	CU_ASSERT_EQUAL(fclose(finput), 0);

	/* use a bad mode value */
	errno = 0;
	finput = popen("echo Hi", "xxx");
	CU_ASSERT_EQUAL(finput, NULL);
	CU_ASSERT_EQUAL(errno, EINVAL);
}

/*======================================================================
 * Test Case - test_posix_spawn()
 *======================================================================*/

static void
test_posix_spawn(void)
{
	/* compile test program, returning path of the executable */
	char *prog = compile_child();
	CU_ASSERT_NOT_EQUAL(prog, NULL);

	/* The child program expects these arguments and environment variables */
	char *argv[5];
	argv[0] = prog;
	argv[1] = "dog";
	argv[2] = "camel";
	argv[3] = "bat";
	argv[4] = 0;
	setenv("MY_TEST_VAR", "Hello", 1);
	setenv("SECOND_VAR", "42", 1);

	/* start the child process running */
	pid_t child_pid;
    CU_ASSERT_EQUAL(posix_spawn(&child_pid, prog, NULL, NULL, argv, environ), 0);

	/* parent - wait for child's return code */
	int status;
	CU_ASSERT_EQUAL(wait(&status), child_pid);
	CU_ASSERT_EQUAL(WEXITSTATUS(status), 123);
}

/*======================================================================
 * Test Case - test_posix_spawnp()
 *======================================================================*/

static void
test_posix_spawnp(void)
{
	/* compile test program, returning path of the executable */
	char *prog = compile_child();
	CU_ASSERT_NOT_EQUAL(prog, NULL);

	/* set up the PATH correctly, so posix_spawnp can find the program */
	char *dir = dirname(prog);
	char *file = basename(prog);
	char *saved_path = getenv("PATH");
	setenv("PATH", dir, 1);

	/* The child program expects these arguments and environment variables */
	char *argv[5];
	argv[0] = prog;
	argv[1] = "dog";
	argv[2] = "camel";
	argv[3] = "bat";
	argv[4] = 0;
	setenv("MY_TEST_VAR", "Hello", 1);
	setenv("SECOND_VAR", "42", 1);

	/* start the child process running */
	pid_t child_pid;
    CU_ASSERT_EQUAL(posix_spawnp(&child_pid, file, NULL, NULL, argv, environ), 0);

	/* parent - wait for child's return code */
	int status;
	CU_ASSERT_EQUAL(wait(&status), child_pid);
	CU_ASSERT_EQUAL(WEXITSTATUS(status), 123);

	/* restore the old PATH */
	setenv("PATH", saved_path, 1);
}

/*======================================================================
 * Test Case - test_remove()
 *======================================================================*/

static void
test_remove(void)
{
	/* make a directory */
	CU_ASSERT_EQUAL(mkdir("remove-dir", 0755), 0);

	/* remove that directory, and validate that it doesn't exist */
	CU_ASSERT_EQUAL(remove("remove-dir"), 0);
	CU_ASSERT_EQUAL(chdir("remove-dir"), -1);

	/* try to remove the directory again - should fail. */
	errno = 0;
	CU_ASSERT_EQUAL(remove("remove-dir"), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_rename()
 *======================================================================*/

static void
test_rename(void)
{
	/* create a file, and check it exists */
	CU_ASSERT(th_create_empty_file("rename-file1", 0666));
	CU_ASSERT_EQUAL(th_get_file_perms("rename-file1"), 0666);

	/* check that the second name doesn't exist */
	CU_ASSERT_EQUAL(th_get_file_perms("rename-file2"), -1);

	/* rename the file to have the second name */
	CU_ASSERT_EQUAL(rename("rename-file1", "rename-file2"), 0);

	/* check that the first file doesn't exist, but the second does */
	CU_ASSERT_EQUAL(th_get_file_perms("rename-file1"), -1);
	CU_ASSERT_EQUAL(th_get_file_perms("rename-file2"), 0666);

	/* try to rename a file that never existed */
	errno = 0;
	CU_ASSERT_EQUAL(rename("rename-file3", "rename-file4"), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_renameat()
 *======================================================================*/

static void
test_renameat(void)
{
	/* Create a file in a sub-directory, then link to it */
	CU_ASSERT_EQUAL(mkdir("renameat-subdir", 0755), 0);

	/* rename a file, relative to renameat-subdir */
	int dirfd = open("renameat-subdir", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(dirfd, -1);
	th_create_empty_file("renameat-subdir/file-source", 0755);
	CU_ASSERT_EQUAL(access("renameat-subdir/file-target", R_OK | X_OK), -1);
	CU_ASSERT_EQUAL(renameat(dirfd, "file-source", AT_FDCWD, "renameat-subdir/file-target"), 0);
	CU_ASSERT_EQUAL(access("renameat-subdir/file-source", R_OK | X_OK), -1);
	CU_ASSERT_EQUAL(access("renameat-subdir/file-target", R_OK | X_OK), 0);
	CU_ASSERT_EQUAL(close(dirfd), 0);

	/* Rename a file to a name that can't be created - should fail */
	int rootfd = open("/", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(rootfd, -1);
	errno = 0;
	CU_ASSERT_EQUAL(linkat(AT_FDCWD, "renameat-subdir/file-target", rootfd, "bad-file", 0), -1);
	CU_ASSERT_EQUAL(errno, EACCES);
	CU_ASSERT_EQUAL(close(rootfd), 0);
}

/*======================================================================
 * Test Case - test_rmdir()
 *======================================================================*/

static void
test_rmdir(void)
{
	/* make a directory */
	CU_ASSERT_EQUAL(mkdir("rmdir-dir", 0755), 0);

	/* remove that directory, and validate that it doesn't exist */
	CU_ASSERT_EQUAL(rmdir("rmdir-dir"), 0);
	CU_ASSERT_EQUAL(chdir("rmdir-dir"), -1);

	/* try to remove the directory again - should fail. */
	errno = 0;
	CU_ASSERT_EQUAL(rmdir("rmdir-dir"), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_symlink()
 *======================================================================*/

static void
test_symlink(void)
{
	const char *test_string = "testdata";

	/* create a file, with a small amount of content in it */
	int fd = open("symlink-tofile", O_CREAT|O_RDWR, 0444);
	CU_ASSERT_NOT_EQUAL(fd, -1);
	write(fd, test_string, strlen(test_string) + 1);
	CU_ASSERT_EQUAL(close(fd), 0);

	/* create a symlink to that file */
	CU_ASSERT_EQUAL(symlink("symlink-tofile", "symlink-thelink"), 0);

	/* read the linked file, and we should see the same content */
	fd = open("symlink-thelink", O_RDONLY);
	char buffer[strlen(test_string) + 1];
	read(fd, buffer, sizeof(buffer) + 1);
	CU_ASSERT_STRING_EQUAL(buffer, test_string);
	close(fd);

	/* try to create a symlink with a name that's already in use. */
	errno = 0;
	CU_ASSERT_EQUAL(symlink("symlink-to-file", "symlink-thelink"), -1);
	CU_ASSERT_EQUAL(errno, EEXIST);
}

/*======================================================================
 * Test Case - test_symlinkat()
 *======================================================================*/

static void
test_symlinkat(void)
{
	CU_ASSERT_EQUAL(mkdir("symlink-subdir", 0755), 0);

	/* make a symlink, relative to symlink-subdir */
	int dirfd = open("symlink-subdir", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(dirfd, -1);
	th_create_empty_file("file-to-symlink-to", 0755);
	CU_ASSERT_EQUAL(access("symlink-subdir/symlink", F_OK), -1);
	CU_ASSERT_EQUAL(symlinkat("../file-to-symlink-to", dirfd, "symlink"), 0);
	CU_ASSERT_EQUAL(access("symlink-subdir/symlink", F_OK), 0);
	CU_ASSERT_EQUAL(close(dirfd), 0);

	/* Create a link in a directory that we can't write into - should fail */
	int rootfd = open("/", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(rootfd, -1);
	errno = 0;
	CU_ASSERT_EQUAL(symlinkat("file-to-symlink-to", rootfd, "file"), -1);
	CU_ASSERT_EQUAL(errno, EACCES);
	CU_ASSERT_EQUAL(close(rootfd), 0);
}

/*======================================================================
 * Test Case - test_system()
 *======================================================================*/

static void
test_system(void)
{
	char *prog = compile_child();
	char cmd[PATH_MAX];

	setenv("MY_TEST_VAR", "Hello", 1);
	setenv("SECOND_VAR", "42", 1);
	sprintf(cmd, "./%s dog camel bat", prog);
	int rc = system(cmd);

	CU_ASSERT_EQUAL(WEXITSTATUS(rc), 123);

	unsetenv("MY_TEST_VAR");
	unsetenv("SECOND_VAR");
}

/*======================================================================
 * Test Case - test_truncate()
 *======================================================================*/

static void
test_truncate(void)
{
	/* create a file with a few characters in it */
	CU_ASSERT(th_create_nonempty_file("truncate-file",
			0666, "this is the content of my file"));

	/* truncate the file to length 10 */
	CU_ASSERT_EQUAL(truncate("truncate-file", 10), 0);

	/* check that the length is now 10 */
	CU_ASSERT_EQUAL(th_get_file_size("truncate-file"), 10);

	/* try to truncate a non-existent file */
	errno = 0;
	CU_ASSERT_EQUAL(truncate("truncate-badfile", 10), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_truncate64()
 *======================================================================*/

/* truncate64 isn't normally defined, so we define it ourselves */
extern int truncate64 (__const char *__file, __off64_t __length);

static void
test_truncate64(void)
{
	/* create a file with a few characters in it */
	CU_ASSERT(th_create_nonempty_file("truncate-file",
			0666, "this is the content of my file"));

	/* truncate the file to length 10 */
	CU_ASSERT_EQUAL(truncate64("truncate-file", 10), 0);

	/* check that the length is now 10 */
	CU_ASSERT_EQUAL(th_get_file_size("truncate-file"), 10);

	/* try to truncate a non-existent file */
	errno = 0;
	CU_ASSERT_EQUAL(truncate64("truncate-badfile", 10), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_unlink()
 *======================================================================*/

static void
test_unlink(void)
{
	/* create a temporary file */
	CU_ASSERT(th_create_empty_file("unlink-file", 0600));

	/* remove that file */
	CU_ASSERT_EQUAL(unlink("unlink-file"), 0);

	/* try to remove the file again */
	errno = 0;
	CU_ASSERT_EQUAL(unlink("unlink-file"), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Test Case - test_unlinkat()
 *======================================================================*/

static void
test_unlinkat(void)
{
	CU_ASSERT_EQUAL(mkdir("unlinkat-subdir", 0755), 0);
	th_create_empty_file("unlinkat-subdir/file1", 0755);
	th_create_empty_file("unlinkat-subdir/file2", 0755);

	/* unlink a file, relative to sub-dir1 */
	int dirfd = open("unlinkat-subdir", O_RDONLY);
	CU_ASSERT_NOT_EQUAL(dirfd, -1);
	CU_ASSERT_EQUAL(access("unlinkat-subdir/file1", R_OK | X_OK), 0);
	CU_ASSERT_EQUAL(unlinkat(dirfd, "file1", 0), 0);
	CU_ASSERT_EQUAL(access("unlinkat-subdir/file1", R_OK | X_OK), -1);
	CU_ASSERT_EQUAL(close(dirfd), 0);

	/* unlink at file, relative to the current directory */
	CU_ASSERT_EQUAL(access("unlinkat-subdir/file2", R_OK | X_OK), 0);
	CU_ASSERT_EQUAL(unlinkat(AT_FDCWD, "unlinkat-subdir/file2", 0), 0);
	CU_ASSERT_EQUAL(access("unlinkat-subdir/file2", R_OK | X_OK), -1);

	/* try to unlink the same file again - should fail */
	errno = 0;
	CU_ASSERT_EQUAL(unlinkat(AT_FDCWD, "unlinkat-subdir/file2", 0), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);
}

/*======================================================================
 * Main Function - init_regress_glibc_suite - main entry point for
 * initializing this test suite
 *======================================================================*/

int init_regress_glibc_suite()
{
	/* add a suite to the registry */
	NEW_TEST_SUITE("Regression tests for interposed glibc functions");

	/* add test cases */
    ADD_TEST_CASE(test_access, "access()");
    ADD_TEST_CASE(test_chdir, "chdir()");
    ADD_TEST_CASE(test_chmod, "chmod()");
    ADD_TEST_CASE(test_creat, "creat()");
    ADD_TEST_CASE(test_creat64, "creat64()");
#if DISABLED
    ADD_TEST_CASE(test_dlopen, "dlopen()");
#endif /* DISABLED */
    ADD_TEST_CASE(test_eaccess, "eaccess()");
    ADD_TEST_CASE(test_euidaccess, "euidaccess()");
    ADD_TEST_CASE(test_execl, "execl()");
    ADD_TEST_CASE(test_execle, "execle()");
    ADD_TEST_CASE(test_execlp, "execlp()");
    ADD_TEST_CASE(test_execv, "execv()");
    ADD_TEST_CASE(test_execve, "execve()");
    ADD_TEST_CASE(test_execvp, "execvp()");
    ADD_TEST_CASE(test_execvpe, "execvpe()");
    ADD_TEST_CASE(test_faccessat, "faccessat()");
    ADD_TEST_CASE(test_fchdir, "fchdir()");
    ADD_TEST_CASE(test_fchmod, "fchmod()");
    ADD_TEST_CASE(test_fchmodat, "fchmodat()");
    ADD_TEST_CASE(test_fexecve, "fexecve()");
    ADD_TEST_CASE(test_fopen, "fopen()");
    ADD_TEST_CASE(test_fopen64, "fopen64()");
#if DISABLED
    ADD_TEST_CASE(test_freopen, "freopen()");
    ADD_TEST_CASE(test_freopen64, "freopen64()");
#endif /* DISABLED */
    ADD_TEST_CASE(test_ftok, "ftok()");
    ADD_TEST_CASE(test_link, "link()");
    ADD_TEST_CASE(test_linkat, "linkat()");
    ADD_TEST_CASE(test_mkdir, "mkdir()");
    ADD_TEST_CASE(test_mkdirat, "mkdirat()");
    ADD_TEST_CASE(test_open, "open()");
	ADD_TEST_CASE(test_open64, "open64()");
    ADD_TEST_CASE(test_openat, "openat()");
    ADD_TEST_CASE(test_openat64, "openat64()");
    ADD_TEST_CASE(test_popen, "popen()");
    ADD_TEST_CASE(test_posix_spawn, "posix_spawn()");
    ADD_TEST_CASE(test_posix_spawnp, "posix_spawnp()");
    ADD_TEST_CASE(test_remove, "remove()");
    ADD_TEST_CASE(test_rename, "rename()");
    ADD_TEST_CASE(test_renameat, "renameat()");
    ADD_TEST_CASE(test_rmdir, "rmdir()");
    ADD_TEST_CASE(test_symlink, "symlink()");
    ADD_TEST_CASE(test_symlinkat, "symlinkat()");
    ADD_TEST_CASE(test_system, "system()");
    ADD_TEST_CASE(test_truncate, "truncate()");
    ADD_TEST_CASE(test_truncate64, "truncate64()");
    ADD_TEST_CASE(test_unlink, "unlink()");
    ADD_TEST_CASE(test_unlinkat, "unlinkat()");

	return 0;
}

/*======================================================================*/
