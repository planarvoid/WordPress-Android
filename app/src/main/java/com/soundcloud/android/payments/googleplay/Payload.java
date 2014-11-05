package com.soundcloud.android.payments.googleplay;

import com.google.common.base.Objects;

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
        return Objects.equal(data, that.data) && Objects.equal(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data, signature);
    }

}
