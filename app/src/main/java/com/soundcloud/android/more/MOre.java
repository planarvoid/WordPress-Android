package com.soundcloud.android.more;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;

class More implements ImageResource {

    private final UserItem source;

    More(UserItem source) {
        this.source = source;
    }

    String getUsername() {
        return source.getName();
    }

    @Override
    public Urn getUrn() {
        return source.getUrn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.getImageUrlTemplate();
    }

}
