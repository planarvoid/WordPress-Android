package com.soundcloud.android.api;

import com.google.common.base.Objects;

import java.io.File;

public final class FilePart extends FormPart {
    private final File file;
    private final String fileName;

    public static FilePart from(File file, String partName, String contentType) {
        return new FilePart(file, partName, contentType);
    }

    public static FilePart from(File file, String fileName, String partName, String contentType) {
        return new FilePart(file, fileName, partName, contentType);
    }

    FilePart(File file, String partName, String contentType) {
        this(file, file.getName(), partName, contentType);
    }

    FilePart(File file, String fileName, String partName, String contentType) {
        super(partName, contentType);
        this.file = file;
        this.fileName = fileName;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FilePart)) {
            return false;
        }
        FilePart that = ((FilePart) o);
        return Objects.equal(file, that.file)
                && Objects.equal(partName, that.partName)
                && Objects.equal(fileName, that.fileName)
                && Objects.equal(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(file, partName, fileName, contentType);
    }
}
