/*
 * Copyright (c) 2013, Daniel Nilsson
 * Released under a simplified BSD license,
 * see README.txt for details.
 */
package com.github.danieln.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;


class PartInputStream extends InputStream {

	private final InputStream stream;
	private final byte[] boundary;
	
	private boolean atStart;
	private boolean lastPart;
	private boolean endOfPart;

	public PartInputStream(InputStream stream, String boundary) {
		if (!stream.markSupported()) {
			throw new IllegalArgumentException("The stream must support mark/reset");
		}
		this.stream = stream;
		try {
			this.boundary = ("--" + boundary).getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e.getMessage());
		}
		atStart = true;
	}

	@Override
	public int read() throws IOException {
		if (endOfPart) {
			return -1;
		}
		int c = stream.read();
		if (c == '\r' || c == '\n' || atStart) {
			int saved = c;
			stream.mark(boundary.length + 2);
			// The boundary includes the newline at the end of the line before if there is one.
			// We check for CRLF (as it should be) or just CR or LF to be on the safe side.
			if (c == '\r') c = stream.read();
			if (c == '\n') c = stream.read();
			// Read new bytes as long as they match the boundary
			boolean ok = true;
			for (int i = 0; i < boundary.length; i++) {
				if ((byte)c != boundary[i]) {
					ok = false;
					break;
				}
				c = stream.read();
			}
			if (ok) {	// Boundary found
				// Check for last part marker ("--")
				if (c == '-') {
					c = stream.read();
					if (c == '-') {
						c = stream.read();
						lastPart = true;
					}
				}
				// Skip to end of line
				while (c > 0 && c != '\r' && c != '\n') {
					c = stream.read();
				}
				// handle CRLF as well as just CR
				if (c == '\r') {
					stream.mark(1);
					if (stream.read() != '\n') {
						stream.reset();
					}
				}
				// Now the stream is positioned just after the newline following the boundary,
				// ready for passing to a new PartInputStream reading the next part.
				// This PartInputStream wont read any more, but instead signal end of file.
				endOfPart = true;
				return -1;
			}
			// It wasn't a boundary after all, rewind to the previous state...
			stream.reset();
			c = saved;
		}
		atStart = false;
		return c;
	}

	public void skipToNextPart() throws IOException {
		while (!endOfPart) {
			read();
		}
	}

	public void skipToNextPart(int limit) throws IOException {
		int i = 0;
		while (!endOfPart && i < limit) {
			read();
			i++;
		}
	}
	
	public boolean isLastPart() {
		return lastPart;
	}

}
