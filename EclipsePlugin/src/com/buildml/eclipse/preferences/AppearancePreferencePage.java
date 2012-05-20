package com.buildml.eclipse.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

import com.buildml.eclipse.Activator;

/**
 * This class provides the BuildML editor preference management for 
 * the BuildML/Appearance page.
 */
public class AppearancePreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new preference page.
	 */
	public AppearancePreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Appearance of BuildMl editors and views.");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Creates the field editors for this page (one editor per preference).
	 */
	public void createFieldEditors() {

		addField(
			new BooleanFieldEditor(
				PreferenceConstants.PREF_COALESCE_DIRS,
				"To save space, &coalesce directory hierarchies onto a single line.",
				getFieldEditorParent()));
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/*-------------------------------------------------------------------------------------*/
	
}