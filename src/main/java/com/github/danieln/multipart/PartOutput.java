package com.github.danieln.multipart;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class PartOutput {

	/**
	 * Header name/value pair.
	 * The name field preserves the original case,
	 * the keys in the headers Map uses lower case.
	 */
	private static class Header {
		public String name;
		public String value;
		public Header(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}

	private PartOutputStream stream;
	private final Map<String, Header> headers = new HashMap<String, Header>();
	
	private boolean headersWritten;

	public PartOutput(PartOutputStream stream) {
		this.stream = stream;
	}

	/**
	 * Get the output stream for writing the body of this part.
	 * Retrieving the output stream will commit all headers.
	 * @return the output stream for this part.
	 * @throws IOException if a write error occurs.
	 */
	public OutputStream getOutputStream() throws IOException {
		writeHeaders();
		return stream;
	}

	/**
	 * Set the value of a header field.
	 * @param name the header name (case is preserved but not significant).
	 * @param value the value of the header.
	 */
	public void setHeaderField(String name, String value) {
		if (headersWritten) {
			throw new IllegalStateException("Headers have already been comitted");
		}
		headers.put(name.toLowerCase(), new Header(name, value));
	}
	
	/**
	 * Set the content type header of this part.
	 * The same as setHeaderField("Content-Type", contentType)
	 * @param contentType the content type value.
	 */
	public void setContentType(String contentType) {
		setHeaderField("Content-Type", contentType);
	}
	
	/**
	 * Set the content length header of this part.
	 * The same as setHeaderField("Content-Length", Integer.toString(contentLength)).
	 * @param contentLength the content length value.
	 */
	public void setContentLength(int contentLength) {
		setHeaderField("Content-Length", Integer.toString(contentLength));
	}
	
	private void writeHeaders() throws IOException {
		if (headersWritten) {
			return;
		}
		headersWritten = true;
		
		for (Header h : headers.values()) {
			stream.write((h.name + ": " + h.value + "\r\n").getBytes("US-ASCII"));
		}
		stream.write("\r\n".getBytes("US-ASCII"));
	}

}
