package com.simbio.core.files.application;

public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException() {
        super("File not found");
    }
}
