package br.com.hanrry.reconpay.exception;

/** A change to a user account that the account's own state forbids. */
public class UserAccountConflictException extends RuntimeException {
    public UserAccountConflictException(String message) {
        super(message);
    }
}
