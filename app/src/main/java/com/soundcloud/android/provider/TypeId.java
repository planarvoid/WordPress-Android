package com.soundcloud.android.provider;

import android.util.Pair;

public class TypeId extends Pair<Integer,Long> {
    public TypeId(int first, long second) {
        super(first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeId)) return false;
        if (!super.equals(o)) return false;

        TypeId typeId = (TypeId) o;
        return !(first != null ? !first.equals(typeId.first) : typeId.first != null) && !(second != null ? !second.equals(typeId.second) : typeId.second != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (first != null ? first.hashCode() : 0);
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}
