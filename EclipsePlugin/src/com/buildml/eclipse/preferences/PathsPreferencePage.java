package com.buildml.eclipse.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

import com.buildml.eclipse.Activator;

/**
 * This class provides the BuildML editor preference management for 
 * the BuildML/Paths page.
 */
public class PathsPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new preference page.
	 */
	public PathsPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Paths for BuildML editors and views.");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Creates the field editors for this page (one editor per preference).
	 */
	public void createFieldEditors() {

		Composite parent = getFieldEditorParent();
		
		Label buildMlHomeLabel = new Label(parent, SWT.NONE);
		buildMlHomeLabel.setText("Directory containing BuildML's bin and lib directories " +
				"(leave blank to use built-in path):");
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd.verticalIndent = 10;
		buildMlHomeLabel.setLayoutData(gd);
		
		addField(new DirectoryFieldEditor(PreferenceConstants.PREF_BUILDML_HOME,
				"", parent));
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/*-------------------------------------------------------------------------------------*/
	
}