/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.outline.dialogs;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import com.buildml.eclipse.utils.BmlTitleAreaDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.fieldeditors.VFSDirSelectFieldEditor;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * A Dialog allowing the user to edit a package's "source" and "generated" path roots.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ChangeRootsDialog extends BmlTitleAreaDialog implements IPropertyChangeListener {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The ID of the package whose roots will be edited */
	private int pkgId;
	
	/** The BuildStore we're acting upon */
	private IBuildStore buildStore;
	
	/** This BuildStore's FileMgr */
	private IFileMgr fileMgr;
	
	/** Set if the source path root editor field is currently valid */
	private boolean sourceValid = true;

	/** Set if the generated path root editor field is currently valid */
	private boolean generatedValid = true;
	
	/** The VFS editor widget for the source root */
	private VFSDirSelectFieldEditor sourceRootFieldEditor;

	/** The VFS editor widget for the generated root */
	private VFSDirSelectFieldEditor generatedRootFieldEditor;

	/** After OK is pressed, this hold the source root's path ID */
	private int sourceRootPathId;

	/** After OK is pressed, this hold the generated root's path ID */
	private int generatedRootPathId;
	
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new ChangeRootsDialog instance. 
	 * @param buildStore The BuildStore containing the root information.
	 * @param pkgId The package whose roots will be edited.
	 */
	public ChangeRootsDialog(IBuildStore buildStore, int pkgId) {
		super(new Shell());
		
		this.buildStore = buildStore;
		this.fileMgr = buildStore.getFileMgr();

		this.pkgId = pkgId;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return The VFS path ID for the source root. Available only after OK is pressed.
	 */
	public int getSourceRootPathId() {
		return sourceRootPathId;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The VFS path ID for the generated root. Available only after OK is pressed.
	 */
	public int getGeneratedRootPathId() {
		return generatedRootPathId;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Dynamically determine whether the current field entries refer to valid VFS paths.
	 * Provide visual feedback to the user in the form of an error message, as well as
	 * the enabling/disabling of the OK button.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		/* Determine which editor (source/generated) the change was made in */
		Object source = event.getSource();
		if (!(source instanceof VFSDirSelectFieldEditor)) {
			return;	
		}
		VFSDirSelectFieldEditor fieldEditor = (VFSDirSelectFieldEditor)source;

		/* Is it valid, or not? */
		String proposedPath = fieldEditor.getStringValue();
		boolean thisValid = (fileMgr.getPath(proposedPath) != ErrorCode.BAD_PATH);
		
		/* mark the appropriate field as valid/invalid, and update OK button accordingly */
		if (fieldEditor == sourceRootFieldEditor) {
			sourceValid = thisValid;
		} else {
			generatedValid = thisValid;
		}
		
		if (sourceValid && generatedValid) {
			setErrorMessage(null);
			getButton(OK).setEnabled(true);			
		} else {
			setErrorMessage("Invalid path.");
			getButton(OK).setEnabled(false);
		}
	}

	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		String pkgName = pkgMgr.getName(pkgId);
		
		setTitle("Select roots for package: " + pkgName);
		setHelpAvailable(false);
		
		Composite container = new Composite(parent, SWT.NULL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gd.verticalIndent = 20;
		gd.horizontalIndent = 10;
		container.setLayoutData(gd);
		
		/* the introductory label */
		Label label = new Label(container, SWT.WRAP);
		label.setText("Specify the 'Source File' root and the " +
				"'Generated File' root for the package. Package roots must encompass all the " +
				"package's file, and also must be below the @workspace root.\n");
		GridData labelGD = new GridData();
		labelGD.horizontalSpan = 3;
		labelGD.widthHint = EclipsePartUtils.getScreenWidth() / 3;
		label.setLayoutData(labelGD);
		
		/* create and initialize the "source root" field editor */
		sourceRootFieldEditor = new VFSDirSelectFieldEditor("Directory", "Source Root: ", container,
				buildStore, "Please select the 'Source File' root for: " + pkgName);
		int sourceRootId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT);
		sourceRootFieldEditor.setStringValue(fileMgr.getPathName(sourceRootId));
		sourceRootFieldEditor.setPropertyChangeListener(this);
		
		/* create and initialize the "generated root" field editor */
		generatedRootFieldEditor = new VFSDirSelectFieldEditor("Directory", "Generated Root: ", container,
				buildStore, "Please select the 'Generated File' root for: " + pkgName);
		int generatedRootId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT);
		generatedRootFieldEditor.setStringValue(fileMgr.getPathName(generatedRootId));
		generatedRootFieldEditor.setPropertyChangeListener(this);
		
		return parent;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {

		/* save the path values, so our caller can access them */
		sourceRootPathId = fileMgr.getPath(sourceRootFieldEditor.getStringValue());
		generatedRootPathId = fileMgr.getPath(generatedRootFieldEditor.getStringValue());
		
		/* now dispose the window */
		super.okPressed();
	}

	/*-------------------------------------------------------------------------------------*/

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Select Package Path Roots");
	}	

	/*-------------------------------------------------------------------------------------*/
}
