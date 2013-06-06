/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.utils;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramLink;
import org.eclipse.graphiti.ui.platform.GraphitiShapeEditPart;

/**
 * Various static methods, for interacting with Graphiti.
 * @author Peter Smith <psmith@arapiki.com>
 */
public class GraphitiUtils {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Return the business object underlying the currently-selected Graphiti icon.
	 * 
	 * @param element The element that's currently selected.
	 * @param boClass The underlying class that we're expecting.
	 * @return The business object that underlies the selected pictogram.
	 */
	public static Object getSelectedBusinessObjects(IAdaptable element, Class<?> boClass) {
		
		/* the element must be a Graphiti shape */
		GraphitiShapeEditPart container = (GraphitiShapeEditPart)element;
		
		/* fetch the pictogram element, and its link to the underlying business object */
		PictogramElement pe = container.getPictogramElement();
		PictogramLink pl = pe.getLink();
		EList<EObject> list = pl.getBusinessObjects();
		
		/* we assume that there's only one business object (because that's how we set it up) */
		Object bo = list.get(0);
		if (boClass.isInstance(bo)) {
			return bo;
		}
		return null;
	}
	
	
	/*-------------------------------------------------------------------------------------*/
}
