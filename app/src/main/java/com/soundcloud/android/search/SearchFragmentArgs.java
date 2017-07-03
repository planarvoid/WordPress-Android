package com.soundcloud.android.search;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.os.Parcelable;
import android.support.annotation.Nullable;

@AutoValue
public abstract class SearchFragmentArgs implements Parcelable {
    public abstract SearchType searchType();

    public abstract String apiQuery();

    public abstract String userQuery();

    abstract Optional<String> stringQueryUrn();

    @Nullable
    Urn nullableQueryUrn() {
        return stringQueryUrn().transform(Urn::new).orNull();
    }

    public abstract Optional<Integer> queryPosition();

    public abstract boolean publishSearchSubmissionEvent();

    public abstract boolean isPremium();

    public Optional<Urn> queryUrn() {
        return Optional.fromNullable(nullableQueryUrn());
    }

    public static SearchFragmentArgs create(SearchType type,
                                            String apiQuery,
                                            String userQuery,
                                            Optional<Urn> queryUrn,
                                            Optional<Integer> queryPosition,
                                            boolean publishSearchSubmissionEvent,
                                            boolean isPremium) {
        return new AutoValue_SearchFragmentArgs(type, apiQuery, userQuery, queryUrn.transform(Urn::toString), queryPosition, publishSearchSubmissionEvent, isPremium);
    }
}
