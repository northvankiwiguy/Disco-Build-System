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

package com.buildml.eclipse.bobj;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * A sub-class of UIInteger used to represent "action" objects in the Eclipse UI.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class UIAction extends UIInteger implements IPropertySource2 {

	/*=====================================================================================*
	 * FIELD/TYPES
	 *=====================================================================================*/
	
	public static final String TYPE_ID 	= "action.type";
	public static final String PACKAGE_ID 	= "action.package";
	public static final String COMMAND_ID 	= "action.shell.command";

	private static final PropertyDescriptor TYPE_PROPERTY_DESCRIPTOR = 
			new PropertyDescriptor(TYPE_ID, "Action Type");
	private static final PropertyDescriptor PACKAGE_PROPERTY_DESCRIPTOR = 
			new PropertyDescriptor(PACKAGE_ID, "Package");
	private static final TextPropertyDescriptor COMMAND_PROPERTY_DESCRIPTOR = 
			new TextPropertyDescriptor(COMMAND_ID, "Shell Command");
	
	private static final IPropertyDescriptor[] DESCRIPTORS = {
		TYPE_PROPERTY_DESCRIPTOR,
		PACKAGE_PROPERTY_DESCRIPTOR,
		COMMAND_PROPERTY_DESCRIPTOR
	};
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new UIAction, with the specified "id".
	 * @param id The unique ID that describes this UIAction (as managed by the ActionMgr).
	 */
	public UIAction(int id) {
		super(id);
	}
	
	/*-------------------------------------------------------------------------------------*/	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#getEditableValue()
	 */
	@Override
	public Object getEditableValue() {
		return this;
	}

	/*-------------------------------------------------------------------------------------*/	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
	 */
	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return DESCRIPTORS;
	}
	
	/*-------------------------------------------------------------------------------------*/	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java.lang.Object)
	 */
	@Override
	public Object getPropertyValue(Object arg0) {
		
		IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		if (buildStore == null) {
			return null;
		}
		IActionMgr actionMgr = buildStore.getActionMgr();
		
		/* the type of the action */
		if (arg0.equals(TYPE_ID)) {
			IActionTypeMgr actionTypeMgr = buildStore.getActionTypeMgr();
			int actionTypeId = actionMgr.getActionType(getId());
			if (actionTypeId == ErrorCode.NOT_FOUND) {
				return "Invalid Type";
			}
			return actionTypeMgr.getName(actionTypeId);
		}
		
		/* the package that this action belongs to */
		else if (arg0.equals(PACKAGE_ID)) {
			IPackageMgr pkgMgr = buildStore.getPackageMgr();
			int pkgId = pkgMgr.getActionPackage(getId());
			if (pkgId == ErrorCode.NOT_FOUND) {
				return "Invalid Package";
			}
			return pkgMgr.getName(pkgId);
		}
		else if (arg0.equals(COMMAND_ID)) {
			return actionMgr.getCommand(getId());
		}
		
		/* unrecognized property */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#resetPropertyValue(java.lang.Object)
	 */
	@Override
	public void resetPropertyValue(Object arg0) {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void setPropertyValue(Object arg0, Object arg1) {
		if (arg0.equals(COMMAND_ID)) {
			ActionChangeOperation op = new ActionChangeOperation("change action", getId());
			String oldCommand = (String) getPropertyValue(COMMAND_ID);
			op.recordCommandChange(oldCommand, (String)arg1);
			op.recordAndInvoke();
		}
	}

	/*-------------------------------------------------------------------------------------*/	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource2#isPropertyResettable(java.lang.Object)
	 */
	@Override
	public boolean isPropertyResettable(Object arg0) {
		return false;
	}

	/*-------------------------------------------------------------------------------------*/	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource2#isPropertySet(java.lang.Object)
	 */
	@Override
	public boolean isPropertySet(Object arg0) {
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
