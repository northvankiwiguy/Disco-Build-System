package com.buildml.eclipse.outline.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.outline.OutlinePage;
import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * An Eclipse UI Handler for managing the "Change Package Roots" UI command.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerChangeRootsOutlineContent extends AbstractHandler {


	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		/* delegate this command to the OutlinePage object associated with our MainEditor */
		MainEditor mainEditor = EclipsePartUtils.getActiveMainEditor();
		if (mainEditor != null) {
			OutlinePage outlinePage = (OutlinePage)mainEditor.getAdapter(IContentOutlinePage.class);
			outlinePage.changeRoots();
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		MainEditor mainEditor = EclipsePartUtils.getActiveMainEditor();
		if (mainEditor != null) {
			OutlinePage outlinePage = (OutlinePage)mainEditor.getAdapter(IContentOutlinePage.class);
			return outlinePage.getChangeRootsEnabled();
		}
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
