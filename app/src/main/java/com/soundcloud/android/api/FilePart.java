package com.soundcloud.android.api;

import com.google.common.base.Objects;

import java.io.File;

public final class FilePart extends FormPart {
    public static final String BLOB_MEDIA_TYPE = "application/octet-stream";

    private final File file;
    private final String fileName;

    public static FilePart from(String partName, File file, String contentType) {
        return new FilePart(partName, file, contentType);
    }

    public static FilePart from(String partName, File file, String fileName, String contentType) {
        return new FilePart(partName, file, fileName, contentType);
    }

    FilePart(String partName, File file, String contentType) {
        this(partName, file, file.getName(), contentType);
    }

    FilePart(String partName, File file, String fileName, String contentType) {
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

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("partName", partName)
                .add("file", file)
                .add("fileName", fileName)
                .toString();
    }
}
