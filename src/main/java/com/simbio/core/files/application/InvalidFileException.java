package com.simbio.core.files.application;

public class InvalidFileException extends RuntimeException {

    public InvalidFileException() {
        super("File does not satisfy the upload policy");
    }
}
