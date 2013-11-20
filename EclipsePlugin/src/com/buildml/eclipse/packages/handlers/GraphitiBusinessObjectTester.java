package com.buildml.eclipse.packages.handlers;

import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import com.buildml.eclipse.utils.GraphitiUtils;

/**
 * Eclipse plugin.xml property tester - used in &lt;enableWhen&gt; clauses to determine
 * whether the currently selected objects are Graphiti pictogramElements that have
 * a specific business object type. For example, we may test whether the user has selected
 * a "UIAction" object.
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
public class GraphitiBusinessObjectTester extends PropertyTester {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * This method is called by the plugin system, usually when evaluating a &ltenableWhen&gt
	 * clause.
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		
		if (property == null) {
			return false;
		}
		
		if (property.equals("isGraphitiBusinessObjectType")) {
			
			/* 
			 * The plugin.xml file must provide exactly one argument (a class name) to
			 * match against.
			 */
			if ((args.length == 1) && (args[0] instanceof String)) {
				
				List<Object> selection = GraphitiUtils.getSelection();
				if (selection.size() != 1) {
					return false;
				}
				
				/* 
				 * Finally, compare the actual class with the expected class - first stripping
				 * off the word "class" that the getClass() method returns.
				 */
				String className = selection.get(0).getClass().toString();
				String classNameWords[] = className.split(" ");
				return args[0].equals(classNameWords[1]);
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
}
