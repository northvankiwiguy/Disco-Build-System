/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.arapiki.disco.eclipse.utils;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * A VisibilityTreeViewer is the same as a TreeViewer, but has special features for
 * hiding certain elements. A non-visible element can either be removed completely from the
 * Tree (i.e. filtered out), or can be greyed-out (still visible, but obviously not selected).
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class VisibilityTreeViewer extends TreeViewer {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * The externally-registered IVisibilityProvider used by this VisibilityTreeViewer
	 * to indicate whether specific elements should be visible in the Tree control, or
	 * not. 
	 */
	private IVisibilityProvider visibilityProvider = null;
	
	/** The TreeViewer filters to use to reveal all paths */
	private ViewerFilter revealViewerFilters[];
	
	/** The TreeViewer filters to use to hide invisible paths */
	private ViewerFilter hiddenViewerFilters[];
	
	/**
	 * If true, show hidden elements as being greyed-out. If false, don't
	 * show them at all.
	 */
	boolean greyVisibilityMode = true;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new VisibilityTreeViewer class. 
	 * @param tree The Tree widget that this TreeViewer will wrap.
	 */
	public VisibilityTreeViewer(Tree tree) {
		super(tree);
		
		/* 
		 * Filter for revealing invisible elements so that they can be shown greyed out
		 * (actually, this disables all filters).
		 */
		revealViewerFilters = new ViewerFilter[0];
		
		/* Filter for hiding non-visible elements so they won't be seen at all. */
		hiddenViewerFilters = new ViewerFilter[1];
		hiddenViewerFilters[0] = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return visibilityProvider.isVisible(element);
			}
		};
		
		/* disabled greyed-out mode */
		setGreyVisibilityMode(false);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * For a given TreeViewer element, set its visibility state. A visible element will appear,
	 * as normal, but a non-visible element will either be greyed out, or not render at all.
	 * 
	 * @param element The element to be made visible or non-visible.
	 * @param state True to make it visible, else false.
	 */
	public void setVisibility(Object element, boolean state)
	{
		/* 
		 * First, ensure that there's an underlying model, otherwise everything
		 * should be considered to be selected.
		 */
		if (visibilityProvider == null) {
			return;
		}
		
		/* Set visibility of this element in the underlying model. */		
		visibilityProvider.setVisibility(element, state);
		
		/* refresh the whole TreeViewer so that things appear/disappear/re-colour */
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				refresh();
			}
		});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set this viewer's visibility provider class. This provider informs the TreeViewer
	 * which elements in the Tree are to be visible (or non-greyed-out) versus being
	 * invisible (or greyed-out).
	 * 
	 * @param visibilityProvider An external provider class (implementing IVisibilityProvider).
	 */
	public void setVisibilityProvider(IVisibilityProvider visibilityProvider)
	{
		this.visibilityProvider = visibilityProvider;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Specify whether this TreeViewer should render non-visible elements using
	 * grey font, or whether they should not be rendered at all. 
	 * @param greyVisibility True if non-visible element should be greyed out,
	 * 			or false if they should not be shown at all.
	 */
	public void setGreyVisibilityMode(boolean greyVisibility) {
		
		if (greyVisibility) {
			/* disable the TreeViewer filter, so we'll see the non-visible element */
			setFilters(revealViewerFilters);
		} else {
			setFilters(hiddenViewerFilters);
		}		
		this.greyVisibilityMode = greyVisibility;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether this TreeViewer will show non-visible elements using a grey font,
	 * as opposed to not rendering them at all.
	 * @return True if non-visible element are being greyed-out, else false.
	 */
	public boolean getGreyVisibilityMode() {
		return greyVisibilityMode;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/*
	 * Overrides the parent class's doUpdateItem(). We use the visibility provider object
	 * (if it exists) to colour the widget appropriately.
	 */
	protected void doUpdateItem(Widget widget, Object element, boolean fullMap)
	{
		/* invoke the super class to do most of the work */
		super.doUpdateItem(widget, element, fullMap);
		
		/* 
		 * now, if there's a filter registered, ask it whether this element
		 * should be highlighted
		 */
		if ((visibilityProvider != null) && (widget instanceof TreeItem)) {
			setWidgetHighlight((TreeItem)widget, 
					visibilityProvider.isVisible(element));
		}
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Internal method for setting the colour of a widget (to black or grey), depending
	 * on whether the corresponding tree element is visible or not.
	 * @param item The Widget to be coloured.
	 * @param state True to highlight the widget with black, else false to grey it out.
	 */
	private void setWidgetHighlight(TreeItem item, boolean state) {
		if (state) {
			item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		} else {
			item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
