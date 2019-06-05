package org.petersen.landc.exceptions;

/**
 * 
 * @author Claus Petersen
 * 
 */
@SuppressWarnings("serial")
public final class ConfigException extends Throwable {

	private String message = null;

	public ConfigException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}
}
