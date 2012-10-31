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

#include <jni.h>
#include <unistd.h>
#include <limits.h>
#include <errno.h>

#include "com_buildml_utils_os_SystemUtils.h"


/*
 * Helper function for throwing an exception.
 */
void
JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg)
{
	jclass cls = (*env)->FindClass(env, name);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
    	(*env)->ThrowNew(env, cls, msg);
    }
    /* free the local ref */
    (*env)->DeleteLocalRef(env, cls);
}


/*
 * Class:     com_buildml_utils_os_SystemUtils
 * Method:    isSymlink
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_buildml_utils_os_SystemUtils_isSymlink
  (JNIEnv *env, jobject jobj, jstring fileName)
{
	/* NULL pointers not allowed */
	if (fileName == NULL){
		JNU_ThrowByName(env, "java/lang/NullPointerException", "null file name not permitted.");
		return 0;
	}

	/* fetch the Java UTF string into a C-style string */
    jboolean iscopy;
    const char *cFileName = (*env)->GetStringUTFChars(env, fileName, &iscopy);

    /* attempt to read the destination of this link - rc < 0 is an error */
    char buf[1];
    ssize_t rc = readlink(cFileName, buf, 1);

    /* free memory used by strings */
    (*env)->ReleaseStringUTFChars(env, fileName, cFileName);

    /* was the file not found? */
    if (rc < 0){
    	if ((errno == EACCES) || (errno == ENOENT) || (errno == ENOTDIR)){
    		JNU_ThrowByName(env, "java/io/FileNotFoundException", "File could not be found.");
    		return 0;
    	}
    }

    /* if the rc > 0, a name was available, which means this is a symlink */
    return (rc > 0);
}

/*
 * Class:     com_buildml_utils_os_SystemUtils
 * Method:    readSymlink
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_buildml_utils_os_SystemUtils_readSymlink
  (JNIEnv *env, jobject jobj, jstring fileName)
{
	/* NULL pointers not allowed */
	if (fileName == NULL){
		JNU_ThrowByName(env, "java/lang/NullPointerException", "null file name not permitted.");
		return NULL;
	}

	/* fetch the Java UTF string into a C-style string */
    jboolean iscopy;
    const char *cFileName = (*env)->GetStringUTFChars(env, fileName, &iscopy);

	/* read the destination of this symlink */
    char buf[PATH_MAX + 1];
    ssize_t rc = readlink(cFileName, buf, PATH_MAX);

    /* insert a NUL character at the end */
    if (rc > 0) {
    	buf[rc] = '\0';
    }

    /* free memory used by strings */
    (*env)->ReleaseStringUTFChars(env, fileName, cFileName);

    /* was the file not found? */
    if (rc < 0){
    	if ((errno == EACCES) || (errno == ENOENT) || (errno == ENOTDIR)){
    		JNU_ThrowByName(env, "java/io/FileNotFoundException", "File could not be found.");
    		return NULL;
    	}
    }

	/* return the symlink target, after converting it to UTF */
    if (rc > 0) {
    	return((*env)->NewStringUTF(env, buf));
    }

    /* on any error, return NULL */
    else {
    	return NULL;
    }
}

/*
 * Class:     com_buildml_utils_os_SystemUtils
 * Method:    createSymlink
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT jint JNICALL Java_com_buildml_utils_os_SystemUtils_createSymlink
  (JNIEnv *env, jobject jobj, jstring fileName, jstring targetFileName)
{
	/* NULL pointers not allowed */
	if ((fileName == NULL) || (targetFileName == NULL)) {
		JNU_ThrowByName(env, "java/lang/NullPointerException", "null file name not permitted.");
		return 0;
	}

    jboolean iscopy;
    const char *cFileName = (*env)->GetStringUTFChars(env, fileName, &iscopy);
    const char *cTargetFileName = (*env)->GetStringUTFChars(env, targetFileName, &iscopy);

    int rc = symlink(cTargetFileName, cFileName);

    /* free memory used by strings */
    (*env)->ReleaseStringUTFChars(env, fileName, cFileName);
    (*env)->ReleaseStringUTFChars(env, targetFileName, cTargetFileName);

    return rc;
}

/*
 * Class:     com_buildml_utils_os_SystemUtils
 * Method:    chmod
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_buildml_utils_os_SystemUtils_chmod
  (JNIEnv *env, jclass clazz, jstring fileName, jint mode)
{
	/* NULL pointers not allowed */
	if (fileName == NULL) {
		JNU_ThrowByName(env, "java/lang/NullPointerException", "null file name not permitted.");
		return 0;
	}

    jboolean iscopy;
    const char *cFileName = (*env)->GetStringUTFChars(env, fileName, &iscopy);

    int rc = chmod(cFileName, mode);

    /* free memory used by strings */
    (*env)->ReleaseStringUTFChars(env, fileName, cFileName);

    return rc;
}



