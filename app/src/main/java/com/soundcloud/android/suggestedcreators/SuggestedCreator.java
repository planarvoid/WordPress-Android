package com.soundcloud.android.suggestedcreators;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

@AutoValue
public abstract class SuggestedCreator {
    public static SuggestedCreator create(UserItem userItem, SuggestedCreatorRelation relation, Optional<Date> followedAt) {
        return new AutoValue_SuggestedCreator(userItem, relation, followedAt);
    }

    public abstract UserItem getCreator();

    public abstract SuggestedCreatorRelation getRelation();

    public abstract Optional<Date> followedAt();
}
