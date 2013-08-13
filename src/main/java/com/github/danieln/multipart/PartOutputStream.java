/*
 * Copyright (c) 2013, Daniel Nilsson
 * Released under a simplified BSD license,
 * see README.txt for details.
 */
package com.github.danieln.multipart;

import java.io.IOException;
import java.io.OutputStream;


class PartOutputStream extends OutputStream {

	private final OutputStream out;
	private final String boundary;
	
	private boolean closed;
	private boolean lastPart;

	PartOutputStream(OutputStream out, String boundary) {
		this.out = out;
		this.boundary = boundary;
	}
	
	void setLastPart(boolean lastPart) {
		this.lastPart = lastPart;
	}
	
	public boolean isLastPart() {
		return lastPart;
	}
	
	@Override
	public void write(int b) throws IOException {
		checkClosed();
		out.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		checkClosed();
		out.write(b, off, len);
	}

	public void write(byte[] b) throws IOException {
		checkClosed();
		out.write(b);
	}

	@Override
	public void flush() throws IOException {
		checkClosed();
		out.flush();
	}
	
	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			if (lastPart) {
				out.write(("\r\n--" + boundary + "--\r\n").getBytes("US-ASCII"));
			} else {
				out.write(("\r\n--" + boundary + "\r\n").getBytes("US-ASCII"));
			}
			out.flush();
		}
	}

	private void checkClosed() throws IOException {
		if (closed) {
			throw new IOException("Part is closed");
		}
	}

}
