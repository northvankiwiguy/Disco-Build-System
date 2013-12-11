/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.refactor.imports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.undo.ActionUndoOp;
import com.buildml.model.undo.FileGroupUndoOp;
import com.buildml.model.undo.FileUndoOp;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.CanNotRefactorException.Cause;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.errors.FatalError;

/**
 * An object of this type is used for handling a single "move to package" operation,
 * which takes one or more existing package members (files, file group, actions, etc)
 * and moves them to a new package.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class MovePackageRefactorer {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** ID of the package we're moving members into */
	private int destPkgId;
	
	/** The MultiUndoOp that we'll append all our operations to */
	private MultiUndoOp multiOp;
	
	/** The build store that members belong to */
	private IBuildStore buildStore;
	
	/** Package manager for this buildStore */
	private IPackageMgr pkgMgr;

	/** Action manager for this buildStore */
	private IActionMgr actionMgr;
	
	/** File manager for this buildStore */
	private IFileMgr fileMgr;
	
	/** File Group manager for this buildStore */
	private IFileGroupMgr fileGroupMgr;
	
	/** Package member manager for this buildStore */
	private IPackageMemberMgr pkgMemberMgr;
	
	/** Package Root manager for this buildStore */
	private IPackageRootMgr pkgRootMgr;
	
	/** The destination package's path root ID */
	private int pkgRootId;
	
	/** The slotID of the "Input" slot for a "Shell Command" action */
	private int inputSlotId;
	
	/** The slotID of the "Output" slot for a "Shell Command" action */
	private int outputSlotId;
	
	/** The cache of action->fileGroup mapping, used to avoid re-importing actions multiple times */
	private Map<Integer, Integer> actionCache;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new {@link MovePackageRefactorer} object.
	 * 
	 * @param destPkgId 	ID of the package to move members into.
	 * @param multiOp 		MultiUndoOp to add change operations to.
	 * @param buildStore 	The IBuildStore that stores the packages/members.
	 */
	public MovePackageRefactorer(int destPkgId, MultiUndoOp multiOp, IBuildStore buildStore) {
		this.destPkgId = destPkgId;
		this.multiOp = multiOp;
		this.buildStore = buildStore;
		
		pkgMgr = buildStore.getPackageMgr();
		actionMgr = buildStore.getActionMgr();
		fileMgr = buildStore.getFileMgr();
		fileGroupMgr = buildStore.getFileGroupMgr();
		pkgMemberMgr = buildStore.getPackageMemberMgr();
		pkgRootMgr = buildStore.getPackageRootMgr();
		
		/* 
		 * Determine "Input" and "Output0" slot IDs - these are populated by our import
		 * algorithm, so compute them here once.
		 */
		IActionTypeMgr actionTypeMgr = buildStore.getActionTypeMgr();
		int shellTypeId = actionTypeMgr.getActionTypeByName("Shell Command");
		SlotDetails inputSlotDetails = actionTypeMgr.getSlotByName(shellTypeId, "Input");
		if (inputSlotDetails == null) {
			throw new FatalError("Can't find slot \"Input\"");
		}
		SlotDetails outputSlotDetails = actionTypeMgr.getSlotByName(shellTypeId, "Output0");
		if (outputSlotDetails == null) {
			throw new FatalError("Can't find slot \"Output0\"");
		}
		inputSlotId = inputSlotDetails.slotId;
		outputSlotId = outputSlotDetails.slotId;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * This is the main entry point for moving package members from one package to another.
	 * 
	 * @param members	The list of members to move.
	 * @throws CanNotRefactorException Something went wrong with the move operation.
	 */
	public void moveMembersToPackage(List<MemberDesc> members) throws CanNotRefactorException {

		/* Validate that the destination package ID is valid */
		if (!pkgMgr.isValid(destPkgId)) {
			throw new CanNotRefactorException(Cause.INVALID_PACKAGE, destPkgId);
		}
		
		/* 
		 * We'll need to know the package's source root ID. Files can't be moved into
		 * a package if they're not within this root.
		 */
		pkgRootId = pkgRootMgr.getPackageRoot(destPkgId, IPackageRootMgr.SOURCE_ROOT);
		if (pkgRootId == ErrorCode.NOT_FOUND) {
			throw new FatalError("Unrecognized package ID");
		}

		/* validate all members passed as input  - this will throw an exception if there's an error */
		validateMembersList(members);
		
		/* initialize the action->fileGroup mapping cache */
		actionCache = new HashMap<Integer, Integer>();
		
		/*
		 * Now, traverse the list of input members and separate them into groups:
		 *   1) Loose files that aren't generated by an action (these will be placed in their
		 *      own file group).
		 *   2) Files that are generated by a single action (we care about that action, rather
		 *      than the file, and simply treat it as if the caller had asked to move the action).
		 *   3) Files that are generated by multiple actions (i.e. modified). This is an error.
		 *   4) Actions that the caller has explicitly asked to move to the destination package.
		 */
		List<Integer> looseMembers = new ArrayList<Integer>();
		List<Integer> badFiles = new ArrayList<Integer>();
		List<Integer> actions = new ArrayList<Integer>();
				
		/* for each input member that is a file or an action... */
		for (MemberDesc member : members) {
			int id = member.memberId;
			int type = member.memberType;
			
			/* for files, determine if it's generated or not */
			if (type == IPackageMemberMgr.TYPE_FILE) {

				/* Check for actions that "modify" this file (which is invalid) */
				Integer[] modifyingActions = actionMgr.getActionsThatAccess(id, OperationType.OP_MODIFIED);
				if (modifyingActions.length > 0) {
					badFiles.add(id);
				}

				/*
				 * Check for actions that write this file (only one action can write, else it's
				 * invalid). Zero writers = loose file, One writer = record the action, Multiple writers =
				 * an error.
				 */
				else {
					Integer[] generatorActions = actionMgr.getActionsThatAccess(id, OperationType.OP_WRITE);
					int numActions = generatorActions.length;
					if (numActions == 0) {
						looseMembers.add(id);
					} else if (numActions == 1) {
						int generatorAction = generatorActions[0];
						if (!actions.contains(generatorAction)) {
							actions.add(generatorAction);
						}
					} else {
						badFiles.add(id);
					}
				}
			}
			
			/* else record all actions for later processing */
			else if (type == IPackageMemberMgr.TYPE_ACTION) {
				if (!actions.contains(id)) {
					actions.add(id);
				}
			}
		}
		
		/*
		 * Were there any files that were generated by multiple actions (or modified by an action)?
		 * If so, throw an error.
		 */
		if (badFiles.size() != 0) {
			throw new CanNotRefactorException(Cause.FILE_IS_MODIFIED, badFiles.toArray(new Integer[0]));
		}
		
		/* 
		 * If there are any loose files, create a source file group and populate it with those files.
		 * This becomes a standalone file group that appears on the diagram, without any generator
		 * actions.
		 */
		if (looseMembers.size() > 0) {
			createSourceFileGroup(looseMembers);
		}
		
		/* 
		 * For each action, move the action and any predecessor files/filegroups/actions to the package.
		 * It's these calls that manage the recursion through the chain of imported files/actions.
		 */
		for (int actionId : actions) {
			moveActionToPackage(actionId);
		}
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Validate the list of members that is input into moveMembersToPackage(). There are
	 * many possible errors, including invalid member types, or undefined action, file,
	 * or fileGroup ID numbers. Errors are report via exceptions.
	 * 
	 * @param members 		The list of MemberDesc to be validated.
	 * @throws CanNotRefactorException The reason why the members list is invalid.
	 */
	private void validateMembersList(List<MemberDesc> members) throws CanNotRefactorException {

		IFileMgr fileMgr = buildStore.getFileMgr();
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		IActionMgr actionMgr = buildStore.getActionMgr();
		
		/* can't be null! */
		if (members == null) {
			throw new CanNotRefactorException(Cause.INVALID_MEMBER, -1);
		}
		
		/* keep a lists of invalid "things" - we can provide the whole list in the exception */
		List<Integer> invalidFiles = new ArrayList<Integer>();
		List<Integer> invalidFileGroups = new ArrayList<Integer>();
		List<Integer> invalidActions = new ArrayList<Integer>();
		
		/*
		 * Scan the list of members, validating their type, and whether each ID is valid.
		 * Invalid entries are logged, and will be reported as exceptions once we've seen
		 * all the members.
		 */
		for (MemberDesc member: members) {
			int id = member.memberId;
			switch (member.memberType) {
			case IPackageMemberMgr.TYPE_FILE:
				PathType pathType = fileMgr.getPathType(id);
				if (pathType == PathType.TYPE_INVALID) {
					invalidFiles.add(id);
				}
				break;
					
			case IPackageMemberMgr.TYPE_FILE_GROUP:
				if (fileGroupMgr.getGroupType(id) == ErrorCode.NOT_FOUND) {
					invalidFileGroups.add(id);
				}
				break;
					
			case IPackageMemberMgr.TYPE_ACTION:
				if (!actionMgr.isActionValid(id)) {
					invalidActions.add(id);
				}
				break;
					
			default:
				throw new CanNotRefactorException(Cause.INVALID_MEMBER, member.memberType);
			}
		}
		
		/* Thrown exceptions if we found anything invalid */
		if (!invalidFiles.isEmpty()) {
			throw new CanNotRefactorException(Cause.INVALID_PATH, invalidFiles.toArray(new Integer[0]));
		}
		if (!invalidActions.isEmpty()) {
			throw new CanNotRefactorException(Cause.INVALID_ACTION, invalidActions.toArray(new Integer[0]));
		}
		if (!invalidFileGroups.isEmpty()) {
			throw new CanNotRefactorException(Cause.INVALID_FILE_GROUP, invalidFileGroups.toArray(new Integer[0]));
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given an actionId, move the action, and possibly all its predecessors to the new package.
	 * If necessary, an output file group is created and also moved to the new package. So
	 * too are all the files in that groups.
	 *
	 * @param actionId ID of the action to be moved.
	 * @return ID of the output file group, or ErrorCode.NOT_FOUND if there's no output group.
	 * @throws CanNotRefactorException If something goes wrong.
	 */
	private int moveActionToPackage(int actionId) throws CanNotRefactorException {
		
		/* first, check to see if this action is already imported this package - if so, we're done */
		Object existingMap = actionCache.get(Integer.valueOf(actionId));
		if (existingMap instanceof Integer) {
			return (Integer)existingMap;
		}
		
		/* validate that the action is atomic */
		Integer children[] = actionMgr.getChildren(actionId);
		if (children.length != 0) {
			throw new CanNotRefactorException(Cause.ACTION_NOT_ATOMIC, actionId);
		}
		
		/* 
		 * The action is not yet imported, and it is atomic. Figure out its current package
		 * and prepare to move it to the new package.
		 */
		int fileGroupId = ErrorCode.NOT_FOUND;
		PackageDesc desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId);
		if (desc == null) {
			throw new FatalError("Can't retrieve action's current package");
		}
			
		/* Create UndoOp for changing the action's package */
		ActionUndoOp actionOp = new ActionUndoOp(buildStore, actionId);
		actionOp.recordPackageChange(desc.pkgId, destPkgId);
		multiOp.add(actionOp);
			
		/* Create a new file group containing all the files that this action generates */
		Integer writtenFiles[] = actionMgr.getFilesAccessed(actionId, OperationType.OP_WRITE);
		if (writtenFiles.length > 0) {
			fileGroupId = createSourceFileGroup(Arrays.asList(writtenFiles));
				
			/* Connect the "output" slot from the action to this new file group */
			ActionUndoOp slotOp = new ActionUndoOp(buildStore, actionId);
			slotOp.recordSlotChange(outputSlotId, null, fileGroupId);
			multiOp.add(slotOp);
		}
		
		/* compute/generate/move all the predecessor actions or file groups */
		computeInputActions(actionId);
		
		/* 
		 * Store actionId/fileGroupId in our cache, to avoid doing this import again
		 * and ending up with multiple output file groups when only one is required.
		 */
		actionCache.put(actionId, fileGroupId);
		
		/* return the ID of the output file group */
		return fileGroupId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For the specified action, identify (or generate) the necessary input file groups,
	 * which may involve recursively moving predecessor actions into our destination package.
	 * 
	 * @param actionId ID of the action to be recursively dealt with.
	 * @throws CanNotRefactorException Something went wrong.
	 */
	private void computeInputActions(int actionId) throws CanNotRefactorException {
		
		/* compute the list of input files that are read by this action */
		Integer readFiles[] = actionMgr.getFilesAccessed(actionId, OperationType.OP_READ);
		
		/* From this of input files, we need to track the loose files, the bad files and the actions */
		List<Integer> looseMembers = new ArrayList<Integer>();
		List<Integer> badFiles = new ArrayList<Integer>();
		List<Integer> actions = new ArrayList<Integer>();
		
		/* for each file, figure out which action generates it, or perhaps it's a loose file? */
		for (int i = 0; i < readFiles.length; i++) {
			int fileId = readFiles[i];

			/* if any actions modify this file, that's an error */
			Integer[] modifyingActions = actionMgr.getActionsThatAccess(fileId, OperationType.OP_MODIFIED);
			if (modifyingActions.length > 0) {
				badFiles.add(fileId);
			}
			
			else {
				/* figure out how this file is generated (if it is at all) */
				Integer generatorActions[] = actionMgr.getActionsThatAccess(fileId, OperationType.OP_WRITE);
				int generatorLength = generatorActions.length;

				/* no generating action, so it's a "loose" file */
				if (generatorLength == 0) {
					looseMembers.add(fileId);
				}

				/* With one generator action, record that action, if it's not already recorded */
				else if (generatorLength == 1) {
					if (!actions.contains(generatorActions[0])) {
						actions.add(generatorActions[0]);
					}
				}

				/* files with multiple generators are a problem */
				else {
					badFiles.add(fileId);
				}
			}				
		}
		
		/*
		 * Were there any files that were generated by multiple actions (or modified by an action).
		 */
		if (badFiles.size() != 0) {
			throw new CanNotRefactorException(Cause.FILE_IS_MODIFIED, badFiles.toArray(new Integer[0]));
		}

		/*
		 * We now have a list of actions (with no duplicates) that are known to generate the files
		 * that we need as input to the current action (actionId). Recursively deal with those
		 * actions, and use their output file groups as our input (possibly using a merge file group
		 * and filters).
		 */
		List<Integer> fileGroups = new ArrayList<Integer>();
		
		/* If there are any loose files, create a file group and populate it with those files */
		if (looseMembers.size() > 0) {
			int looseFileGroupId = createSourceFileGroup(looseMembers);
			fileGroups.add(looseFileGroupId);
		}
		
		/* 
		 * For each generating action, deal with it recursively, then record the output file group.
		 * By the time this loop is done, we should have all the predecessor actions/groups scheduled
		 * to be moved to destPkgId. The fileGroups list will contain the complete list of file
		 * groups generated by these sub actions.
		 */
		for (int subActionId : actions) {
			int inputFileGroupId = moveActionToPackage(subActionId);
			if (inputFileGroupId != ErrorCode.NOT_FOUND) {
				if (!fileGroups.contains(inputFileGroupId)) {
					fileGroups.add(inputFileGroupId);
				}
			}
		}
		
		/* join the upstream file group(s) to this action's input slot, if there are any groups. */
		int numUpstreamFileGroups = fileGroups.size();
		if (numUpstreamFileGroups >= 1) {
			int inputFileGroupId;
			
			/* a single input group */
			if (numUpstreamFileGroups == 1) {
				inputFileGroupId = fileGroups.get(0);
			} 
			/* create a merge group */
			else {
				inputFileGroupId = createMergeFileGroup(fileGroups);
			}
		
			ActionUndoOp slotOp = new ActionUndoOp(buildStore, actionId);
			slotOp.recordSlotChange(inputSlotId, null, inputFileGroupId);
			multiOp.add(slotOp);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a list of file IDs, create a new source file group and schedule the members to
	 * be added to it (by appending to the multiOp). In addition to moving the file group
	 * into the destination package, all the individual files are also moved. If any files
	 * are not within the source root, throw a {@link CanNotRefactorException} with cause
	 * code of PATH_OUT_OF_RANGE.
	 * 
	 * @param members A list of file IDs to be added to the file group.
	 * @return The ID of the newly-created file group.
	 * @throws CanNotRefactorException Something went wrong during the refactoring.
	 */
	private int createSourceFileGroup(List<Integer> members) throws CanNotRefactorException {
		
		/*
		 * Create the new fileGroup. Even though we won't populate it until the multiOp
		 * is executed, we must still allocate a new fileGroup ID number.
		 */
		int fileGroupId = fileGroupMgr.newSourceGroup(destPkgId);
		if (fileGroupId < 0) {
			throw new FatalError("Unable to create new file group");
		}
		FileGroupUndoOp op = new FileGroupUndoOp(buildStore, fileGroupId);
		op.recordMembershipChange(new ArrayList<Integer>(), members);
		multiOp.add(op);
	
		/* 
		 * Move all the files into the destination package, using FileUndoOps. Before
		 * a file can be moved, we must ensure that it's within the source root of the package.
		 */
		List<Integer> filesOutOfRange = new ArrayList<Integer>();
		
		/* 
		 * For each loose file, validate if it's within the package roots, and if so, 
		 * schedule an UndoOp to make the necessary change. If not, throw an exception.
		 */
		for (int pathId : members) {
			PackageDesc oldDesc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, pathId);
			if (oldDesc == null) {
				throw new FatalError("Can't find pathId");
			}
		
			/* 
			 * Check that this path is within the source root. If so, schedule it to be move to
			 * the destination package.
			 */
			if (fileMgr.isAncestorOf(pkgRootId, pathId)) {
				FileUndoOp pkgChangeOp = new FileUndoOp(buildStore, pathId);
				pkgChangeOp.recordChangePackage(oldDesc.pkgId, oldDesc.pkgScopeId, 
						destPkgId, IPackageMemberMgr.SCOPE_PRIVATE);
				multiOp.add(pkgChangeOp);
			}
			
			/* 
			 * Else, record this pathID as being out of range. We'll report an exception
			 * once we've collected the complete list of invalid paths.
			 */
			else {
				filesOutOfRange.add(pathId);
			}
		}
		
		/*
		 * If any files were out of range, throw an exception.
		 */
		if (filesOutOfRange.size() > 0) {
			throw new CanNotRefactorException(Cause.PATH_OUT_OF_RANGE, filesOutOfRange.toArray(new Integer[0]));
		}
		
		return fileGroupId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Insert the specified file groups into a newly created merge file group.
	 * 
	 * @param subFileGroups The sub file groups to be added.
	 * @return The ID of the newly-created merge file group.
	 */
	private int createMergeFileGroup(List<Integer> subFileGroups) {
		/*
		 * Create the new fileGroup. Even though we won't populate it until the multiOp
		 * is executed, we must still allocate a new fileGroup ID number.
		 */
		int fileGroupId = fileGroupMgr.newMergeGroup(destPkgId);
		if (fileGroupId < 0) {
			throw new FatalError("Unable to create new merge file group");
		}
		FileGroupUndoOp op = new FileGroupUndoOp(buildStore, fileGroupId);
		op.recordMembershipChange(new ArrayList<Integer>(), subFileGroups);
		multiOp.add(op);
		
		return fileGroupId;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
