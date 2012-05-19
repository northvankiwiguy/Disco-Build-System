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

package com.buildml.utils.string;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

/**
 * This class provides various utility functions for manipulating file system
 * paths, and their path components. All the methods in this class are static,
 * so they're essentially just worker functions without any state.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class PathUtils {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a single component of a file system path, validate that it's well formed. This
	 * ensures that the name doesn't contain / or \, and that it's not empty, . or ..
	 * 
	 * @param pathComponent The path component to validate.
	 * @return True if name is valid, else false.
	 */
	public static boolean validatePathComponent(String pathComponent) {
		return !((pathComponent == null) ||
				  (pathComponent.isEmpty()) ||
				  (pathComponent.contains("/")) ||
				  (pathComponent.contains("\\")) ||
			      (pathComponent.equals(".")) ||
			      (pathComponent.equals("..")));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given an absolute path (starting with '/'), normalize it so that there
	 * are no ".." or "." components, and no excess / characters. Note that unlike
	 * File.getCanonicalPath(), we do not access the underlying file system
	 * (this allows us to process content on a different machine from which the
	 * path actually exists).
	 * 
	 * @param curDir The non-normalized path.
	 * @return The normalized path.
	 */
	public static String normalizeAbsolutePath(String curDir) {

		/*
		 * This algorithm involves scanning through a StringBuffer (buf) and
		 * keep track of the various '/' characters found (these delineate the
		 * various path components). There are several cases to handle: 1)
		 * Component "." is found - simply delete it and keep scanning. 2)
		 * Component "/" is found - this is an excessive /, and can also be
		 * deleted. 3) Component ".." is found - we need to backtrack and delete
		 * the previous component as well.
		 * 
		 * We use two pointers (thisIndex, nextIndex) to track the start and end
		 * of each component. We also use a Stack (slashStack) to record the
		 * positions of all previous / characters, in case we need to backtrack.
		 */
		if (curDir == null) {
			return null;
		}
		StringBuffer buf = new StringBuffer(curDir);
		int maxIndex = buf.length();
		int thisIndex = buf.indexOf("/");
		Stack<Integer> slashStack = new Stack<Integer>();

		/*
		 * While not at the end of the string yet, keep looking for more
		 * components.
		 */
		while ((thisIndex != -1) && (thisIndex != maxIndex)) {

			/*
			 * Find the next / character. If there aren't any more, use the end
			 * of string. This will allow us to find the next "component"
			 * (between slashes).
			 */
			int nextIndex = buf.indexOf("/", thisIndex + 1);
			if (nextIndex == -1) {
				nextIndex = maxIndex;
			}
			String pathPart = buf.substring(thisIndex, nextIndex);

			/* a path component of "/." or "/" should be removed */
			if (pathPart.equals("/.") || pathPart.equals("/")) {
				buf.delete(thisIndex, nextIndex);
				maxIndex -= (nextIndex - thisIndex);

				/* a path component of "/.." involves going backwards */
			} else if (pathPart.equals("/..")) {

				/*
				 * Find the index of the previous component's starting point by
				 * popping the stack. If we go off the end of the stack, use
				 * index 0 instead. This allows for paths like "/../.." that are
				 * still legal in Linux.
				 */
				int lastIndex;
				try {
					lastIndex = slashStack.pop();
				} catch (EmptyStackException ex) {
					lastIndex = 0;
				}
				buf.delete(lastIndex, nextIndex);
				maxIndex -= (nextIndex - lastIndex);
				thisIndex = lastIndex;

			} else {
				/*
				 * normal case - record the slash position (in case we see a
				 * future ..) and move on
				 */
				slashStack.push(Integer.valueOf(thisIndex));
				thisIndex = nextIndex;
			}
		}
		/*
		 * Return the normalized string. Note the special cases of "/.." that
		 * should result in "/".
		 */
		if (buf.length() == 0) {
			return "/";
		} else {
			return buf.toString();
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a file system path is absolute or relative.
	 * 
	 * @param path The path in question.
	 * @return True if absolute, else false.
	 */
	public static boolean isAbsolutePath(String path) {

		return ((path != null) && (!path.isEmpty()) && 
				((path.charAt(0) == '/') || (path.charAt(0) == '\\')));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a path-like string, break it into separate file/directory
	 * component names. For example, "/a/b/c" will be broken into {"a", "b", "c"}.
	 * 
	 * @param path The full path
	 * @return An array of String, one element for each component.
	 */
	public static String[] tokenizePath(String path) {
		List<String> resultList = new ArrayList<String>();
	
		/* null path return an empty array */
		if (path == null) {
			return new String[]{};
		}
		
		/*
		 * Traverse the path string, dividing it on / boundaries.
		 */
		int startIndex = 0;
		int endIndex;
		
		boolean done = false;
		do {
			/*
			 * Find the next / in the string, until the end of string
			 * is reached.
			 */
			int slashIndex = path.indexOf('/', startIndex);
			if (slashIndex == -1){
				endIndex = path.length();
				done = true;
			} else {
				endIndex = slashIndex;
			}
			
			/* if there's some content in the path component */
			if (endIndex > startIndex) {
				String cmpt = path.substring(startIndex, endIndex);
				resultList.add(cmpt);
			}
			startIndex = slashIndex + 1;
						
		} while (!done);
			
		/* return the resulting list of path components */
		return resultList.toArray(new String[0]);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given an array of file system paths, detect whether this specific path falls within
	 * one of those directories (known as the "root"). If not, return null. If so, return
	 * the portion of the path with the root removed.
	 * <p>
	 * For example, with pathRoots equal to { "/home/fred/", "/tmp" } and path equal to 
	 * "/home/fred/src/foo.c", return "src/foo.c".
	 * 
	 * @param pathRoots An array of path roots, in the form of absolute paths.
	 * @param path An absolute path that may or may not fall within one of the roots.
	 * @return The portion of the path, with the matching root extracted, or null if the
	 * path isn't within one of the roots.
	 */
	public static String matchPathRoot(String [] pathRoots, String path) {
		
		for (String root : pathRoots) {
			
			/* this path matches the root? */
			if (path.startsWith(root)) {
				return path.substring(root.length());
			}
		}
		
		/* no match */
		return null;
		
	}
	
	/*-------------------------------------------------------------------------------------*/
}

