/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.packages;

import org.eclipse.emf.common.util.URI;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.graphiti.ui.editor.DefaultPersistencyBehavior;
import org.eclipse.graphiti.ui.editor.DefaultUpdateBehavior;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;

import com.buildml.eclipse.ISubEditor;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgrListener;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileGroupMgrListener;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.model.IPackageMemberMgrListener;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageMgrListener;
import com.buildml.model.types.PackageSet;
import com.buildml.utils.types.IntegerTreeSet;

/**
 * A Graphiti DiagramEditor for displaying a BuildML package. This editor would typically
 * appears within one of the tabs in the BuildML main editor.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageDiagramEditor extends DiagramEditor 
									implements ISubEditor, IPackageMgrListener, 
												IActionMgrListener, IPackageMemberMgrListener,
												IFileGroupMgrListener {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The BuildStore we're editing */
	private IBuildStore buildStore = null;

	/** The PackageMgr we're using for package information */
	private IPackageMgr pkgMgr = null;

	/** The PackageMgr we're using for package membership */
	private IPackageMemberMgr pkgMemberMgr = null;

	/** The FileGroupMgr we're using for file group content */
	private IFileGroupMgr fileGroupMgr = null;

	/** The ActionMgr we're using for action information */
	private IActionMgr actionMgr = null;
	
	/** The ID of the package we're displaying */
	private int packageId;

	/** The delegate object that Graphiti queries to see if our underlying model has changed */
	private PackageEditorUpdateBehavior updateBehavior;
		
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new PackageDiagramEditor object.
	 * 
	 * @param buildStore  The BuildStore that we're editing.
	 * @param packageId	  ID of the package we're displaying
	 */
	public PackageDiagramEditor(IBuildStore buildStore, int packageId) {
		super();
	
		/* Save away our BuildStore information, for later use */
		this.buildStore = buildStore;
		this.pkgMgr = buildStore.getPackageMgr();
		this.pkgMemberMgr = buildStore.getPackageMemberMgr();
		this.fileGroupMgr = buildStore.getFileGroupMgr();
		this.actionMgr = buildStore.getActionMgr();
		this.packageId = packageId;
		
		/* listen for changes to the packages and actions */
		pkgMgr.addListener(this);
		pkgMemberMgr.addListener(this);
		actionMgr.addListener(this);
		fileGroupMgr.addListener(this);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getEditorImage()
	 */
	@Override
	public Image getEditorImage() {
		return EclipsePartUtils.getImage("images/package_icon.gif");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setOption(int, boolean)
	 */
	@Override
	public void setOption(int optionBits, boolean enable) {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setOptions(int)
	 */
	@Override
	public void setOptions(int optionBits) {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getOptions()
	 */
	@Override
	public int getOptions() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#isOptionSet(int)
	 */
	@Override
	public boolean isOptionSet(int optionBit) {
		// TODO Auto-generated method stub
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#updateOptionsFromPreferenceStore()
	 */
	@Override
	public void updateOptionsFromPreferenceStore() {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getFilterPackageSet()
	 */
	@Override
	public PackageSet getFilterPackageSet() {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setFilterPackageSet(com.buildml.model.types.PackageSet)
	 */
	@Override
	public void setFilterPackageSet(PackageSet newSet) {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setRemovable(boolean)
	 */
	@Override
	public void setRemovable(boolean removable) {
		/* nothing to do - package diagrams are always removable */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#isRemovable()
	 */
	@Override
	public boolean isRemovable() {
		/* package diagrams are always removeable */
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#isDisposed()
	 */
	@Override
	public boolean isDisposed() {
		// TODO Auto-generated method stub
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#expandSubtree(java.lang.Object)
	 */
	@Override
	public void expandSubtree(Object node) {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#refreshView(boolean)
	 */
	@Override
	public void refreshView(boolean force) {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setVisibilityFilterSet(com.buildml.utils.types.IntegerTreeSet)
	 */
	@Override
	public void setVisibilityFilterSet(IntegerTreeSet visibleActions) {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getVisibilityFilterSet()
	 */
	@Override
	public IntegerTreeSet getVisibilityFilterSet() {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setItemVisibilityState(java.lang.Object, boolean)
	 */
	@Override
	public void setItemVisibilityState(Object item, boolean state) {
		// TODO Auto-generated method stub
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#pageChange()
	 */
	@Override
	public void pageChange() {
		
		/*
		 * This call is necessary when somebody double-clicks on a package name in the
		 * Outline view. If we don't call setFocus(), then the diagram won't be updated
		 * from the model correctly. However, this call isn't necessary in the case
		 * where we switch editor tabs (setFocus() gets called some other way).
		 */
		setFocus();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#hasFeature(java.lang.String)
	 */
	@Override
	public boolean hasFeature(String feature) {
		if ("removable".equals(feature)) {
			return true;
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#doCopyCommand(org.eclipse.swt.dnd.Clipboard, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void doCopyCommand(Clipboard clipboard, ISelection selection) {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getEditorImagePath()
	 */
	@Override
	public String getEditorImagePath() {
		/* not relevant for this editor */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The BuildStore associated with this sub-editor.
	 */
	public IBuildStore getBuildStore() {
		return buildStore;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The ID of the package that this editor represents.
	 */
	public int getPackageId() {
		return packageId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Receive notifications if the BuildStore's packages change in some way.
	 */
	@Override
	public void packageChangeNotification(int pkgId, int how) {
		
		if (pkgId == this.packageId){
			
			/* was the package that we're displaying removed? */
			if (how == IPackageMgrListener.REMOVED_PACKAGE) {
				MainEditor mainEditor = EclipsePartUtils.getActiveMainEditor();
				if (mainEditor != null) {
					mainEditor.setActiveEditor(this);
					int activeTab = mainEditor.getActivePage();
					if (activeTab != -1) {
						mainEditor.removePage(activeTab);
					}
				}
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgrListener#packageMemberChangeNotification(int, int)
	 */
	@Override
	public void packageMemberChangeNotification(int pkgId, int how, int memberType, int memberId) {

		if (pkgId == this.packageId){
			/* has the content of the package changed? Action/files added or removed? */
			if ((how == IPackageMemberMgrListener.CHANGED_MEMBERSHIP) ||
				(how == IPackageMemberMgrListener.CHANGED_LOCATION)) {
				updateBehavior.markChanged();
				setFocus();
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Receive notifications if an action (possibly on this package) changes in some way.
	 */
	@Override
	public void actionChangeNotification(int actionId, int how) {
		PackageDesc pkg = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId);
		if ((pkg == null) || (pkg.pkgId != this.packageId)) {
			return;
		}
		updateBehavior.markChanged();
		setFocus();		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Receive notifications if a file group (possibly on this package) changes in some way.
	 */
	@Override
	public void fileGroupChangeNotification(int fileGroupId, int how) {
		PackageDesc pkg = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId);
		if ((pkg == null) || (pkg.pkgId != this.packageId)) {
			return;
		}
		updateBehavior.markChanged();
		setFocus();	
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * This editor is being closed/disposed.
	 */
	@Override
	public void dispose() {
		super.dispose();
		pkgMgr.removeListener(this);
		pkgMemberMgr.removeListener(this);
		actionMgr.removeListener(this);
		fileGroupMgr.removeListener(this);
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Override the default persistency behaviour of this editor. This allows us to
	 * generate the view by demand-loading from the database, rather than loading a
	 * resource (file) from disk.
	 */
	@Override
	protected DefaultPersistencyBehavior createPersistencyBehavior() {
		return new EditorPersistencyBehavior(this);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Override the default update behaviour for this editor. This allows us to update
	 * the diagram (pictogram) when the underlying model changes.
	 */
	@Override
	protected DefaultUpdateBehavior createUpdateBehavior() {
		updateBehavior = new PackageEditorUpdateBehavior(this);
		return updateBehavior;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * No matter what IEditorInput this editor is opened with, we replace this with
	 * a DiagramEditorInput object which is suitable for passing to a DiagramEditor object.
	 */
	@Override
	protected DiagramEditorInput convertToDiagramEditorInput(IEditorInput input)
			throws PartInitException {
								
		URI pkgURI = URI.createURI("buildml:" + packageId);
		return new DiagramEditorInput(pkgURI, "com.buildml.eclipse.diagram.package.provider");
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.editor.DiagramEditor#createContextMenuProvider()
	 */
	@Override
	protected ContextMenuProvider createContextMenuProvider() {
		
		/* 
		 * for now, we don't need a context menu. In future, we can implement our own
		 * class (derived from DiagramEditorContextMenuProvider) that provides the menu.
		 */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
