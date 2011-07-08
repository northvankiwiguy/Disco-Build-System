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

package com.arapiki.utils.string;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Test methods for the StringArray class
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestStringArray {

	/**
	 * Test method for {@link com.arapiki.utils.string.StringArray#shiftLeft(java.lang.String[], int)}.
	 */
	@Test
	public void testShiftLeftStringArrayInt() {

		String emptyArray[] = new String[0];
		String oneArray[] = new String[] {"one"};
		String twoArray[] = new String[] {"one", "two"};
		String largeArray[] = new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};

		/* test with an empty array */
		String result[] = StringArray.shiftLeft(emptyArray, 1);
		assertEquals(0, result.length);
		
		result = StringArray.shiftLeft(emptyArray, 10);
		assertEquals(0, result.length);
		
		/* test with an array of one */
		result = StringArray.shiftLeft(oneArray, 0);
		assertArrayEquals(result, oneArray);
		
		/* we expect to have a new copy of the array, not the original */
		assertTrue(result != oneArray);

		result = StringArray.shiftLeft(oneArray, 1);
		assertEquals(0, result.length);
		
		result = StringArray.shiftLeft(oneArray, 10);
		assertEquals(0, result.length);

		/* test with an array of two */
		result = StringArray.shiftLeft(twoArray, 0);
		assertArrayEquals(result, twoArray);
		
		result = StringArray.shiftLeft(twoArray, 1);
		assertArrayEquals(result, new String[] {"two"});
		
		result = StringArray.shiftLeft(twoArray, 10);
		assertEquals(0, result.length);
		
		/* test with a larger array, and shift left by 1 */
		result = StringArray.shiftLeft(largeArray, 1);
		assertArrayEquals(result, new String[] {"b", "c", "d", "e", "f", "g", "h", "i", "j"});
		
		/* test with a larger array, and shift left by 2 */
		result = StringArray.shiftLeft(largeArray, 2);
		assertArrayEquals(result, new String[] {"c", "d", "e", "f", "g", "h", "i", "j"});
		
		/* test with a larger array, and shift left by almost the full array length */
		result = StringArray.shiftLeft(largeArray, 9);
		assertArrayEquals(result, new String[] {"j"});

		/* test with a larger array, and shift left by the full array length */
		result = StringArray.shiftLeft(largeArray, 10);
		assertEquals(0, result.length);

		/* test with a larger array, and shift left by more than the array length */
		result = StringArray.shiftLeft(largeArray, 11);
		assertEquals(0, result.length);
		
		/* test with a negative count */
		result = StringArray.shiftLeft(largeArray, -10);
		assertArrayEquals(result, largeArray);
	}

}
