package com.example.event_hub.Model;

public abstract class ResultWrapper<T> {
    private ResultWrapper() {} // Private constructor for sealed class simulation in Java

    public static final class Success<T> extends ResultWrapper<T> {
        private final T data;
        public Success(T data) { this.data = data; }
        public T getData() { return data; }
    }

    public static final class Error<T> extends ResultWrapper<T> {
        private final String message;
        // private final Throwable throwable; // Optional: if you want to pass the exception too

        public Error(String message) {
            this.message = message;
            // this.throwable = null;
        }
        // public Error(String message, Throwable throwable) {
        //     this.message = message;
        //     this.throwable = throwable;
        // }
        public String getMessage() { return message; }
        // public Throwable getThrowable() { return throwable; }
    }

    public static final class Loading<T> extends ResultWrapper<T> {
        public Loading() {}
    }

    // Optional: For an initial or idle state if needed
    public static final class Idle<T> extends ResultWrapper<T> {
        public Idle() {}
    }
}
