/*
 * Copyright (c) 2013, Daniel Nilsson
 * Released under a simplified BSD license,
 * see README.txt for details.
 */
package com.github.danieln.multipart;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


/**
 * Parse MIME multipart content according to RFC2046.
 * <p>
 * This class can be used to parse e-mail messages with attachments,
 * MJPEG streams over HTTP or any other use of MIME multipart.
 * Multipart messages have a content type with major type "multipart".
 * This class doesn't care about the subtype since all follow the same
 * basic syntax. It is up to the application to handle the semantics of
 * the subtypes.
 * <p>
 * Examples of multipart content types are:
 * <ul>
 * <li>multipart/mixed
 * <li>multipart/related
 * <li>multipart/x-mixed-replace
 * </ul>
 * The content type must also contain the parameter "boundary" which
 * specifies the string that, prepended with "--", separates the parts.
 * A complete content type can look like this:
 * "multipart/mixed;boundary=QWERTY12345"
 * @author Daniel Nilsson
 */
public class MultipartInput {

	/**
	 * Key for the "boundary" parameter.
	 */
	public static final String KEY_BOUNDARY = "boundary";

	private static final int PREEMBLE_LIMIT = 2000;
	
	private final InputStream stream;
	private final String subtype;
	private final Map<String, String> parameters;
	private final String boundary;

	private PartInputStream partStream;

	/**
	 * Create a new MultipartMessage that parses the given stream.
	 * @param stream the multipart stream.
	 * @param contentType the content type, must have major type "multipart" and a "boundary" parameter.
	 * @throws IOException if a read error occurs.
	 * @throws IllegalArgumentException if the content type is bad.
	 */
	public MultipartInput(InputStream stream, String contentType) throws IOException {
		this.stream = new BufferedInputStream(stream);
		this.subtype = parseContentType(contentType);
		this.parameters = parseParams(contentType);
		String b = getParameter(KEY_BOUNDARY);
		if (b == null || b.length() == 0) {
			throw new IllegalArgumentException("No or empty boundary specified in the ContentType");
		}
		if (b.startsWith("--")) {	// Fix for servers that wrongly include the dashes in the boundary.
			b = b.substring(2);
		}
		this.boundary = b;
		this.partStream = new PartInputStream(this.stream, boundary);
		partStream.skipToNextPart(PREEMBLE_LIMIT);
		// If a boundary was found read should return -1 (EOF)
		if (partStream.read() >= 0) {
			throw new IOException("Can't find first part boundary");
		}
	}

	/**
	 * Get the multipart subtype, eg. "mixed" or "x-mixed-replace".
	 * @return the subtype.
	 */
	public String getSubtype() {
		return subtype;
	}
	
	/**
	 * Get the value of a content type parameter.
	 * @param key the parameter name, eg. "boundary".
	 * @return the parameter value.
	 */
	public String getParameter(String key) {
		return parameters.get(key.toLowerCase());
	}
	
	/**
	 * Retrieve the next part in the stream.
	 * Once a new part is retrieved the old one's input stream will be placed at end of file.
	 * This means that the application must read all data it needs from one part before
	 * getting the next one.
	 * @return the next part, or null if there are no more parts.
	 * @throws IOException if a read error occurs.
	 */
	public PartInput nextPart() throws IOException {
		partStream.skipToNextPart();
		if (partStream.isLastPart()) {
			return null;
		}
		partStream = new PartInputStream(stream, boundary);
		return new PartInput(partStream);
	}
	
	/**
	 * Parse a content type, check that it is multipart and return the subtype.
	 * @param contentType the content type value.
	 * @return the content subtype.
	 * @throws IllegalArgumentException if the major type isn't "multipart".
	 */
	static String parseContentType(String contentType) {
		int i = contentType.indexOf(';');
		String type;
		if (i >= 0) {
			type = contentType.substring(0, i);
		} else {
			type = contentType;
		}
		type = type.trim().toLowerCase();
		if (!type.startsWith("multipart/")) {
			throw new IllegalArgumentException("Not a multipart MIME type: " + contentType);
		}
		return type.substring(10).trim();
	}

	/**
	 * Parse a content type and return all parameters.
	 * @param contentType the content type value.
	 * @return a map of all parameters.
	 * @throws IllegalArgumentException if there is a syntax error in the parameters.
	 */
	static Map<String, String> parseParams(String contentType) {
		Map<String, String> parameters = new HashMap<String, String>();
		int i = contentType.indexOf(';');
		if (i < 0) {
			return parameters;
		}
		String params = contentType.substring(i + 1);
		String key = null;
		String value = null;
		boolean inKey = true;
		boolean inString = false;
		int start = 0;
		for (i = 0; i < params.length(); i++) {
			switch (params.charAt(i)) {
				case '=':
					if (inKey) {
						key = params.substring(start, i).trim().toLowerCase();
						start = i + 1;
						inKey = false;
					} else if (!inString) {
						throw new IllegalArgumentException("ContentType parameter value has illegal character '=' at " + i + ": " + params);
					}
					break;
				case ';':
					if (inKey) {
						if (params.substring(start, i).trim().length() > 0) {
							throw new IllegalArgumentException("ContentType parameter missing value at " + i + ": " + params);
						} else {
							throw new IllegalArgumentException("ContentType parameter key has illegal character ';' at " + i + ": " + params);
						}
					} else if (!inString) {
						value = params.substring(start, i).trim();
						parameters.put(key, value);
						key = null;
						value = null;
						start = i + 1;
						inKey = true;
					}
					break;
				case '"':
					if (inKey) {
						throw new IllegalArgumentException("ContentType parameter key has illegal character '\"' at " + i + ": " + params);
					} else if (inString) {
						value = params.substring(start, i).trim();
						parameters.put(key, value);
						key = null;
						value = null;
						for (i++; i < params.length() && params.charAt(i) != ';'; i++) {
							if (!Character.isWhitespace(params.charAt(i))) {
								throw new IllegalArgumentException("ContentType parameter value has garbage after quoted string at " + i + ": " + params);
							}
						}
						start = i + 1;
						inString = false;
						inKey = true;
					} else {
						if (params.substring(start, i).trim().length() > 0) {
							throw new IllegalArgumentException("ContentType parameter value has garbage before quoted string at " + i + ": " + params);
						}
						start = i + 1;
						inString = true;
					}
					break;
			}
		}
		if (inKey) {
			if (i > start && params.substring(start, i).trim().length() > 0) {
				throw new IllegalArgumentException("ContentType parameter missing value at " + i + ": " + params);
			}
		} else if (!inString) {
			value = params.substring(start, i).trim();
			parameters.put(key, value);
		} else {
			throw new IllegalArgumentException("ContentType parameters contain an unterminated quoted string: " + params);
		}
		return parameters;
	}
}
