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

package com.buildml.eclipse.packages.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.util.IColorConstant;

import com.buildml.eclipse.bobj.UIFileActionConnection;

/**
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
public class AddFileActionConnectionFeature extends AbstractAddFeature {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The colour of connection lines */
	private static final IColorConstant CONNECTION_COLOUR = IColorConstant.BLACK;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new AddFileActionConnectionFeature (will usually be a singleton).
	 * @param fp The FeatureProvider that owns this feature.
	 */
	public AddFileActionConnectionFeature(IFeatureProvider fp) {
		super(fp);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Display the connection line.
	 */
	public PictogramElement add(IAddContext context) {
				
		IAddConnectionContext addConContext = (IAddConnectionContext) context;
		UIFileActionConnection bo = (UIFileActionConnection) addConContext.getNewObject();
		IPeCreateService peCreateService = Graphiti.getPeCreateService();

		/* create a connection between the two points */
		Connection connection = peCreateService.createFreeFormConnection(getDiagram());
		connection.setStart(addConContext.getSourceAnchor());
		connection.setEnd(addConContext.getTargetAnchor());

		/* draw the line */
		IGaService gaService = Graphiti.getGaService();
		Polyline polyline = gaService.createPolyline(connection);
		polyline.setLineWidth(2);
		polyline.setForeground(manageColor(CONNECTION_COLOUR));
		
		/* draw the arrow */
	    ConnectionDecorator cd = peCreateService.createConnectionDecorator(connection, false, 1.0, true);
	    Polyline arrow = gaService.createPolyline(cd, new int[] { -10, 5, 0, 0, -10, -5 });
		arrow.setLineWidth(2);
		arrow.setForeground(manageColor(CONNECTION_COLOUR));
	    
		/* link the connection pictogram to the business object */
		link(connection, bo);

		return connection;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * This feature can handle adding UIFileActionConnection business objects.
	 */
	public boolean canAdd(IAddContext context) {
		if (context instanceof IAddConnectionContext
				&& context.getNewObject() instanceof UIFileActionConnection) {
			return true;
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
}

