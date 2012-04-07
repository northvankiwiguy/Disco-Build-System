package com.arapiki.disco.eclipse;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class for the Disco editor. A single instance of this class is
 * created when the plug-in is loaded. It provides a central location for
 * accessing the plug-in's features.
 */
public class Activator extends AbstractUIPlugin {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The plug-in ID for the Disco editor */
	public static final String PLUGIN_ID = "com.arapiki.disco";

	/** The shared instance of this plug-in (a singleton) */
	private static Activator plugin;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new Activator object (although typically you would use getDefault()
	 * to return the single instance of this class.
	 */
	public Activator() {
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the shared instance. This is used by many different classes for
	 * retrieving the single instance of the Activator class.
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
