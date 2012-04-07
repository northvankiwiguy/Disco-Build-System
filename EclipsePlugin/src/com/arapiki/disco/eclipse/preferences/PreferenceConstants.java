package com.arapiki.disco.eclipse.preferences;

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
	 * Preference: whether or not file path roots should be displayed
	 * in a file editor (otherwise only the path name is displayed).
	 */
	public static final String PREF_SHOW_ROOTS = "showRootsPreference";
	
	/*-------------------------------------------------------------------------------------*/
}
