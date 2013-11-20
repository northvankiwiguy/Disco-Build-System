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
import org.eclipse.graphiti.mm.algorithms.Polygon;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.util.IColorConstant;

import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.bobj.UIMergeFileGroupConnection;

/**
 * A Graphiti feature for drawing the connection areas between file groups and the
 * merge file groups they're integrated into.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class AddMergeFileGroupConnectionFeature extends AbstractAddFeature {

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
	public AddMergeFileGroupConnectionFeature(IFeatureProvider fp) {
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
		UIMergeFileGroupConnection bo = (UIMergeFileGroupConnection) addConContext.getNewObject();
		IPeCreateService peCreateService = Graphiti.getPeCreateService();

		/* create a connection between the two points */
		Connection connection = peCreateService.createFreeFormConnection(getDiagram());
		connection.setStart(addConContext.getSourceAnchor());
		connection.setEnd(addConContext.getTargetAnchor());
		
		/* draw the line */
		IGaService gaService = Graphiti.getGaService();
		Polyline polyline = gaService.createPolyline(connection);
		polyline.setLineWidth(1);
		polyline.setLineStyle(LineStyle.DOT);
		polyline.setForeground(manageColor(CONNECTION_COLOUR));
		
		/* draw an optional filter symbol */
		if (bo.hasFilter()) {
			ConnectionDecorator cd = peCreateService.createConnectionDecorator(connection, false, 0.5, true);
			Polygon filter = gaService.createPolygon(cd, AddFileActionConnectionFeature.FILTER_COORDS);
			filter.setBackground(manageColor(AddFileActionConnectionFeature.FILTER_COLOUR));
			filter.setForeground(manageColor(AddFileActionConnectionFeature.FILTER_COLOUR));
		}

		/* link the connection pictogram to the business object */
		link(connection, bo);

		return connection;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * This feature can handle adding UIMergeFileGroupConnection business objects.
	 */
	public boolean canAdd(IAddContext context) {
		return (context instanceof IAddConnectionContext
				&& context.getNewObject() instanceof UIMergeFileGroupConnection);
	}

	/*-------------------------------------------------------------------------------------*/
}

