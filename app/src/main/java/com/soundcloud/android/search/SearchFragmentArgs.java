package com.soundcloud.android.search;

import auto.parcel.AutoParcel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.os.Parcelable;
import android.support.annotation.Nullable;

@AutoParcel
public abstract class SearchFragmentArgs implements Parcelable {
    public abstract SearchType searchType();

    public abstract String apiQuery();

    public abstract String userQuery();

    @Nullable
    abstract Urn nullableQueryUrn();

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
        return new AutoParcel_SearchFragmentArgs(type, apiQuery, userQuery, queryUrn.orNull(), queryPosition, publishSearchSubmissionEvent, isPremium);
    }
}
