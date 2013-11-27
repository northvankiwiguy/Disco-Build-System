package com.buildml.eclipse.packages.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.utils.BmlPropertyPage;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.model.undo.ActionUndoOp;

/**
 * An Eclipse "property" page that allows viewing/editing of a shell action's command
 * string. Objects of this class are referenced in the plugin.xml file and are dynamically
 * created when the properties dialog is opened for a UIAction object.
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ActionShellCommandPage extends BmlPropertyPage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The Text widget that contains the action's shell command string */
	private Text textField;
	
	/** The ID of the underlying action */
	private int actionId;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionShellCommandPage object.
	 */
	public ActionShellCommandPage() {
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
		ActionUndoOp op = new ActionUndoOp(buildStore, actionId);
		op.recordCommandChange(getShellCommandValue(), textField.getText());
		new UndoOpAdapter("Change Action", op).invoke();
		return super.performOk();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
