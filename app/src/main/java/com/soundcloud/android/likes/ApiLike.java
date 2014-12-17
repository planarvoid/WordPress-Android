package com.soundcloud.android.likes;

import com.soundcloud.android.model.Urn;

import java.util.Date;

class ApiLike {

    private final Urn urn;
    private final Date createdAt;

    ApiLike(Urn urn, Date createdAt) {
        this.urn = urn;
        this.createdAt = createdAt;
    }

    public Urn getUrn() {
        return urn;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
