package fr.retrosphere.gamevault.service;

public class GameValidationException extends RuntimeException {
    public GameValidationException(String message) {
        super(message);
    }
}
