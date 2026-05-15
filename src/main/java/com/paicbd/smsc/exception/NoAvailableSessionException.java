package com.paicbd.smsc.exception;

import com.paicbd.smsc.utils.Generated;

@Generated
public class NoAvailableSessionException extends RuntimeException {
    public NoAvailableSessionException(String message) {
        super(message);
    }

    public NoAvailableSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
