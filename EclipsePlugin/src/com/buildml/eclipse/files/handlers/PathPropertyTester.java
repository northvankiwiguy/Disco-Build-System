package com.buildml.eclipse.files.handlers;

import java.util.List;

import org.eclipse.core.expressions.PropertyTester;

import com.buildml.eclipse.files.UIFileRecordDir;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.BuildStore;
import com.buildml.model.FileNameSpaces;

/**
 * A Eclipse plugin "PropertyTester" class for determining whether
 * the specified path represents a directory that has an attached
 * file root.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PathPropertyTester extends PropertyTester {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Test the "hasAttachedRoot" property of the currently selected path to see if it
	 * has an attached file root.
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {

		/* the property name must be "hasAttachedRoot" */
		if (property.equals("hasAttachedRoot")) {
			if (receiver instanceof List) {
				List selection = (List)receiver;
				
				/* there must be one path selected on the UI */
				if (selection.size() == 1) {
					
					/* the selection must be a directory (not a file) */
					if (selection.get(0) instanceof UIFileRecordDir) {
						UIFileRecordDir node = (UIFileRecordDir)selection.get(0);
						int pathId = node.getId();
						
						/* finally, test if the path has an attached root */
						BuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
						if (buildStore != null) {
							FileNameSpaces pathMgr = buildStore.getFileNameSpaces();
							return pathMgr.getRootAtPath(pathId) != null;
						}
					}
				}
			}
			return false;
		}
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
