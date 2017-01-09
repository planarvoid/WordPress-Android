package com.soundcloud.android.more;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.User;
import com.soundcloud.java.optional.Optional;

class More implements ImageResource {

    private final User source;

    More(User source) {
        this.source = source;
    }

    String getUsername() {
        return source.username();
    }

    @Override
    public Urn getUrn() {
        return source.urn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.avatarUrl();
    }

}
