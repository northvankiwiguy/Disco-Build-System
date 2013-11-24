package com.buildml.eclipse.packages.properties.filegroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.filegroups.FileGroupChangeOperation;
import com.buildml.eclipse.utils.BmlPropertyPage;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.dialogs.VFSTreeSelectionDialog;
import com.buildml.model.IFileGroupMgr;

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
	
	/** The type of this file group (source, generated, etc) */
	private int fileGroupType;
	
	/** The TreeViewer control that contains the list of files */
	private TreeViewer filesList;
	
	/** The current content of the file group */
	private ArrayList<Integer> currentMembers;
	
	/** The initial content of the file group, before any editing took place */
	private ArrayList<Integer> initialMembers;
	
	/**
	 * Set to true if we're programmatically changing the selection and should therefore
	 * include the listener events.
	 */
	private boolean programmaticallySelecting = false;
	
	/** the JFace provider for fetching children of elements in the tree viewer */
	private FileGroupContentProvider contentProvider;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionShellCommandPage object.
	 */
	public FileGroupContentPropertyPage() {
		/* nothing */
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
	 * PROTECTED METHODS
	 *=====================================================================================*/
	
	/**
	 * Create the widgets that appear within the properties dialog box.
	 */
	@Override
	protected Control createContents(Composite parent) {
		
		/* determine the numeric ID of the file group */
		UIFileGroup fileGroup = (UIFileGroup) GraphitiUtils.getBusinessObjectFromElement(getElement(), UIFileGroup.class);
		if (fileGroup == null) {
			return null;
		}
		fileGroupId = fileGroup.getId();
		fileGroupType = fileGroupMgr.getGroupType(fileGroupId);
		
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
		filesList = new TreeViewer(panel, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		filesList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		contentProvider = new FileGroupContentProvider(buildStore, fileGroupType);
		filesList.setContentProvider(contentProvider);
		filesList.setLabelProvider(new FileGroupLabelProvider(buildStore, fileGroupType));
		
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
		
		/* add button - adds a new file to the source file group */
		if (fileGroupType == IFileGroupMgr.SOURCE_GROUP) {
			final Button newButton = new Button(buttonPanel, SWT.NONE);
			newButton.setText("Add File");
			newButton.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					performAddOperation();
				}
			});
		}
		
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
		filesList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (programmaticallySelecting) {
					return;
				}
				int selectedFilesCount = handleSelection();
				deleteButton.setEnabled(selectedFilesCount >= 1);
				enableMoveButtons(moveUpButton, moveDownButton);
			}
		});

		/* populate the TreeViewer control with all the file members */
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
		currentMembers = (ArrayList<Integer>)initialMembers.clone();
		populateList(filesList);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Compute the state of the "Move Up" and "Move Down" buttons. If the selection has the
	 * last member selected, we can't move down. If it has the first member selected, we can't
	 * move up. If no members are selected, neither is an option.
	 * 
	 * @param moveUpButton			The "Move Up" button control.
	 * @param moveDownButton		The "Move Down" button control.
	 */
	protected void enableMoveButtons(Button moveUpButton, Button moveDownButton) {
		
		/* we must have at least one member selected */
		ITreeSelection selection = (ITreeSelection) filesList.getSelection();
		int selectedFilesCount = selection.size();
		boolean moveUpEnabled = selectedFilesCount >= 1;
		boolean moveDownEnabled = selectedFilesCount >= 1;
		
		/* check whether the first/last members are highlighted */
		Iterator<TreeMember> iter = selection.iterator();
		while (iter.hasNext()) {
			TreeMember member = iter.next();
			if (member.seq == 0) {
				moveUpEnabled = false;
			} else if (member.seq == currentMembers.size() - 1) {
				moveDownEnabled = false;
			}
		}
		
		/* enable/disable the button state, as appropriate */
		moveUpButton.setEnabled(moveUpEnabled);
		moveDownButton.setEnabled(moveDownEnabled);
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
	
		Integer members[];
		if (fileGroupType == IFileGroupMgr.SOURCE_GROUP) {
			members = fileGroupMgr.getPathIds(groupId);
		} else {
			members = fileGroupMgr.getSubGroups(groupId);				
		}
		
		/* 
		 * We store the members in two separate arrays - one to record the initial
		 * set of members, and one to store the "to-be" set of members that the user
		 * is permitted to modify.
		 */
		initialMembers = new ArrayList<Integer>(members.length);
		currentMembers = new ArrayList<Integer>(members.length);
		for (int i = 0; i < members.length; i++) {
			initialMembers.add(members[i]);
			currentMembers.add(members[i]);
		}
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Populate the control containing the list of files.
	 * 
	 * @param filesList The List control.
	 */
	private void populateList(TreeViewer filesList) {
	
		TreeMember membersArray[] = new TreeMember[currentMembers.size()];
		int i = 0;
		for (int id : currentMembers) {
			membersArray[i] = new TreeMember(0, i, id, null);
			i++;
		}
		filesList.setInput(membersArray);
		filesList.expandAll();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new file (or files) to the file group (the new item will be added to the bottom).
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
		ITreeSelection selection = (ITreeSelection) filesList.getSelection();
		Iterator<Object> iter = selection.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			
			/* we can only remove top-level items (for source file groups and merge file groups). */
			if (element instanceof TreeMember) {
				TreeMember member = (TreeMember)element; 
				if (member.level == 0) {
					currentMembers.remove(member.seq);
				}
			}
		}
		
		populateList(filesList);
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
				
		/* if nothing's selected, there's nothing to do */
		ITreeSelection selection = (ITreeSelection) filesList.getSelection();
		if (selection.size() == 0) {
			return;
		}
		
		/* 
		 * Fetch the indicies (into the "currentMembers" member) of the selected items.
		 * For example, if the user selects the 2nd and 4th items in the list,
		 * our array should be [2, 4]. To do this, we need to convert the ITreeSelection
		 * that Eclipse gives us into a sorted array of indicies.
		 */
		ArrayList<Integer> selectedTopLevelIndicies = new ArrayList<Integer>();
		ArrayList<TreeMember> selectedMembers = new ArrayList<TreeMember>();
		Iterator<Object> iter = selection.iterator();
		int i = 0;
		while (iter.hasNext()) {
			Object element = iter.next();
			if (element instanceof TreeMember) {
				TreeMember member = (TreeMember)element;
				if (member.level == 0) {
					selectedTopLevelIndicies.add(member.seq);
					i++;
				}
				selectedMembers.add(member);
			}
		}
		
		/* convert from ArrayList<Integer> to sorted Integer[] */
		Integer selectedIndicies[] = new Integer[i];
		selectedTopLevelIndicies.toArray(selectedIndicies);
		Arrays.sort(selectedIndicies);
		
		/* 
		 * if our first item is at 0, and we're moving up, or our last item is at listSize -1, 
		 * and we're moving down, there's nothing to do. We can't move beyond the bounds of
		 * the list.
		 */
		int currentMemberSize = currentMembers.size();
		if (((direction == -1) && (selectedIndicies[0] == 0)) ||
			((direction == 1) && (selectedIndicies[selectedIndicies.length - 1] == (currentMemberSize - 1)))) {
			return;
		}

		/*
		 * The direction in which we're moving the files will dictate the order in which
		 * we must traverse the list of files (if we go the wrong direction, list items
		 * will "leapfrog" their neighbours, even if their neighbours are also moving.
		 */
		int firstIndex, lastIndex;
		if (direction == 1) {
			firstIndex = selectedIndicies.length - 1;
			lastIndex = -1;
		} else {
			firstIndex = 0;
			lastIndex = selectedIndicies.length;
		}
		
		/*
		 * Now we actually modify the content of "currentMembers". Starting at position
		 * 'firstIndex', and decrementing by 'direction' until we reach 'lastIndex'.
		 */
		int pos = firstIndex;
		while (pos != lastIndex) {
			int index = selectedIndicies[pos];
			int id = currentMembers.get(index);
			
			/* shuffle the item along */
			currentMembers.remove(index);
			currentMembers.add(index + direction, id);
			
			/* move to next selected file */
			pos -= direction;
		}
		
		/* redraw the tree with the modified content */
		populateList(filesList);
		
		/* 
		 * Reset the selection so that the same elements are selected in the new tree. Naturaly
		 * their sequence numbers have now changed.
		 */
		for (TreeMember member: selectedMembers) {
			member.seq += direction;
		}
		StructuredSelection newSelection = new StructuredSelection(selectedMembers);
		filesList.setSelection(newSelection, true);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Handle selection behaviour, which depends on the type of file group selected. For
	 * source groups, any number of items can be selected. For merge groups, if the user clicks
	 * on a single path name, we instead auto-select the entire sub-group that the path
	 * belongs to.
	 * 
	 * @return The number of items selected. Note that a merge file group will show an entire
	 * subgroup as being selected, but will return "1".
	 */
	private int handleSelection() {
		
		ITreeSelection selection = (ITreeSelection)filesList.getSelection();

		/* for source groups, any number of items can be selected */
		if (fileGroupType == IFileGroupMgr.SOURCE_GROUP) {
			return selection.size();
		}
		
		/* for merge file groups, selecting a single item will select all of them in that group */
		else if (fileGroupType == IFileGroupMgr.MERGE_GROUP) {
			
			/* for each selected element, select its entire subtree */
			TreePath paths[] = selection.getPaths();
			List<TreeMember> elementsToSelect = new ArrayList<TreeMember>();			
			for (int i = 0; i < paths.length; i++) {
				
				/* determine the parent, select it, then select all it children */
				TreeMember parentOfSubTree = (TreeMember) paths[i].getFirstSegment();
				elementsToSelect.add(parentOfSubTree);
				Object children[] = contentProvider.getChildren(parentOfSubTree);
				if (children != null) {
					for (int j = 0; j < children.length; j++) {
						elementsToSelect.add((TreeMember)children[j]);	
					}
				}
			}

			/* 
			 * Proceed to modify the current selection, taking care to ignore selection events
			 * (since this method is called from within an event listener).
			 */
			StructuredSelection newSelection = new StructuredSelection(elementsToSelect);
			programmaticallySelecting = true;
			filesList.setSelection(newSelection, true);
			programmaticallySelecting = false;
			
			/* yes, there's a selection */
			return elementsToSelect.size();
		}
		
		/* other situations not yet handled */
		return 0;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
