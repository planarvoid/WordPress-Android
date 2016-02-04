package com.soundcloud.android.search.suggestions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;
import java.util.Map;

@AutoValue
abstract class ApiSearchSuggestion extends SearchSuggestion {

    @JsonCreator
    public static ApiSearchSuggestion create(
            @JsonProperty("query") String query,
            @JsonProperty("highlights") List<Map<String, Integer>> highlights,
            @JsonProperty("track") ApiTrack track,
            @JsonProperty("user") ApiUser user) {

        return new AutoValue_ApiSearchSuggestion(
                query,
                highlights,
                Optional.fromNullable(track),
                Optional.fromNullable(user),
                true
        );
    }

    public abstract String getQuery();

    public abstract List<Map<String, Integer>> getHighlights();

    public abstract Optional<ApiTrack> getTrack();

    public abstract Optional<ApiUser> getUser();

    public abstract boolean isRemote();


    public Urn getUrn() {
        final Optional<ApiTrack> track = getTrack();
        final Optional<ApiUser> user = getUser();

        if (track.isPresent()) {
            return track.get().getUrn();

        } else if (user.isPresent()) {
            return user.get().getUrn();

        } else {
            throw new IllegalStateException("No user or track present");
        }
    }

    public Optional<? extends RecordHolder> getRecordHolder() {
        final Optional<ApiTrack> track = getTrack();
        final Optional<ApiUser> user = getUser();

        if (track.isPresent()) {
            return track;
        } else if (user.isPresent()) {
            return user;
        } else {
            return Optional.absent();
        }
    }
}
