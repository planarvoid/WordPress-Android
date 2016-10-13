package com.soundcloud.android.suggestedcreators;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.users.User;
import com.soundcloud.java.optional.Optional;

import android.support.v7.graphics.Palette;

@AutoValue
public abstract class SuggestedCreatorItem {
    public abstract User creator();
    public abstract SuggestedCreatorRelation relation();

    //UI state
    boolean following;
    Optional<Palette> palette;
    boolean shouldDefaultToPalette = false;

    public static SuggestedCreatorItem fromSuggestedCreator(SuggestedCreator suggestedCreator) {
        final AutoValue_SuggestedCreatorItem autoValue_suggestedCreatorItem = new AutoValue_SuggestedCreatorItem(
                suggestedCreator.getCreator(),
                suggestedCreator.getRelation());
        autoValue_suggestedCreatorItem.following = suggestedCreator.getCreator().isFollowing();
        autoValue_suggestedCreatorItem.palette = Optional.absent();
        return autoValue_suggestedCreatorItem;
    }
}
