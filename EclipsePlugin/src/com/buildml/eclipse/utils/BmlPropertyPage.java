package com.buildml.eclipse.utils;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMgr;

/**
 * An abstract superclass of all Property pages. This class factors out the common functionality
 * that all Property pages will have.
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
public abstract class BmlPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The IBuildStore that contains the action */
	protected IBuildStore buildStore;

	/** The fileMgr within the buildStore */
	protected IFileMgr fileMgr;
	
	/** The actionMgr within the buildStore */
	protected IActionMgr actionMgr;
	
	/** The actionTypeMgr within the buildStore */
	protected IActionTypeMgr actionTypeMgr;

	/** The pkgMgr within the buildStore */
	protected IPackageMgr pkgMgr;
	
	/** The pkgMemberMgr within the buildStore */
	protected IPackageMemberMgr pkgMemberMgr;
	
	/** The IFileGroup manager that contains file group information */
	protected IFileGroupMgr fileGroupMgr;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new BmlPropertyPage. This is an abstract class and must be subclassed
	 * by specific property pages.
	 */
	public BmlPropertyPage() {
		buildStore = EclipsePartUtils.getActiveBuildStore();
		actionMgr = buildStore.getActionMgr();
		actionTypeMgr = buildStore.getActionTypeMgr();
		pkgMgr = buildStore.getPackageMgr();
		pkgMemberMgr = buildStore.getPackageMemberMgr();	
		fileGroupMgr = buildStore.getFileGroupMgr();
		fileMgr = buildStore.getFileMgr();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Property pages should be 1/2 the height of the screen, and 1/3rd the width.
	 */
	@Override
	public Point computeSize() {
		return new Point(EclipsePartUtils.getScreenWidth() / 3, EclipsePartUtils.getScreenHeight() / 2);
	}

	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		
		/* nothing to do, yet */
		return parent;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
