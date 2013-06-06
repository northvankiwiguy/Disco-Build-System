package com.buildml.eclipse.packages.handlers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramLink;
import org.eclipse.graphiti.ui.platform.GraphitiShapeEditPart;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.buildml.eclipse.utils.EclipsePartUtils;

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
		
		if (property.equals("isGraphitiBusinessObjectType")) {
			
			/* 
			 * The plugin.xml file must provide exactly one argument (a class name) to
			 * match against.
			 */
			if ((args.length == 1) && (args[0] instanceof String)) {
				
				/* get the currently selected object (there may only be one selected) */
				IStructuredSelection selection = EclipsePartUtils.getSelection();
				if ((selection == null) || (selection.size() != 1)) {
					return false;
				}
				
				/* we expect this object to be some type of Graphiti object */
				Object object = selection.getFirstElement();
				if (!(object instanceof GraphitiShapeEditPart)) {
					return false;
				}
				
				/* 
				 * Now figure out the business object that's selected. We must first
				 * identify the Graphiti pictogram element and follow the "link" to
				 * the business object.
				 */
				PictogramElement pe = ((GraphitiShapeEditPart)object).getPictogramElement();
				PictogramLink pl = pe.getLink();
				EList<EObject> businessObjects = pl.getBusinessObjects();
				if (businessObjects.size() != 1) {
					return false;
				}
				
				/* 
				 * Finally, compare the actual class with the expected class - first stripping
				 * off the word "class" that the getClass() method returns.
				 */
				String className = businessObjects.get(0).getClass().toString();
				String classNameWords[] = className.split(" ");
				return args[0].equals(classNameWords[1]);
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
}
