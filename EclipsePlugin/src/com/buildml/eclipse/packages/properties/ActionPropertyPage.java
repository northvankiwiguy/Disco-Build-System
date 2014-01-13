package com.buildml.eclipse.packages.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.BmlPropertyPage;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.dialogs.VFSTreeSelectionDialog;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.model.IPackageRootMgr;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.errors.FatalError;

/**
 * An Eclipse "property" page that allows viewing/editing of an actions top-level properties
 * Objects of this class are referenced in the plugin.xml file and are dynamically
 * created when the properties dialog is opened for a UIAction object.
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ActionPropertyPage extends BmlPropertyPage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The ID of the underlying action */
	private int actionId;

	/** The original path ID of the directory this action executed in (before we changed it) */
	private int originalPathId;
	
	/** The new path ID of the directory this action executes in */
	private int selectedPathId;
	
	/** The package that this action resides in */
	private PackageDesc pkg;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionShellCommandPage object.
	 */
	public ActionPropertyPage() {
		/* nothing */
	}
 
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/
	
	/**
	 * Create the widgets that appear within the properties dialog box.
	 */
	@Override
	protected Control createContents(Composite parent) {
		
		/* determine the numeric ID of the action */
		UIAction action = (UIAction) GraphitiUtils.getBusinessObjectFromElement(getElement(), UIAction.class);
		if (action == null) {
			return null;
		}
		actionId = action.getId();
		
		setTitle("Action Properties:");
		
		/* create a panel in which all sub-widgets are added. */
		final Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);

		/* 
		 * Display the action's type (e.g. "Shell Command")
		 */		
		String actionType = "Invalid";
		int actionTypeId = actionMgr.getActionType(actionId);
		if (actionTypeId != ErrorCode.NOT_FOUND) {
			String actionTypeString = actionTypeMgr.getName(actionTypeId);
			if (actionTypeString != null) {
				actionType = actionTypeString;
			}
		}
		new Label(panel, SWT.NONE).setText("Action Type : ");
		new Label(panel, SWT.NONE).setText(actionType);
		new Label(panel, SWT.NONE); /* filler */
		
		/* 
		 * Display the package that the action belongs to
		 */
		String packageName = "Invalid";
		pkg = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId);
		if (pkg != null) {
			String packageNameString = pkgMgr.getName(pkg.pkgId);
			if (packageNameString != null) {
				packageName = packageNameString;
			}
		}
		new Label(panel, SWT.NONE).setText("Action Package : ");
		new Label(panel, SWT.NONE).setText(packageName);
		new Label(panel, SWT.NONE); /* filler */
		
		/*
		 * Display the action's directory, providing the option to change the directory.
		 */
		originalPathId = actionMgr.getDirectory(actionId);
		String pathString = fileMgr.getPathName(originalPathId);
		new Label(panel, SWT.NONE).setText("Action Directory : ");
		final Label actionDirectoryLabelValue = new Label(panel, SWT.NONE);
		actionDirectoryLabelValue.setText(pathString);
		Button editDirectoryButton = new Button(panel, SWT.NONE);
		editDirectoryButton.setText(" Edit Directory ");
		
		/* 
		 * When "Edit Directory" is selected, allow selection of a new directory,
		 * then refresh the labels appropriately.
		 */
		editDirectoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editDirectory();
				actionDirectoryLabelValue.setText(fileMgr.getPathName(selectedPathId));
				panel.layout();
			}
		});
		
		return panel;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * The user has pressed the "edit" directory button, so proceed to allow them to edit
	 * the action's directory.
	 */
	protected void editDirectory() {

		/*
		 * Determine and source or generated roots for this package. The selected directory
		 * must fall within one of them. If not, we reject the selection and try again.
		 */
		int srcRootId = pkgRootMgr.getPackageRoot(pkg.pkgId, IPackageRootMgr.SOURCE_ROOT);
		int genRootId = pkgRootMgr.getPackageRoot(pkg.pkgId, IPackageRootMgr.GENERATED_ROOT);
		if ((srcRootId == ErrorCode.NOT_FOUND) || (genRootId == ErrorCode.NOT_FOUND)) {
			throw new FatalError("Unable to fetch source or generated package root for package " + pkg.pkgId);
		}

		/* repeat until cancelled, or a valid selection is made */
		boolean done = false;
		while (!done) {
			
			/* bring up the selection dialog so the user can select a directory */
			VFSTreeSelectionDialog dialog = 
					new VFSTreeSelectionDialog(getShell(), buildStore, 
							"Select the directory in which this action should execute", false);

			/* 
			 * if OK is pressed, then fetch the selected directory, validate that it's within
			 * the package's root. The performOK() method will actually put the change into effect.
			 */
			if (dialog.open() == VFSTreeSelectionDialog.OK) {
				Object[] result = dialog.getResult();
				if (result.length == 1) {
					UIDirectory selection = (UIDirectory)result[0];
					int possiblePathId = selection.getId();

					/* validate that the selected directory is within the package roots */
					if (fileMgr.isAncestorOf(srcRootId, possiblePathId) ||
							fileMgr.isAncestorOf(genRootId, possiblePathId)) {

						selectedPathId = possiblePathId;
						done = true;
					}

					/* else - give an error */
					else {
						String pkgSrcRoot = fileMgr.getPathName(srcRootId);
						String pkgGenRoot = fileMgr.getPathName(genRootId);

						AlertDialog.displayErrorDialog("Invalid Directory",
								"The directory you have selected is not within the current package's " +
										"source or generated root." +
										"\n\nPackage Source Root  : " + pkgSrcRoot +
										"\nPackage Generated Root : " + pkgGenRoot);
					}
				}
			}
			
			/* else cancelled */
			else {
				done = true;
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The user has pressed the OK button
	 */
	@Override
	public boolean performOk() {
		
		// TODO: add code for changing the directory slot via an IUndoOp.
		// TODO: put this operation in the same MultiUndoOp as the performOK() for shell commands.
		// This is possibly done via the BmlPropertyPage parent class.
		return super.performOk();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
