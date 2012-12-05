/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.utils.dnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

/**
 * A drag/drop Transfer type for transfer BuildML elements between views/editors. An
 * "element" is a file, action, package, package folder, etc., which all have internal
 * numeric IDs within a BuildStore. Therefore, to move one of these elements, we simply
 * need to specify the the top of element and the ID number.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class BuildMLTransfer extends ByteArrayTransfer {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** This Transfer's type name */
	private static final String MY_TYPE_NAME = "BuildMLTransferType";
	
	/** The type ID (provided by the drag/drop framework */
	private static final int MY_TYPE_ID = registerType(MY_TYPE_NAME);
	
	/** The singleton instance of this class */
	private static BuildMLTransfer _instance = new BuildMLTransfer();

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * @return The singleton instance of the BuildMLTransfer class.
	 */
	public static BuildMLTransfer getInstance() {
		return _instance;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/** 
	 * Private constructor - This class can't be instantiated
	 */
	private BuildMLTransfer() {}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/*
	 * Serialize one or more BuildTransferType objects into their byte-stream representation.
	 * It is this byte buffer that will be passed from drag-source to drop-target.
	 */
	@Override
	public void javaToNative(Object object, TransferData transferData) {
	
		/* we can only transfer a valid array of BuildMLTransferType */
		if ((object == null) || !(object instanceof BuildMLTransferType[])) {
			return;
		}
		
		if (isSupportedType(transferData)) {
			BuildMLTransferType[] myTypes = (BuildMLTransferType[])object;
			try {
				/* write data to a byte array */
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream writeOut = new DataOutputStream(out);
				
				/* 
				 * Write all transferring types in the same buffer. We write
				 * the type and id as integers, and the BuildStore reference
				 * using its toString() form.
				 */
				int length = myTypes.length;
				writeOut.writeInt(length);
				
				/* for each BuildMLTransferType in the input array, serialize the object */
				for (int i = 0; i < length; i++) {
					byte[] buildStoreBuffer = myTypes[i].owner.toString().getBytes();
					writeOut.writeInt(buildStoreBuffer.length);
					writeOut.write(buildStoreBuffer);
					writeOut.writeInt(myTypes[i].type);
					writeOut.writeInt(myTypes[i].id);
				}
				byte[] buffer = out.toByteArray();
				writeOut.close();

				/* ask our super class to perform the transfer */
				super.javaToNative(buffer, transferData);
				
			} catch (IOException e) {
				/* won't happen when writing to a byte array */
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * De-serialized one or more BuildMLTransferType objects from an incoming byte stream
	 * into a outgoing array of objects. This is used when a byte stream is sent from
	 * drag-source to drop-target, and needs to be converted back into objects.
	 */
	@Override
	protected Object nativeToJava(TransferData transferData) {
		
		if (isSupportedType(transferData)) {
			
			/* ask our parent class for the raw transferred data */
			byte buffer[] = (byte[])super.nativeToJava(transferData);
			if (buffer == null) {
				return null;
			}
			
			/* read the objects from the byte array */
			ByteArrayInputStream in = new ByteArrayInputStream(buffer);
			DataInputStream readIn = new DataInputStream(in);
			
			BuildMLTransferType[] myData = null;
			try {
				int length = readIn.readInt();
				myData = new BuildMLTransferType[length];
				
				/* for each entry, create a new BuildMLTransferType... */
				for (int i = 0; i != length; i++) {
					int buildStoreSize = readIn.readInt();
					byte[] buildStoreBuffer = new byte[buildStoreSize];
					readIn.read(buildStoreBuffer);
					int type = readIn.readInt();
					int id = readIn.readInt();
					myData[i] = new BuildMLTransferType(new String(buildStoreBuffer),
													    type, id);
				}
			} catch (IOException e) {
				return null;
			}
			return myData;
		}
		return null;
	}
		
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.Transfer#getTypeNames()
	 */
	@Override
	protected String[] getTypeNames() {
		return new String[] { MY_TYPE_NAME };
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.swt.dnd.Transfer#getTypeIds()
	 */
	@Override
	protected int[] getTypeIds() {
		return new int[] { MY_TYPE_ID };
	}
	
	/*-------------------------------------------------------------------------------------*/
}
