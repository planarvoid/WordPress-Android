package com.soundcloud.android.api;

import com.soundcloud.java.objects.MoreObjects;

public final class StringPart extends FormPart {

    private final String value;

    public static StringPart from(String partName, String value) {
        return new StringPart(partName, value);
    }

    StringPart(String partName, String value) {
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
        return MoreObjects.equal(value, that.value)
                && MoreObjects.equal(partName, that.partName)
                && MoreObjects.equal(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(value, partName, contentType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("partName", partName)
                .add("value", value)
                .toString();
    }
}
