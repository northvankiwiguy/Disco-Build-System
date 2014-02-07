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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramLink;
import org.eclipse.graphiti.pattern.IPattern;
import org.eclipse.graphiti.ui.platform.GraphitiConnectionEditPart;
import org.eclipse.graphiti.ui.platform.GraphitiShapeEditPart;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.buildml.eclipse.bobj.UIPackage;
import com.buildml.eclipse.packages.DiagramFeatureProvider;
import com.buildml.eclipse.packages.PackageDiagramEditor;

/**
 * Various static methods, for interacting with Graphiti.
 * @author Peter Smith <psmith@arapiki.com>
 */
public class GraphitiUtils {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Return the business object underlying the specified Graphiti element. This is
	 * primarily useful in PropertyPages where the element is obtained from getElement().
	 * 
	 * @param element The element that's currently selected.
	 * @param boClass The underlying class that we're expecting.
	 * @return The business object that underlies the selected pictogram.
	 */
	public static Object getBusinessObjectFromElement(IAdaptable element, Class<?> boClass) {
		
		/* the element must be a Graphiti shape or connection */
		PictogramElement pe = null;
		if (element instanceof GraphitiShapeEditPart) {
			GraphitiShapeEditPart container = (GraphitiShapeEditPart)element;
			pe = container.getPictogramElement();

		} else if (element instanceof GraphitiConnectionEditPart) {
			GraphitiConnectionEditPart container = (GraphitiConnectionEditPart)element;
			pe = container.getPictogramElement();
		}
		if (pe instanceof Diagram) {
			PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
			if (pde != null) {
				return new UIPackage(pde.getPackageId());
			}
		}
		
		if (pe == null) {
			return null;
		}
		PictogramLink pl = pe.getLink();
		if (pl == null) {
			return null;
		}
		EList<EObject> list = pl.getBusinessObjects();
		
		/* we assume that there's only one business object (because that's how we set it up) */
		Object bo = list.get(0);
		if (boClass.isInstance(bo)) {
			return bo;
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a Graphiti pictogram, return the underlying business object. This is useful for
	 * identifying the underlying UIAction, UIFileGroup etc. that a was selected on a Graphiti
	 * diagram. Selecting the Diagram itself will return the corresponding UIPackage object.
	 * 
	 * @param pictogram The Graphiti pictogram selected on the Graphiti diagram.
	 * @return The underlying business object (UIAction, UIFileGroup etc), or null if the
	 * pictogram is not a recognized type.
	 */
	public static Object getBusinessObject(Object pictogram) {

		if (pictogram instanceof Diagram) {
			PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
			if (pde != null) {
				return new UIPackage(pde.getPackageId());
			}
		}
		
		else if (pictogram instanceof PictogramElement) {
			PictogramElement cs = (PictogramElement)pictogram;
			PictogramLink pl = cs.getLink();
			if (pl != null) {
				EList<EObject> bos = pl.getBusinessObjects();
				if ((bos != null) && (bos.size() == 1)) {
					return bos.get(0);
				}
			}			
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return a list of the objects currently selected on the Graphiti Diagram. The returned
	 * list will only contain business objects (e.g. UIFileGroup, etc). This method is
	 * useful for converting the Graphiti EditPart objects into objects that we actually
	 * care about.
	 * 
	 * @return A list of business objects currently selected, or null if there's an
	 * error fetching the selection.
	 */
	public static List<Object> getSelection() {
		
		IStructuredSelection selectedParts = EclipsePartUtils.getSelection();
		if (selectedParts == null) {
			return null;
		}
		
		/* traverse the list of selected "edit parts" and convert to business objects */
		List<Object> result = new ArrayList<Object>();
		Iterator<Object> iter = selectedParts.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			PictogramElement pe = null;
			
			/* handle shapes (actions, file groups, etc) */
			if (element instanceof GraphitiShapeEditPart) {
				GraphitiShapeEditPart shapeEditPart = (GraphitiShapeEditPart)element;
				pe = shapeEditPart.getPictogramElement();
			}
			
			/* handle connection arrows */
			else if (element instanceof GraphitiConnectionEditPart) {
				GraphitiConnectionEditPart connectionEditPart = (GraphitiConnectionEditPart)element;
				pe = connectionEditPart.getPictogramElement();
			}
			
			/* convert Pictogram Element into business object, and add to result list */
			if (pe != null) {
				Object bo = getBusinessObject(pe);
				if (bo != null) {
					result.add(bo);
				}
			}
		
		}
		return result;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the DiagramFeatureProvider for the currently active PackageDiagramEditor.
	 * @return The DiagramFeatureProvider, or null if none is active.
	 */
	public static DiagramFeatureProvider getActiveGraphitiFeatureProvider() {
		PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
		if (pde == null) {
			return null;
		}
		IDiagramTypeProvider dtp = pde.getDiagramTypeProvider();
		if (dtp == null) {
			return null;
		}
		return (DiagramFeatureProvider) dtp.getFeatureProvider();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For the currently active package diagram, return the Pattern object that implements
	 * the Graphiti pattern for the specified pictogram.
	 * 
	 * @param pictogramElement The pictogram that is managed by the pattern we're searching for.
	 * @return The pattern object, or null if it couldn't be found.
	 */
	public static IPattern getPattern(PictogramElement pictogramElement) {
		DiagramFeatureProvider dfp = getActiveGraphitiFeatureProvider();
		if (dfp == null) {
			return null;
		}
		return dfp.getPatternForPictogramElement(pictogramElement);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
