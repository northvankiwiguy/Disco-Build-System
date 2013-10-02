package com.buildml.eclipse.packages.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.utils.BmlPropertyPage;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.utils.errors.ErrorCode;

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
		UIAction action = (UIAction) GraphitiUtils.getSelectedBusinessObjects(getElement(), UIAction.class);
		if (action == null) {
			return null;
		}
		actionId = action.getId();
		
		setTitle("Action Properties:");
		
		/* create a panel in which all sub-widgets are added. */
		Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);

		/* Display the action's type (e.g. "Shell Command") */
		Label actionTypeLabel = new Label(panel, SWT.NONE);
		String actionType = "Invalid";
		int actionTypeId = actionMgr.getActionType(actionId);
		if (actionTypeId != ErrorCode.NOT_FOUND) {
			String actionTypeString = actionTypeMgr.getName(actionTypeId);
			if (actionTypeString != null) {
				actionType = actionTypeString;
			}
		}
		actionTypeLabel.setText("Action Type:         " + actionType);
		
		/* Display the package that the action belongs to */
		Label actionPackageLabel = new Label(panel, SWT.NONE);
		String packageName = "Invalid";
		PackageDesc pkg = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId);
		if (pkg != null) {
			String packageNameString = pkgMgr.getName(pkg.pkgId);
			if (packageNameString != null) {
				packageName = packageNameString;
			}
		}
		actionPackageLabel.setText("Action Package:   " + packageName);
		
		return panel;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
