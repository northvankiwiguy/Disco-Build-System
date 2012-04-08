package com.arapiki.disco.eclipse.files.handlers;

import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.State;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.files.DiscoFilesEditor;

/**
 * A handler for managing the state of various view options. These options are
 * triggered by the pop-up menu ("Show details"), or via the editor's toolbar icons.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerShowDetail implements IHandler, IElementUpdater {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** records the current state of the most recently executed command */
	private boolean isSelected = false;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/*
	 * Not implemented, but required for interface.
	 */
	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
		/* empty */
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Not implemented, but required for interface.
	 */
	@Override
	public void dispose() {
		/* empty */
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * This method is invoke by the UI system when a user clicks on a "show detail" check-box,
	 * such as in a popup-menu or a toolbar. Our goal is to toggle the state of the check-box
	 * (or check-boxes), then update the editor contenet to reflect the new state. For example,
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
			/* empty - there is no parameter */
		}
		
		/* toggle the state of the command */
		State state = cmd.getState(cmd.getId() + ".state");
		isSelected = !(Boolean)state.getValue();
		state.setValue((Boolean)isSelected);
		
		/* ensure that all menu items and toolbar icons are toggled */
		service.refreshElements(event.getCommand().getId(), null);
		
		/* if there was no parameter, we don't know which option to change. Exit silently */
		if (paramValue == null) {
			return null;
		}
		
		/* 
		 * Now update the editor itself so that the content of the editor reflects
		 * the new state of the command.
		 */
		DiscoMainEditor editor = (DiscoMainEditor)HandlerUtil.getActiveEditor(event);
		final DiscoFilesEditor subEditor = (DiscoFilesEditor)editor.getActiveSubEditor();
		
		if (paramValue.equals("path-roots")) {
			subEditor.setOption(DiscoFilesEditor.OPT_SHOW_ROOTS, isSelected);
		} else if (paramValue.equals("components")) {
			subEditor.setOption(DiscoFilesEditor.OPT_SHOW_COMPONENTS, isSelected);
		} else {
			/* unknown option - return silently */
			return null;
		}
		
		/*
		 * Refresh the DiscoFilesEditor content as a background task.
		 */
		subEditor.refreshView();						
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#isHandled()
	 */
	@Override
	public boolean isHandled() {
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Not implemented, but required for interface.
	 */
	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
		/* empty */
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
		element.setChecked(isSelected);
	}

	/*-------------------------------------------------------------------------------------*/
}
