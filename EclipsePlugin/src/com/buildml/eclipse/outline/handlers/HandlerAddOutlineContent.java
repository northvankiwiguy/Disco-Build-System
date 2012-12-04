package com.buildml.eclipse.outline.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.outline.OutlinePage;
import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * An Eclipse UI Handler for managing the "Add Package" and "Add Package Folder" UI commands.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerAddOutlineContent extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		String operation = event.getParameter("com.buildml.eclipse.commandParameters.outlineContent");
		if (operation != null) {

			/* delegate this command to the OutlinePage object associated with our MainEditor */
			MainEditor mainEditor = EclipsePartUtils.getActiveMainEditor();
			if (mainEditor != null) {
				OutlinePage outlinePage = (OutlinePage)mainEditor.getAdapter(IContentOutlinePage.class);
				
				if (operation.equals("newPackage")) {
					outlinePage.newPackageOrFolder(false);
				} else if (operation.equals("newPackageFolder")) {
					outlinePage.newPackageOrFolder(true);
				}
			}
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
