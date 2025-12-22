package com.exprivia.nest.cruud.exception;

/**
 * Eccezione custom per segnalare la creazione di un'entità con nome duplicato.
 */
public class DuplicateNameException extends Exception {

    /** Costruttore con messaggio descrittivo. */
    public DuplicateNameException(String message) {
        super(message);
    }
}
