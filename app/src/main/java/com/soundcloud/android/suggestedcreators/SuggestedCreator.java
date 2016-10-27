package com.soundcloud.android.suggestedcreators;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.users.User;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

@AutoValue
public abstract class SuggestedCreator {
    public static SuggestedCreator create(User user, SuggestedCreatorRelation relation, Optional<Date> followedAt) {
        return new AutoValue_SuggestedCreator(user, relation, followedAt);
    }

    public abstract User getCreator();

    public abstract SuggestedCreatorRelation getRelation();

    public abstract Optional<Date> followedAt();
}
