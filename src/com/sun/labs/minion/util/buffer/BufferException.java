package com.sun.labs.minion.util.buffer;


import java.util.logging.Logger;

/**
 * A runtime exception that can be thrown by buffers and caught by people who
 * care about catching such things.
 */
public class BufferException extends RuntimeException {

    private static final Logger logger = Logger.getLogger(BufferException.class.getName());

    public BufferException() {
    }

    public BufferException(String message) {
        super(message);
    }

    public BufferException(String message, Throwable cause) {
        super(message, cause);
    }

    public BufferException(Throwable cause) {
        super(cause);
    }

    public BufferException(String arg0, Throwable arg1, boolean arg2,
                           boolean arg3) {
        super(arg0, arg1, arg2, arg3);
    }
    
    
}
