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

package com.buildml.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.EditorPart;

import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.preferences.PreferenceConstants;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.PackageSet;
import com.buildml.utils.os.SystemUtils;
import com.buildml.utils.types.IntegerTreeRecord;
import com.buildml.utils.types.IntegerTreeSet;

/**
 * An abstract class that BuildML "import" editor tabs (such as FilesEditor and ActionsEditor)
 * should be derived from. Editors of this type can be placed within a tab of the top-level 
 * BuildML. They share common features, such as option settings, item visibility, and
 * the ability to filter based on a selected set of packages.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public abstract class ImportSubEditor extends EditorPart implements IElementComparer, ISubEditor {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The BuildStore we're editing */
	protected IBuildStore buildStore = null;
	
	/**
	 * The set of packages to be displayed (that is, files will be displayed
	 * if they belong to one of these packages).
	 */
	private PackageSet filterPackageSet;
	
	/**
	 * The current options setting for this editor. The field contains a bitmap of
	 * OPT_* values from the EditorOptions interface.
	 */
	private int editorOptionBits = 0;
	
	/**
	 * If a new editor tab is created with fewer than this many visible tree entries,
	 * we should auto-expand the entire tree so that all elements are visible. If there
	 * are more than this many elements, only expand the first couple of levels.
	 */
	protected static final int AUTO_EXPAND_THRESHOLD = 200;
	
	/**
	 * Indicates whether this editor is "removable". That is, can the user close the
	 * tab (the default is "true"), or is this tab permanently fixed to the editor (false)
	 */
	private boolean removable = true;
	
	/**
	 * Set to true the first time the user is warned about overriding the BUILDML_PATH.
	 * They should only see the message once per Eclipse invocation, so this field is static.
	 */
	private static boolean warnedAboutPathOverride = false;
	
	/**
	 * Record whether or not we've been disposed. Nobody should be allowed to make this
	 * sub-editor active if it's disposed.
	 */
	private boolean editorIsDisposed = false;

	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/

	/**
	 * Create a new SubEditor instance, using the specified BuildStore as input
	 * @param buildStore The BuildStore to display/edit.
	 * @param tabTitle The text to appear on the editor's tab.
	 */
	public ImportSubEditor(IBuildStore buildStore, String tabTitle) {
		super();
	
		/* set the name of the tab that this editor appears in */
		setPartName(tabTitle);
	
		/* Save away our BuildStore information, for later use */
		this.buildStore = buildStore;
		
		
		/* create a new package set so we can selectively filter out packages */
		filterPackageSet = new PackageSet(buildStore);
		filterPackageSet.setDefault(true);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		/* not implemented - is handled by MainEditor */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		/* not implemented - is handled by MainEditor */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#isDirty()
	 */
	@Override
	public boolean isDirty() {
		/* not implemented - is handled by MainEditor */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		/* not implemented - is handled by MainEditor */
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getEditorImage()
	 */
	@Override
	public Image getEditorImage() {
		
		/* ask the subeditor instance where its image is (if it has one) */
		String path = getEditorImagePath();
		if (path != null) {
			
			/* 
			 * Create a descriptor, and perhaps a new image, if it's not already
			 * available in this plugin's image registry.
			 */
			ImageDescriptor imageDescr = Activator.getImageDescriptor(path);
			ImageRegistry pluginImageRegistry = Activator.getDefault().getImageRegistry();
			Image iconImage = pluginImageRegistry.get(imageDescr.toString());
			if (iconImage == null) {
				iconImage = imageDescr.createImage();
				pluginImageRegistry.put(imageDescr.toString(), iconImage);
			}
			return iconImage;
		}

		/* no icon for this editor */
		else {
			return null;
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setOption(int, boolean)
	 */
	@Override
	public void setOption(int optionBits, boolean enable)
	{
		/* if enable is set, then we're adding the new options */
		if (enable) {
			editorOptionBits |= optionBits;
		}
		
		/* else, we're clearing the options */
		else {
			editorOptionBits &= ~optionBits;
		}
		
		/* The sub-editor may now need to react to the change in settings. */
		updateEditorWithNewOptions(optionBits, enable);		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setOptions(int)
	 */
	@Override
	public void setOptions(int optionBits)
	{
		/* we call setOptions for each option, to ensure that side-effects are triggered */ 
		for (int bitNum = 0; bitNum != EditorOptions.NUM_OPTIONS; bitNum++) {
			int thisBitMap = (1 << bitNum);
			
			/* explicitly enable or disable this option */
			setOption(thisBitMap, (optionBits & thisBitMap) != 0);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getOptions()
	 */
	@Override
	public int getOptions()
	{
		return editorOptionBits;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#isOptionSet(int)
	 */
	@Override
	public boolean isOptionSet(int optionBit)
	{
		return (editorOptionBits & optionBit) != 0;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#updateOptionsFromPreferenceStore()
	 */
	@Override
	public void updateOptionsFromPreferenceStore()
	{
		IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		
		setOption(EditorOptions.OPT_COALESCE_DIRS, 
				prefStore.getBoolean(PreferenceConstants.PREF_COALESCE_DIRS));
		
		/*
		 * Determine where the BuildML binaries and libraries are kept. By default we get
		 * them from within the plugin jar file, although the user is permitted
		 * to override that path. If they do, however, they should be warned.
		 */
		String buildMlPath = null;
		URL url = FileLocator.find(Activator.getDefault().getBundle(), new Path("/files"), null);
		if (url != null) {
			try {
				URL filesDirUrl = FileLocator.toFileURL(url);
				buildMlPath = filesDirUrl.getPath();
			} catch (IOException e) {
				/* nothing - buildMlPath stays null, which indicates an error */
			}
		}

		/*
		 * If we can't locate the /files directory within the plugin, that's likely because we're
		 * running this plugin within the eclipse PDE (as opposed to the plugin being installed
		 * and executed in the normal way). If this is the case, then buildMlPath == null.
		 * 
		 * In any situation, the user is welcome to override the value of buildMlPath with their
		 * own setting. However, if they're not using the plugin jar's copy of the files, we
		 * should warn them.
		 */
		String prefBuildMlPath = prefStore.getString(PreferenceConstants.PREF_BUILDML_HOME);
		if (!prefBuildMlPath.isEmpty() && !warnedAboutPathOverride) {
			if (buildMlPath != null) {
				AlertDialog.displayWarningDialog("Overriding BUILDML_HOME setting",
						"Although the bin and lib directories have been found in the plugin jar file, " +
						"you have chosen to override the path. Please go into the BuildML preferences " +
						"if you wish to remove this override.");
				warnedAboutPathOverride = true;
			}
			buildMlPath = prefBuildMlPath;
		}
		
		/*
		 * Check that the BUILDML_HOME preference is set, is a directory, and contains subdirectories
		 * "lib" and "bin".
		 */
		if ((buildMlPath == null) || buildMlPath.isEmpty()) {
			AlertDialog.displayErrorDialog("Missing Preference Setting", 
					"The preference setting: \"Directory containing BuildML's bin and lib directories\" " +
					"is not defined. Please go into the BuildML preferences and set a suitable value.");
		}
		else {
			File buildMlPathFile = new File(buildMlPath);
			if (!(buildMlPathFile.isDirectory()) ||
					(!new File(buildMlPathFile, "bin").isDirectory()) ||
					(!new File(buildMlPathFile, "lib").isDirectory())) {
				AlertDialog.displayErrorDialog("Invalid Preference Setting", 
						"The preference setting: \"Directory containing BuildML's bin and lib directories\" " +
						"does not refer to a valid directory.");
			}
			/* 
			 * Else, the path is good. The only additional requirement is that the 'cfs' command
			 * be executable. This won't be the case if the /files directory has just been
			 * extracted into the Eclipse configuration directory for the first time. Note that
			 * chmod can fail if the files aren't owned by the current user, but that's not a
			 * problem for us.
			 */
			else {
				System.setProperty("BUILDML_HOME", buildMlPath);
				SystemUtils.chmod(buildMlPath + "/bin/cfs", 0755);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(final Composite parent) {
		/* 
		 * Update this editor's option by reading the user-specified values in the
		 * preference store. Also, attach a listener so that we hear about future
		 * changes to the preference store and adjust our options accordingly.
		 */
		updateOptionsFromPreferenceStore();
		Activator.getDefault().getPreferenceStore().
					addPropertyChangeListener(preferenceStoreChangeListener);
		
		/* 
		 * Resizing the top-level shell causes columns to be realigned/redrawn. We need
		 * to schedule this as a UI thread runnable, since we don't want it to run until
		 * after the resizing has finished, at which point we know the new window size.
		 * TODO: add a removeListener.
		 */
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				refreshView(false);
			}
		});
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Listener to identify changes being made to this plug-in's preference store, typically
	 * as part of editing the BuildMl preferences (this could change how our editor is displayed).
	 */
	private IPropertyChangeListener preferenceStoreChangeListener =
		new IPropertyChangeListener() {

			/**
			 * Completely redraw the files editor tree, based on the new preference
			 * settings.
			 */
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				updateOptionsFromPreferenceStore();
			}
		};
		
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		
		editorIsDisposed = true;
		
		/* remove this preference store listener */
		Activator.getDefault().getPreferenceStore().
		removePropertyChangeListener(preferenceStoreChangeListener);
		super.dispose();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getFilterPackageSet()
	 */
	@Override
	public PackageSet getFilterPackageSet() {
		return filterPackageSet;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setFilterPackageSet(com.buildml.model.types.PackageSet)
	 */
	@Override
	public void setFilterPackageSet(PackageSet newSet) {
		filterPackageSet = newSet;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setRemovable(boolean)
	 */
	@Override
	public void setRemovable(boolean removable) {
		this.removable = removable;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#isRemovable()
	 */
	@Override
	public boolean isRemovable() {
		return removable;
	}
		
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#isDisposed()
	 */
	@Override
	public boolean isDisposed() {
		return editorIsDisposed;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * This method is used by TreeViewers to compare elements in the tree for "sameness".
	 * This is useful when refreshing a TreeViewer, to ensure that expanded elements stay
	 * expanded after a refresh.
	 */
	@Override
	public boolean equals(Object a, Object b) {
				
		if (a == b) {
			return true;
		}

		/* 
		 * Ensure that both a and b have the same class, which must be a class that's
		 * derived from UIInteger.
		 */
		if ((a instanceof UIInteger) && (a.getClass() == b.getClass())) {
			return ((UIInteger)a).getId() == ((UIInteger)b).getId();		
		}
		
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IElementComparer#hashCode(java.lang.Object)
	 */
	@Override
	public int hashCode(Object element) {

		if (!(element instanceof IntegerTreeRecord)) {
			return 0;
		}		
		return ((IntegerTreeRecord)element).getId();
	}
	
	/*=====================================================================================*
	 * ABSTRACT METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#expandSubtree(java.lang.Object)
	 */
	@Override
	public abstract void expandSubtree(Object node);
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * When a sub-editor's options are modified, this method is called so that the editor
	 * can react appropriately to its now settings.
	 * @param optionBits The option bit(s) that were changed.
	 * @param enable True if the option(s) were added, else false.
	 */
	protected abstract void updateEditorWithNewOptions(int optionBits, boolean enable);
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#refreshView(boolean)
	 */
	@Override
	public abstract void refreshView(boolean force);
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setVisibilityFilterSet(com.buildml.utils.types.IntegerTreeSet)
	 */
	@Override
	public abstract void setVisibilityFilterSet(IntegerTreeSet visibleActions);
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getVisibilityFilterSet()
	 */
	@Override
	public abstract IntegerTreeSet getVisibilityFilterSet();
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#setItemVisibilityState(java.lang.Object, boolean)
	 */
	@Override
	public abstract void setItemVisibilityState(Object item, boolean state);
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#pageChange()
	 */
	@Override
	public abstract void pageChange();

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#hasFeature(java.lang.String)
	 */
	@Override
	public abstract boolean hasFeature(String feature);
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#doCopyCommand(org.eclipse.swt.dnd.Clipboard, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public abstract void doCopyCommand(Clipboard clipboard, ISelection selection);

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.ISubEditor#getEditorImagePath()
	 */
	@Override
	public abstract String getEditorImagePath();

	/*-------------------------------------------------------------------------------------*/
}