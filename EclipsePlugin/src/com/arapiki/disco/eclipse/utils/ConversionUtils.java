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

import com.arapiki.disco.model.types.FileRecord;

/**
 * A collection of static methods implementing type conversions, as required
 * by the Eclipse GUI.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ConversionUtils {
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given an Integer[], return a corresponding FileRecord[]
	 * @param intArr The Integer[] to be converted.
	 * @return The equivalent FileRecord[].
	 */
	public static FileRecord[] convertIntArrToFileRecordArr(
			Integer[] intArr)
	{
		FileRecord result[] = new FileRecord[intArr.length];
		for (int i = 0; i < intArr.length; i++) {
			result[i] = new FileRecord(intArr[i]);
		}
		return result;		
	}
	
	/*-------------------------------------------------------------------------------------*/
}

