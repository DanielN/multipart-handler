/*
 * Copyright (c) 2013, Daniel Nilsson
 * Released under a simplified BSD license,
 * see README.txt for details.
 */
package com.github.danieln.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import junit.framework.TestCase;


public class MultipartInputTest extends TestCase {

	public void testParseContentType() {
		String subtype;
		subtype = MultipartInput.parseContentType("multipart/mixed");
		assertEquals("subtype", "mixed", subtype);
		subtype = MultipartInput.parseContentType("multipart/mixed;");
		assertEquals("subtype", "mixed", subtype);
		subtype = MultipartInput.parseContentType("multipart/x-foo-bar ; foo=\"bar/baz\";test=");
		assertEquals("subtype", "x-foo-bar", subtype);
		// TODO test failures too
	}
	
	public void testParseParams() {
		Map<String, String> ps;
		ps = MultipartInput.parseParams("multipart/mixed");
		assertEquals("parameter count", 0, ps.size());
		ps = MultipartInput.parseParams("multipart/mixed; ");
		assertEquals("parameter count", 0, ps.size());
		ps = MultipartInput.parseParams("multipart/mixed;boundary=qwerty");
		assertEquals("boundary value", "qwerty", ps.get("boundary"));
		ps = MultipartInput.parseParams("multipart/mixed; boundary=\"qwerty\"");
		assertEquals("boundary value", "qwerty", ps.get("boundary"));
		ps = MultipartInput.parseParams("multipart/mixed;test=\"quoted=can contain;\";boundary=qwerty");
		assertEquals("test value", "quoted=can contain;", ps.get("test"));
		assertEquals("boundary value", "qwerty", ps.get("boundary"));
		ps = MultipartInput.parseParams("multipart/mixed\t; boundary = 42 ;foo=;bar=   123;baz= \"\" ;");
		assertEquals("boundary value", "42", ps.get("boundary"));
		assertEquals("foo value", "", ps.get("foo"));
		assertEquals("bar value", "123", ps.get("bar"));
		assertEquals("baz value", "", ps.get("baz"));
		// TODO test failures too
	}
	
	public void testMultipartMessage() throws IOException {
		String message = "--qwerty\r\n"
			+ "Content-Type: text/plain\r\n"
			+ "Content-Length: 14\r\n"
			+ "\r\n"
			+ "This is a test\r\n"
			+ "--qwerty\r\n"
			+ "Content-Type: text/plain\r\n"
			+ "Content-Length: 0\r\n"
			+ "\r\n"
			+ "--qwerty--";
		InputStream stream = new ByteArrayInputStream(message.getBytes("US-ASCII"));
		MultipartInput mpm = new MultipartInput(stream, "multipart/mixed;boundary=qwerty");
		assertEquals("subtype", "mixed", mpm.getSubtype());
		assertEquals("boundary value", "qwerty", mpm.getParameter("boundary"));
		PartInput part = mpm.nextPart();
		assertNotNull("First part", part);
		assertEquals("Part 1 type", "text/plain", part.getContentType());
		assertEquals("Part 1 length", 14, part.getContentLength());
		InputStream in = part.getInputStream();
		assertNotNull("Part 1 stream", in);
		byte[] buf = new byte[14];
		int n = in.read(buf);
		assertEquals("Stream 1 length", 14, n);
		assertTrue("Stream 1 content", "This is a test".equals(new String(buf, "US-ASCII")));
		n = in.read();
		assertEquals("Stream 1 end", -1, n);

		part = mpm.nextPart();
		assertNotNull("Second part", part);
		assertEquals("Part 2 type", "text/plain", part.getContentType());
		assertEquals("Part 2 length", 0, part.getContentLength());
		in = part.getInputStream();
		assertNotNull("Part 2 stream", in);
		n = in.read();
		assertEquals("Stream 2 end", -1, n);

		part = mpm.nextPart();
		assertNull("Next part", part);
	}

	public void testMultipartMessage2() throws IOException {
		String message = "--qwerty\r\n"
			+ "\r\n"
			+ "No header\r\n"
			+ "\r\n"
			+ "--qwerty\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "--qwerty--";
		InputStream stream = new ByteArrayInputStream(message.getBytes("US-ASCII"));
		MultipartInput mpm = new MultipartInput(stream, "multipart/mixed;boundary=qwerty");
		PartInput part = mpm.nextPart();
		assertNotNull("First part", part);
		assertNull("Part 1 type", part.getContentType());
		assertEquals("Part 1 length", -1, part.getContentLength());
		InputStream in = part.getInputStream();
		assertNotNull("Part 1 stream", in);
		byte[] buf = new byte[11];
		int n = in.read(buf);
		assertEquals("Stream 1 length", 11, n);
		assertTrue("Stream 1 content", "No header\r\n".equals(new String(buf, "US-ASCII")));
		n = in.read();
		assertEquals("Stream 1 end", -1, n);
		
		part = mpm.nextPart();
		assertNotNull("Second part", part);
		assertNull("Part 2 type", part.getContentType());
		assertEquals("Part 2 length", -1, part.getContentLength());
		in = part.getInputStream();
		assertNotNull("Part 2 stream", in);
		n = in.read();
		assertEquals("Stream 2 end", -1, n);
		
		part = mpm.nextPart();
		assertNull("Next part", part);
	}

	public void testMultipartMessageEOF() throws IOException {
		String message = "--qwerty\r\n"
			+ "Content-Type: text/plain\r\n"
			+ "Content-Length: 14\r\n"
			+ "\r\n"
			+ "This is a test";	// EOF without boundary
		InputStream stream = new ByteArrayInputStream(message.getBytes("US-ASCII"));
		MultipartInput mpm = new MultipartInput(stream, "multipart/mixed;boundary=qwerty");
		PartInput part = mpm.nextPart();
		assertNotNull("First part", part);
		InputStream in = part.getInputStream();
		assertNotNull("Part 1 stream", in);
		byte[] buf = new byte[14];
		int n = in.read(buf);
		assertEquals("Stream 1 length", 14, n);
		assertTrue("Stream 1 content", "This is a test".equals(new String(buf, "US-ASCII")));
		n = in.read();
		assertEquals("Stream 1 end", -1, n);

		part = mpm.nextPart();
		assertNull("Next part", part);
	}

}
