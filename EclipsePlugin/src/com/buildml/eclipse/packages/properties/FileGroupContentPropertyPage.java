package com.buildml.eclipse.packages.properties;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;

import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.filegroups.FileGroupChangeOperation;
import com.buildml.eclipse.utils.BmlPropertyPage;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.dialogs.VFSTreeSelectionDialog;

/**
 * An Eclipse "property" page that allows viewing/editing of file group's content.
 * Objects of this class are referenced in the plugin.xml file and are dynamically
 * created when the properties dialog is opened for a UIFileGroup object.
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupContentPropertyPage extends BmlPropertyPage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The ID of the underlying file group */
	private int fileGroupId;
	
	/** The List control that contains the list of files */
	private List filesList;
	
	/** The current content of the file group */
	private ArrayList<Integer> currentMembers;
	
	/** The initial content of the file group, before any editing took place */
	private ArrayList<Integer> initialMembers;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionShellCommandPage object.
	 */
	public FileGroupContentPropertyPage() {
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
		
		/* determine the numeric ID of the file group */
		UIFileGroup fileGroup = (UIFileGroup) GraphitiUtils.getSelectedBusinessObjects(getElement(), UIFileGroup.class);
		if (fileGroup == null) {
			return null;
		}
		fileGroupId = fileGroup.getId();
		if (!fetchMembers(fileGroupId)) {
			return null;
		}
		
		setTitle("File Group Content:");
		
		/* 
		 * Create a panel in which all sub-widgets are added. The first (of 2)
		 * columns will content the "list" of files in the file group. The
		 * second (of 2) columns contain buttons for performing actions
		 * on those files.
		 */
		Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		panel.setLayout(layout);
		
		/*
		 * The first column - the list of files in the file group.
		 */
		filesList = new List(panel, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		filesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/*
		 * The second column - buttons that we can press to modify the file group content
		 */
		Composite buttonPanel = new Composite(panel, SWT.NONE);
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		RowLayout buttonPanelLayout = new RowLayout(SWT.VERTICAL);
		buttonPanelLayout.fill = true;
		buttonPanelLayout.marginLeft = buttonPanelLayout.marginRight = 10;
		buttonPanelLayout.spacing = 10;
		buttonPanel.setLayout(buttonPanelLayout);
		
		/* add button - adds a new file to the file group */
		final Button newButton = new Button(buttonPanel, SWT.NONE);
		newButton.setText("Add File");
		newButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				performAddOperation();
			}
		});
		
		/* delete button - deletes the selected files */
		final Button deleteButton = new Button(buttonPanel, SWT.NONE);
		deleteButton.setText("Delete");
		deleteButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				performDeleteOperation();
			}
		});
		
		/* move up button - move the selected files upwards in the list */
		final Button moveUpButton = new Button(buttonPanel, SWT.NONE);
		moveUpButton.setText("Move Up");
		moveUpButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				performMoveOperation(-1);
			}
		});
		
		/* move down button - move the selected files down the list */
		final Button moveDownButton = new Button(buttonPanel, SWT.NONE);
		moveDownButton.setText("Move Down");
		moveDownButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				performMoveOperation(1);
			}
		});

		/*
		 * When items in the list box are selected/deselected, we need to enable/disable
		 * the buttons accordingly. By default, all buttons are disabled.
		 */
		deleteButton.setEnabled(false);
		moveUpButton.setEnabled(false);
		moveDownButton.setEnabled(false);
		
		filesList.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selectedFilesCount = filesList.getSelectionCount();
				deleteButton.setEnabled(selectedFilesCount >= 1);
				moveUpButton.setEnabled(selectedFilesCount >= 1);
				moveDownButton.setEnabled(selectedFilesCount >= 1);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				/* nothing */
			}
		});

		/* populate the List control with all the file members */
		populateList(filesList);

		return panel;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Restore the list of files to its initial value.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void performDefaults() {
		currentMembers = (ArrayList)initialMembers.clone();
		populateList(filesList);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The OK button was pressed. Proceed to change the file group in the underlying database 
	 */
	@Override
	public boolean performOk() {
		
		/* create an undo/redo operation that will invoke the underlying database changes */
		FileGroupChangeOperation op = new FileGroupChangeOperation("Modify File Group", fileGroupId);
		op.recordMembershipChange(initialMembers, currentMembers);
		op.recordAndInvoke();
		return super.performOk();
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Fetch the initial set of members of this file group. 
	 * 
	 * @param groupId The ID of the group we're editing.
	 * @return True if the group is valid, else false.
	 */
	private boolean fetchMembers(int groupId) {
		
		int groupSize = fileGroupMgr.getGroupSize(groupId);
		if (groupSize < 0) {
			return false;
		}
		
		/* 
		 * We store the members in two separate arrays - one to record the initial
		 * set of members, and one to store the "to-be" set of members that the user
		 * is permitted to modify.
		 */
		initialMembers = new ArrayList<Integer>(groupSize);
		currentMembers = new ArrayList<Integer>(groupSize);
		for (int i = 0; i != groupSize; i++) {
			int memberId = fileGroupMgr.getPathId(groupId, i);
			if (memberId < 0) {
				return false;
			}
			initialMembers.add(memberId);
			currentMembers.add(memberId);
		}
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Populate the control containing the list of files.
	 * 
	 * @param filesList The List control.
	 */
	private void populateList(List filesList) {
		filesList.removeAll();
		for (int i = 0; i < currentMembers.size(); i++) {
			String pathName = fileMgr.getPathName(currentMembers.get(i), true);
			if (pathName != null) {
				filesList.add(pathName);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new file to the file group (the new item will be added to the bottom).
	 */
	private void performAddOperation() {
		VFSTreeSelectionDialog dialog = 
				new VFSTreeSelectionDialog(getShell(), buildStore, "Select file to add to file group.", true);
		dialog.setAllowMultiple(true);
		
		/* invoke the dialog, allowing the user to select a directory/file */
		if (dialog.open() == VFSTreeSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length >= 1) {
				
				/* add the new members to the bottom of the members list */
				for (int i = 0; i != result.length; i++) {
					UIInteger selection = (UIInteger)result[i];
					int selectionId = selection.getId();
					currentMembers.add(selectionId);
				}
				
				populateList(filesList);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Delete the selected file(s).
	 */
	private void performDeleteOperation() {
		
		/* fetch the indicies of the selected items */
		int selectedIndicies[] = filesList.getSelectionIndices();
		Arrays.sort(selectedIndicies);
		
		/* 
		 * move backwards through the list, deleting them (deleting them in the forward
		 * direction will mess up our index numbers).
		 */
		int i = selectedIndicies.length - 1;
		while (i >= 0) {
			int deleteIndex = selectedIndicies[i]; 
			filesList.remove(deleteIndex);
			currentMembers.remove(deleteIndex);
			i--;
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Move the selected list items further up or down the list.
	 * 
	 * @param direction -1 to move the selected items upwards, or 1 to move downwards.
	 */
	private void performMoveOperation(int direction) {
		
		if ((direction != 1) && (direction != -1)) {
			return;
		}
		
		/* fetch the indicies of the selected items */
		int selectedIndicies[] = filesList.getSelectionIndices();
		Arrays.sort(selectedIndicies);
		int count = selectedIndicies.length;
		int listSize = filesList.getItemCount();
		
		/* if nothing's selected, there's nothing to do */
		if (count == 0) {
			return;
		}
		
		/* 
		 * if our first item is at 0, and we're moving up, or our last item is at listSize -1, 
		 * and we're moving down, there's nothing to do.
		 */
		if (((direction == -1) && (selectedIndicies[0] == 0)) ||
			((direction == 1) && (selectedIndicies[count - 1] == listSize - 1))) {
			return;
		}

		/*
		 * The direction in which we're moving the files will dictate the order in which
		 * we must traverse the list of files.
		 */
		int firstIndex, lastIndex;
		if (direction == 1) {
			firstIndex = count - 1;
			lastIndex = -1;
		} else {
			firstIndex = 0;
			lastIndex = count;
		}
		
		/*
		 * Traverse the list of files that we're moving. Starting at position 'firstIndex',
		 * and decrementing by 'direction' until we reach 'lastIndex'.
		 */
		int pos = firstIndex;
		while (pos != lastIndex) {
			int index = selectedIndicies[pos];
			String label = filesList.getItem(index);
			int id = currentMembers.get(index);
			
			/* shuffle the item along */
			filesList.remove(index);
			currentMembers.remove(index);
			filesList.add(label, index + direction);
			currentMembers.add(index + direction, id);
			filesList.select(index + direction);
			
			/* move to next selected file */
			pos -= direction;
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
