package com.buildml.eclipse.preferences;

/**
 * Constant definitions for plug-in preferences. These constants are used in
 * various parts of the code for accessing user-settable preferences.
 */
public class PreferenceConstants {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** 
	 * Preference: whether or not the file editor should display directory hierarchies
	 * on a single line. For example a/b/c is show on a single line, as opposed to
	 * three separate lines in the directory tree.
	 */
	public static final String PREF_COALESCE_DIRS = "coalesceDirectoryPreference";
	
	/**
	 * Preference: the directory location where the BuildML "bin" and "lib" directories
	 * can be found. This is necessary so that executable binary and dynamically loadable
	 * libraries can be found.
	 */
	public static final String PREF_BUILDML_HOME = "buildmlHomeDirectory";
	
	/*-------------------------------------------------------------------------------------*/
}
