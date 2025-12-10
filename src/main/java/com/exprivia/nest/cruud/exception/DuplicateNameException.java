package com.exprivia.nest.cruud.exception;

/**
 * Duplicate Name Exception class
 */
public class DuplicateNameException extends Exception {

    /**
     * Constructor for Exception
     * @param message exception
     */
    public DuplicateNameException(String message) {
        super(message);
    }
}
