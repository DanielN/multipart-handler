/*
 * Copyright (c) 2013, Daniel Nilsson
 * Released under a simplified BSD license,
 * see README.txt for details.
 */
package com.github.danieln.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import junit.framework.TestCase;


public class MultipartOutputTest extends TestCase {

	public void testGetContentType() {
		MultipartOutput mo = new MultipartOutput(new ByteArrayOutputStream(), "x-foo-bar", "my_boundary");
		String subtype = MultipartInput.parseContentType(mo.getContentType());
		assertEquals("ContentType", "x-foo-bar", subtype);
		Map<String, String> params = MultipartInput.parseParams(mo.getContentType());
		assertEquals("Boundary", "my_boundary", params.get("boundary"));
	}

	public void testNewPart() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		MultipartOutput mo = new MultipartOutput(out);
		PartOutput po = mo.newPart();
		po.setContentType("text/plain");
		OutputStream partOut = po.getOutputStream();
		partOut.write("foo".getBytes("US-ASCII"));
		po = mo.lastPart();
		partOut = po.getOutputStream();
		partOut.write("bar".getBytes("US-ASCII"));
		mo.close();
		
		byte[] arr = new byte[3];
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		MultipartInput mm = new MultipartInput(in, mo.getContentType());
		
		PartInput part = mm.nextPart();
		assertNotNull("First part", part);
		assertEquals("Part 1 ContentType", "text/plain", part.getContentType());
		InputStream partIn = part.getInputStream();
		assertEquals("Part 1 bytes", 3, partIn.read(arr));
		assertEquals("Part 1 data", "foo", new String(arr, "US-ASCII"));
		assertTrue("Part 1 at end", partIn.read() == -1);
		
		part = mm.nextPart();
		assertNotNull("Second part", part);
		assertNull("Part 2 ContentType", part.getContentType());
		partIn = part.getInputStream();
		assertEquals("Part 2 bytes", 3, partIn.read(arr));
		assertEquals("Part 2 data", "bar", new String(arr, "US-ASCII"));
		assertTrue("Part 2 at end", partIn.read() == -1);

		part = mm.nextPart();
		assertNull("Third part", part);
	}

}
