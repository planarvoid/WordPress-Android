package com.soundcloud.android.profile;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.User;
import com.soundcloud.java.optional.Optional;

import android.support.v7.graphics.Palette;

class ProfileImageSource implements UserImageSource {

    private Optional<Palette> paletteOptional;
    private boolean shouldDefaultToPalette;
    private Urn creatorUrn;
    private Optional<String> avatarUrl;
    private Optional<String> visualUrl;

    ProfileImageSource(User user) {
        creatorUrn = user.urn();
        avatarUrl = user.avatarUrl();
        visualUrl = user.visualUrl();
        shouldDefaultToPalette = false;
        paletteOptional = Optional.absent();
    }

    @Override
    public void setPalette(Optional<Palette> paletteOptional) {
        this.paletteOptional = paletteOptional;
    }

    @Override
    public void setShouldDefaultToPalette(boolean shouldDefault) {
        this.shouldDefaultToPalette = shouldDefault;
    }

    @Override
    public Optional<Palette> getPalette() {
        return paletteOptional;
    }

    @Override
    public boolean shouldDefaultToPalette() {
        return shouldDefaultToPalette;
    }

    @Override
    public Urn getCreatorUrn() {
        return creatorUrn;
    }

    @Override
    public Optional<String> getAvatarUrl() {
        return avatarUrl;
    }

    @Override
    public Optional<String> getVisualUrl() {
        return visualUrl;
    }
}
