package com.boutique.common.exception;

/** Violation d'une règle métier (stock insuffisant, transition de statut invalide, doublon...). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
