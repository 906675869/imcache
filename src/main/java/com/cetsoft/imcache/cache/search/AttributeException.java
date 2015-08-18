package com.cetsoft.imcache.cache.search;

/**
 * The Class AttributeException.
 */
public class AttributeException extends RuntimeException{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 8883617514611224481L;

	/**
	 * Instantiates a new attribute exception.
	 *
	 * @param exception the exception
	 */
	public AttributeException(Exception exception){
		super(exception);
	}
}
