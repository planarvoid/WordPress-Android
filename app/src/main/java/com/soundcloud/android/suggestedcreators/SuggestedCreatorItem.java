package com.soundcloud.android.suggestedcreators;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.users.User;

@AutoValue
public abstract class SuggestedCreatorItem {
    public abstract User user();
    public abstract SuggestedCreatorRelation relation();

    //UI state
    boolean following;

    public static SuggestedCreatorItem fromSuggestedCreator(SuggestedCreator suggestedCreator) {
        final AutoValue_SuggestedCreatorItem autoValue_suggestedCreatorItem = new AutoValue_SuggestedCreatorItem(
                suggestedCreator.getCreator(),
                suggestedCreator.getRelation());
        autoValue_suggestedCreatorItem.following = suggestedCreator.getCreator().isFollowing();
        return autoValue_suggestedCreatorItem;
    }
}
