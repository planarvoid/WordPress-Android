package com.soundcloud.android.suggestedcreators;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.UserImageSource;
import com.soundcloud.android.users.User;
import com.soundcloud.java.optional.Optional;

import android.support.v7.graphics.Palette;

@AutoValue
public abstract class SuggestedCreatorItem implements UserImageSource {
    public abstract User creator();
    public abstract SuggestedCreatorRelation relation();

    //UI state
    boolean following;

    private Optional<Palette> palette;
    private boolean shouldDefaultToPalette = false;

    public static SuggestedCreatorItem fromSuggestedCreator(SuggestedCreator suggestedCreator) {
        final AutoValue_SuggestedCreatorItem autoValue_suggestedCreatorItem = new AutoValue_SuggestedCreatorItem(
                suggestedCreator.getCreator(),
                suggestedCreator.getRelation());
        autoValue_suggestedCreatorItem.following = suggestedCreator.getCreator().isFollowing();
        autoValue_suggestedCreatorItem.setPalette(Optional.<Palette>absent());
        return autoValue_suggestedCreatorItem;
    }

    @Override
    public void setPalette(Optional<Palette> paletteOptional) {
        this.palette = paletteOptional;
    }

    @Override
    public void setShouldDefaultToPalette(boolean shouldDefault) {
        this.shouldDefaultToPalette = shouldDefault;
    }

    @Override
    public Optional<Palette> getPalette() {
        return palette;
    }

    @Override
    public boolean shouldDefaultToPalette() {
        return shouldDefaultToPalette;
    }

    @Override
    public Urn getCreatorUrn() {
        return creator().urn();
    }

    @Override
    public Optional<String> getAvatarUrl() {
        return creator().avatarUrl();
    }

    @Override
    public Optional<String> getVisualUrl() {
        return creator().visualUrl();
    }
}
