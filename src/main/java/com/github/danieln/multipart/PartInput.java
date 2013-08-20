/*
 * Copyright (c) 2013, Daniel Nilsson
 * Released under a simplified BSD license,
 * see README.txt for details.
 */
package com.github.danieln.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * One part of a multipart message being read.
 */
public class PartInput {

	private final InputStream stream;
	private final Map<String, String> headers = new HashMap<String, String>();
	
	private boolean headersParsed;

	PartInput(InputStream stream) {
		this.stream = stream;
	}

	/**
	 * Get the input stream for the body of this part.
	 * @return the input stream for this part.
	 * @throws IOException if a read error occurs.
	 */
	public InputStream getInputStream() throws IOException {
		parseHeaders();
		return stream;
	}
	
	/**
	 * Get the value of a header field.
	 * @param name the header name.
	 * @return the value of the header, or null if there is no such header.
	 */
	public String getHeaderField(String name) {
		try {
			parseHeaders();
			return headers.get(name.toLowerCase());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get the value of a header as an integer.
	 * @param name the header name.
	 * @param def the default value.
	 * @return the value if the header, or the default value if there is no such header.
	 */
	public int getHeaderFieldInt(String name, int def) {
		try {
			return Integer.parseInt(getHeaderField(name));
		} catch (RuntimeException e) {
			return def;
		}
	}
	
	/**
	 * Get the content type header of this part.
	 * @return the same as getHeaderField("content-type").
	 */
	public String getContentType() {
		return getHeaderField("content-type");
	}
	
	/**
	 * Get the content length header of this part.
	 * @return the same as getHeaderFieldInt("content-length", -1).
	 */
	public int getContentLength() {
		return getHeaderFieldInt("content-length", -1);
	}
	
	private void parseHeaders() throws IOException {
		if (headersParsed) {
			return;
		}
		headersParsed = true;
		
		StringBuilder sb = new StringBuilder();
		String key = null;
		boolean inKey = true;
		int c = stream.read();
		if (c == '\r' || c == '\n') {
			if (c == '\r') {
				c = stream.read();
				// FIXME If c != '\n' it should be put back into the stream
			}
			// No headers
			return;
		}
		mainloop: while (c >= 0) {
			switch (c) {
				case ':':
					if (inKey) {
						key = sb.toString().toLowerCase();
						sb.setLength(0);
						inKey = false;
					} else {
						sb.append((char)c);
					}
					break;
				case '\t':
				case ' ':
					sb.append(' ');
					break;
				case '\n':
				case '\r':
					// We need to check two at least character to detect end of headers and line folding 
					int pc = c;
					c = stream.read();
					if (pc == '\r' && c == '\n') {
						// Got CRLF (correct newline sequence), need to check more...
						c = stream.read();
						if (c == '\r') {
							// Got CRLF + CR need one more...
							c = stream.read();
						}
					}
					if (c == ' ' || c == '\t') {
						// CRWS or LFWS or CRLFWS or CRLFCRWS
						// line folding
						sb.append(' ');
					} else {
						// header separator
						if (key != null) {
							headers.put(key, sb.toString().trim());
						}
						sb.setLength(0);
						if (c == '\r' || c == '\n') {
							// CRCR or LFLF or LFCR or CRLFLF or CRLFCRLF or CRLFCRCR
							// end of headers
							break mainloop;
						}
						inKey = true;
						sb.append((char)c);
					}
					break;
				default:
					sb.append((char)c);
					break;
			}
			c = stream.read();
		}
	}
}
