/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.arapiki.disco.main.commands;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.xml.sax.SAXException;

import com.arapiki.disco.main.CliUtils;
import com.arapiki.disco.main.ICliCommand;
import com.arapiki.disco.model.BuildStore;
import com.buildml.scanner.FatalBuildScannerError;
import com.buildml.scanner.electricanno.ElectricAnnoScanner;
import com.buildml.utils.files.ProgressFileInputStreamListener;

/**
 * Disco CLI Command class that implements the "scan-ea-anno" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandScanEaAnno implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/scan-ea-anno.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "scan-ea-anno";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		return new Options();
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<emake-annotation-file>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Scan an Electric Accelerator annotation file.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {
		/* no options - nothing to do */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#invoke(com.arapiki.disco.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {
		
		CliUtils.validateArgs(getName(), args, 1, 1, "An Electric Accelerator annotation file must be specified.");

		String fileName = args[0];
		ElectricAnnoScanner eas = new ElectricAnnoScanner(buildStore);
		try {
			/* create a listener that will monitor/display our progress in parsing the file */
			ProgressFileInputStreamListener listener = new ProgressFileInputStreamListener() {
				@Override
				public void progress(long current, long total, int percentage) {
					System.out.print("\rPercentage complete: " + percentage + "%");
				}
				@Override
				public void done() {
					System.out.println();
				}
			};

			/* now parse - the listener will update us as we go */
			eas.parse(fileName, listener);

		} catch (FileNotFoundException e) {
			CliUtils.reportErrorAndExit("ElectricAccelerator annotation file " + fileName + " not found.");

		} catch (IOException e) {
			CliUtils.reportErrorAndExit("I/O error while reading ElectricAccelerator annotation file " + fileName + ".");

		} catch (SAXException e) {
			CliUtils.reportErrorAndExit("Unexpected syntax in ElectricAccelerator annotation file " + fileName + ".");

		} catch (FatalBuildScannerError e) {
			CliUtils.reportErrorAndExit("Logic problem while scanning ElectricAccelerator annotation file " + 
					fileName + "\n" + e.getMessage());
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
