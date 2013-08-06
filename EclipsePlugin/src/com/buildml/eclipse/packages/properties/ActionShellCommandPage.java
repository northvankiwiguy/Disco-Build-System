package com.buildml.eclipse.packages.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;

/**
 * An Eclipse "property" page that allows viewing/editing of a shell action's command
 * string. Objects of this class are referenced in the plugin.xml file and are dynamically
 * created when the properties dialog is opened for a UIAction object.
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ActionShellCommandPage extends PropertyPage implements
		IWorkbenchPropertyPage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The Text widget that contains the action's shell command string */
	private Text textField;
	
	/** The ID of the underlying action */
	private int actionId;
	
	/** The IBuildStore that contains the action */
	private IBuildStore buildStore;
	
	/** The actionMgr within the buildStore */
	private IActionMgr actionMgr;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionShellCommandPage object.
	 */
	public ActionShellCommandPage() {
		buildStore = EclipsePartUtils.getActiveBuildStore();
		actionMgr = buildStore.getActionMgr();
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
		
		setTitle("Shell Action's Command String:");

		/* create a panel in which all sub-widgets are added. */
		Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);
			
		/* 
		 * Create a multi-line text box (1/3rd of the screen width) for displaying
		 * the shell action's command string.
		 */
		textField = new Text(panel, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = EclipsePartUtils.getScreenWidth() / 3;
		textField.setLayoutData(gd);
		textField.setText(getShellCommandValue());
				
		// TODO: how do I set the title of the properties box?
		
		return panel;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * @return The selected action's shell command string.
	 */
	private String getShellCommandValue() {
		return actionMgr.getCommand(actionId);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The OK button has been pressed in the properties box. Save all the field values
	 * into the database. This is done via the undo/redo stack.
	 */
	@Override
	public boolean performOk() {
		
		/* create an undo/redo operation that will invoke the underlying database changes */
		ActionChangeOperation op = new ActionChangeOperation("change action", actionId);
		op.recordCommandChange(getShellCommandValue(), textField.getText());
		op.recordAndInvoke();
		return super.performOk();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
