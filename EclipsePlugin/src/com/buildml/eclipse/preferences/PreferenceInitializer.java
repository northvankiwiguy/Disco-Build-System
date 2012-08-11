package com.buildml.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.buildml.eclipse.Activator;

/**
 * Class used to initialize default preference values for all preference
 * pages in the BuildML plugin.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		/* Set defaults for preferences on the "Appearance" page. */
		store.setDefault(PreferenceConstants.PREF_COALESCE_DIRS, true);
		
		// TODO: set the default for PreferenceConstants.PREF_BUILDML_HOME, which should be
		// somewhere within the Eclipse plugin directory.
	}
	
	/*-------------------------------------------------------------------------------------*/
}
