package com.soundcloud.android.api;

import com.soundcloud.java.objects.MoreObjects;

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
        return MoreObjects.equal(file, that.file)
                && MoreObjects.equal(partName, that.partName)
                && MoreObjects.equal(fileName, that.fileName)
                && MoreObjects.equal(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(file, partName, fileName, contentType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("partName", partName)
                .add("file", file)
                .add("fileName", fileName)
                .toString();
    }
}
