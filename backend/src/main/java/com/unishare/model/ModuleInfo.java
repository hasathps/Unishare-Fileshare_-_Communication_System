package com.unishare.model;

/**
 * Simple data holder for module metadata used by landing page and file listing.
 */
public class ModuleInfo {
    private final String code;
    private final String name;
    private final String description;
    private int fileCount;

    public ModuleInfo(String code, String name, String description) {
        this(code, name, description, 0);
    }

    public ModuleInfo(String code, String name, String description, int fileCount) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.fileCount = Math.max(fileCount, 0);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = Math.max(fileCount, 0);
    }
}