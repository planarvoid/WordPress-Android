package com.soundcloud.android.suggestedcreators;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.users.User;

@AutoValue
public abstract class SuggestedCreator {
    public static SuggestedCreator create(User user, SuggestedCreatorRelation relation) {
        return new AutoValue_SuggestedCreator(user, relation);
    }

    public abstract User getCreator();

    public abstract SuggestedCreatorRelation getRelation();
}
