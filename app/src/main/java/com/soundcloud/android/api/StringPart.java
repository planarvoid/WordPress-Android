package com.soundcloud.android.api;

import com.google.common.base.Objects;

public final class StringPart extends FormPart {

    private final String value;

    public StringPart(String partName, String value) {
        super(partName, "text/plain; charset=UTF-8");
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StringPart)) {
            return false;
        }
        StringPart that = ((StringPart) o);
        return Objects.equal(value, that.value)
                && Objects.equal(partName, that.partName)
                && Objects.equal(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value, partName, contentType);
    }
}
