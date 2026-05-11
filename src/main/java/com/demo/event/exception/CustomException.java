package com.demo.event.exception;

public class CustomException {
    public class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }

    public class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    public class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }

}
