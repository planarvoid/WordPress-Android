package com.soundcloud.android.api;

public abstract class FormPart {
    protected final String partName;
    protected final String contentType;

    protected FormPart(String partName, String contentType) {
        this.partName = partName;
        this.contentType = contentType;
    }

    public String getPartName() {
        return partName;
    }

    public String getContentType() {
        return contentType;
    }
}
