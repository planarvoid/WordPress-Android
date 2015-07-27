package com.soundcloud.android.payments.googleplay;

import com.soundcloud.java.objects.MoreObjects;

public final class Payload {

    public final String data;
    public final String signature;

    public Payload(String data, String signature) {
        this.data = data;
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Payload that = (Payload) o;
        return MoreObjects.equal(data, that.data) && MoreObjects.equal(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(data, signature);
    }

}
