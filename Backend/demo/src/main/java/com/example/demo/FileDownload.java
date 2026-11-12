package com.example.demo;

public class FileDownload {
    private boolean success;
    private String message;
    private String fileName;
    private long size;
    private String library;

    public FileDownload(boolean success, String message, String fileName, long size, String library) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
        this.size = size;
        this.library = library;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public String getLibrary() {
        return library;
    }
}