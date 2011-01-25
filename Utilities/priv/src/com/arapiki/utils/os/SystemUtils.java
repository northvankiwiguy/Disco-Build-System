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

package com.arapiki.utils.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 * TODO: // fix this to be more error proof.
 */
public class SystemUtils {
	
	public static String executeShellCmd(String cmd) 
		throws IOException, InterruptedException {
		
		Runtime run = Runtime.getRuntime();
		Process pr = run.exec(cmd);
		pr.waitFor();
		BufferedReader buf = 
			new BufferedReader(new InputStreamReader(pr.getInputStream()));
		
		StringBuffer result = new StringBuffer();
		String line;
		while ((line = buf.readLine()) != null) {
			result.append(line);
		}
		return result.toString();
	}
}
