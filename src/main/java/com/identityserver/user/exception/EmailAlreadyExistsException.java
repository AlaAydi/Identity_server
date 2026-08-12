package com.identityserver.user.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Un utilisateur existe déjà avec l'adresse email : " + email);
    }
}
