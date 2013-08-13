/*
 * Copyright (c) 2013, Daniel Nilsson
 * Released under a simplified BSD license,
 * see README.txt for details.
 */
package com.github.danieln.multipart;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;


/**
 * Write MIME multipart content according to RFC2046.
 * <p>
 * This class can be used to write e-mail messages with attachments,
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
 * @author Daniel Nilsson
 */
public class MultipartOutput {

	private static final Random RNG = new Random();
	
	private final OutputStream stream;
	private final String subtype;
	private final String boundary;
	
	private PartOutputStream partStream;

	/**
	 * Create a new MultipartOutput that write to the given stream.
	 * @param stream were to write the multipart stream.
	 * @param subtype the multipart subtype, eg. "mixed" or "x-mixed-replace".
	 * @param boundary the string to use as the boundary (should never occur in the part data).
	 */
	public MultipartOutput(OutputStream stream, String subtype, String boundary) {
		this.stream = stream;
		this.subtype = subtype;
		this.boundary = boundary;
		partStream = new PartOutputStream(stream, boundary);
	}
	
	/**
	 * Create a new MultipartOutput that write to the given stream.
	 * A random boundary string will be generated.
	 * @param stream were to write the multipart stream.
	 * @param subtype the multipart subtype, eg. "mixed" or "x-mixed-replace".
	 */
	public MultipartOutput(OutputStream stream, String subtype) {
		this(stream, subtype, makeRandomBoundary());
	}

	/**
	 * Create a new MultipartOutput that write to the given stream.
	 * Uses the default subtype of "mixed".
	 * A random boundary string will be generated.
	 * @param stream were to write the multipart stream.
	 */
	public MultipartOutput(OutputStream stream) {
		this(stream, "mixed");
	}
	
	/**
	 * Get the content type of the multipart message.
	 * This is what should be put into the e-mail or HTTP Content-Type header.
	 * @return the content type, including the boundary parameter.
	 */
	public String getContentType() {
		return "multipart/" + subtype + ";boundary=\"" + boundary + "\"";
	}

	/**
	 * Create a new, not final, part of the multipart message.
	 * If the {@link OutputStream#close()} method is called on the output stream of the returned
	 * part the boundary marker is output and nothing more can be written to that part.
	 * The next call to start a new part will automatically close the old part first.
	 * @return the new part.
	 * @throws IOException if an I/O error occurs, or if the MultiparOutput has been closed.
	 */
	public PartOutput newPart() throws IOException {
		return newPart(false);
	}
	
	/**
	 * Create a new, final, part of the multipart message.
	 * If the {@link OutputStream#close()} method is called on the output stream of the returned
	 * part the boundary marker is output and nothing more can be written to that part.
	 * The next call to start a new part will automatically close the old part first.
	 * @return the new part.
	 * @throws IOException if an I/O error occurs, or if the MultiparOutput has been closed.
	 */
	public PartOutput lastPart() throws IOException {
		return newPart(true);
	}
	
	/**
	 * Create a new part of the multipart message.
	 * @see #newPart()
	 * @see #lastPart()
	 * @param last true if this will be the final part of the multipart message.
	 * @return the new part.
	 * @throws IOException if an I/O error occurs, or if the MultiparOutput has been closed.
	 */
	public PartOutput newPart(boolean last) throws IOException {
		if (partStream == null) {
			throw new IOException("MultipartOutput is closed");
		}
		if (partStream.isLastPart()) {
			throw new IOException("Can't start another part after the final one");
		}
		partStream.close();
		partStream = new PartOutputStream(stream, boundary);
		partStream.setLastPart(last);
		return new PartOutput(partStream);
	}

	/**
	 * End the multipart message and close the underlying OutputStream.
	 * If the current part hasn't been explicitly closed yet then the
	 * terminating boundary marker is output.
	 * If the MultiparOutput is closed when the last part was non-final
	 * and explicitly closed, then the produced multipart message will
	 * lack a proper termination boundary. This may cause to receiver
	 * of the message to fail to detect the end of the message. 
	 * @throws IOException if an I/O error occurs.
	 */
	public void close() throws IOException {
		if (partStream != null) {
			partStream.setLastPart(true);
			partStream.close();
			partStream = null;
			stream.close();
		}
	}
	
	private static String makeRandomBoundary() {
		return String.format("%016X", RNG.nextLong());
	}
	
}
