package com.arapiki.disco.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.arapiki.disco.eclipse.Activator;

/**
 * Class used to initialize default preference values for all preference
 * pages in the Disco plugin.
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
	}
	
	/*-------------------------------------------------------------------------------------*/
}
