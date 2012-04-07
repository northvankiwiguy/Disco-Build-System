package com.arapiki.disco.eclipse.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import com.arapiki.disco.eclipse.Activator;

/**
 * This class provides the Disco editor preference management for 
 * the Disco/Appearance page.
 */
public class DiscoAppearancePreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new preference page.
	 */
	public DiscoAppearancePreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Appearance of Disco editors and views.");
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

		addField(
			new BooleanFieldEditor(
				PreferenceConstants.PREF_SHOW_ROOTS,
				"Show file path &roots at the top level of file editors.",
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