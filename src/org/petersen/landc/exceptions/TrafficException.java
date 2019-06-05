package org.petersen.landc.exceptions;

/**
 * 
 * @author Claus Petersen
 * 
 */
@SuppressWarnings("serial")
public final class TrafficException extends Throwable {

	private String message = null;
	private boolean systemMesssage;

	public TrafficException(String message) {
		this.message = message;
		this.systemMesssage = false;
	}

	public TrafficException(String message, boolean system) {
		this.message = message;
		this.systemMesssage = system;
	}

	public String getMessage() {
		return this.message;
	}

	public boolean getSystemMessage() {
		return this.systemMesssage;
	}
}
