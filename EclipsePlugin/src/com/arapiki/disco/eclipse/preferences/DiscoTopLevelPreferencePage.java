package com.arapiki.disco.eclipse.preferences;

import java.util.Calendar;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.arapiki.disco.eclipse.Activator;
import com.arapiki.utils.version.Version;

/**
 * This class represents a preference page that is contributed to the Preferences 
 * dialog. This is the top-level preference page for Disco, which simply displays
 * an informational message about the tool.
 */

public class DiscoTopLevelPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new DiscoTopLevelPreferencePage.
	 */
	public DiscoTopLevelPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Preferences for the Disco Build System.");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		/* there are no editors in this page - just the content created by createContents() */
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {

		/* Create a new composite, which has a 1 x 1 grid */
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 20;
		layout.marginWidth = 10;
		composite.setLayout(layout);

		/* add a single (wrappable) label into the grid */
		Label l1 = new Label(composite, SWT.WRAP);
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		l1.setText("Disco is a build tool for managing the software build process using " +
				"UML-like diagrams.\n\nCopyright (C) " + year + " Arapiki Solutions Inc.\n\n" +
		        "Disco version: " + Version.getVersionNumber() + "\n" +
				"Information: http://disco.arapiki.com\n" +
				"Contact: disco@arapiki.com\n\n" +
				"Preferences can be set on child pages.");
		GridData gridData = new GridData();
		gridData.widthHint = 350;
		l1.setLayoutData(gridData);
		
		/* return the content that we just created. */
		return composite;
	}
	
	/*-------------------------------------------------------------------------------------*/
}