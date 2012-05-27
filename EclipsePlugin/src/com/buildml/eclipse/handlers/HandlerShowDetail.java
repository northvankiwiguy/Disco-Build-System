package com.buildml.eclipse.handlers;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import com.buildml.eclipse.EditorOptions;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.errors.FatalError;

/**
 * A command handler for managing the state of various view options. These options are
 * triggered by the pop-up menu ("Show details"), or via the editor's toolbar icons.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerShowDetail extends AbstractHandler implements IElementUpdater {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * Given that each instance of this handler is for a specific type of options
	 * (e.g. packages, show-roots, etc). We can save time on each invocation of
	 * the handler by caching the option value that the editor expects to see.
	 */
	private int savedHandlerOptionBit = -1;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * This method is invoke by the UI system when a user clicks on a "show detail" check-box,
	 * such as in a popup-menu or a toolbar. Our goal is to toggle the state of the check-box
	 * (or check-boxes), then update the editor content to reflect the new state. For example,
	 * if the "Show Roots" check-box is currently unset, then start by setting the check-box,
	 * then refresh the editor so that path roots are shown.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
				
		/* get details of the command that was invoked */
		ICommandService service =
			(ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		Command cmd = event.getCommand();
		
		/* 
		 * Determine which "detail" was toggled, by looking at the parameter that's associated
		 * with this command (null == there is no parameter set).
		 */
		String paramValue = null;
		try {
			IParameter parms[] = cmd.getParameters();
			if (parms.length == 1) {
				String paramId = parms[0].getId();
				paramValue = event.getParameter(paramId);
			}
		} catch (NotDefinedException e) {
			throw new FatalError("Unable to find command: " + cmd.getId());
		}
		
		/* if there was no parameter, we don't know which option to change. Exit silently */
		if (paramValue == null) {
			throw new FatalError("Unable to find command parameters for: " + cmd.getId());
		}

		/* 
		 * Now update the editor itself so that the content of the editor reflects
		 * the new state of the command.
		 */
		MainEditor editor = (MainEditor)HandlerUtil.getActiveEditor(event);
		final SubEditor subEditor = (SubEditor)editor.getActiveSubEditor();
		
		if (savedHandlerOptionBit == -1) {
			savedHandlerOptionBit = computeOptionBits(paramValue);
		}
		boolean isSelected = subEditor.isOptionSet(savedHandlerOptionBit);
		isSelected = !isSelected;
		subEditor.setOption(savedHandlerOptionBit, isSelected);
		
		/*
		 * Ensure that all menu items and toolbar icons are toggled. This implicitly calls
		 * the updateElement() method (see below) on each of the widgets that needs to
		 * reflect the new state of the option.
		 */
		service.refreshElements(event.getCommand().getId(), null);
		
		/*
		 * Refresh the SubEditor content as a background task. This is necessary
		 * for the option's effect to be invoked (e.g. redrawing the tree viewer with
		 * the new option set).
		 */
		subEditor.refreshView(false);
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * This method is invoked whenever a check-box command is invoked. All elements that
	 * render this command (e.g. menus, tool-bars, etc) will have their check-box state
	 * modified by this method.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		
		/* 
		 * We need to query the current editor page to see what the settings for the 
		 * editor are.
		 */
		SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
		
		/* 
		 * Note: this handler instance is specific to the option we're setting/querying, so
		 * savedHandlerOptionBit won't change from one invocation to the next.
		 */
		if ((subEditor != null) && (savedHandlerOptionBit != -1)) {
			element.setChecked(subEditor.isOptionSet(savedHandlerOptionBit));
		}
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Translate an option name (as passed in via a Command's parameter) into a option value,
	 * as understood by SubEditor classes.
	 * @param param The option's name.
	 * @return The corresponding option value (EditorOptions.OPT_SHOW_*).
	 */
	private int computeOptionBits(String param) {		
		if (param.equals("path-roots")) {
			return EditorOptions.OPT_SHOW_ROOTS;
		} else if (param.equals("packages")) {
			return EditorOptions.OPT_SHOW_PACKAGES;
		} else if (param.equals("show-hidden")) {
			return EditorOptions.OPT_SHOW_HIDDEN;
		} else {
			throw new FatalError("Unable to handle command parameter: " + param);
		}
	}
	/*-------------------------------------------------------------------------------------*/
}
