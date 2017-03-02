package com.soundcloud.android.search;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.fromNullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class SearchModelCollection<T> extends ModelCollection<T> {

    private final Optional<SearchModelCollection<T>> premiumContent;
    private final int tracksCount;
    private final int playlistsCount;
    private final int usersCount;

    SearchModelCollection(List<T> collection) {
        super(collection);
        this.premiumContent = absent();
        this.tracksCount = 0;
        this.playlistsCount = 0;
        this.usersCount = 0;
    }

    SearchModelCollection(List<T> collection, Map<String, Link> links) {
        super(collection, links);
        this.premiumContent = absent();
        this.tracksCount = 0;
        this.playlistsCount = 0;
        this.usersCount = 0;
    }

    SearchModelCollection(@JsonProperty("collection") List<T> collection,
                          @JsonProperty("_links") Map<String, Link> links,
                          @JsonProperty("query_urn") String queryUrn,
                          @JsonProperty("premium_content") @Nullable SearchModelCollection<T> premiumContent,
                          @JsonProperty("tracks_count") int tracksCount,
                          @JsonProperty("playlists_count") int playlistsCount,
                          @JsonProperty("users_count") int usersCount) {
        super(collection, links, queryUrn);
        this.premiumContent = fromNullable(premiumContent);
        this.tracksCount = tracksCount;
        this.playlistsCount = playlistsCount;
        this.usersCount = usersCount;
    }

    SearchModelCollection(List<T> collection,
                          Map<String, Link> links,
                          Urn queryUrn,
                          Optional<SearchModelCollection<T>> premiumContent,
                          int tracksCount,
                          int playlistsCount,
                          int usersCount) {
        super(collection, links, queryUrn);
        this.premiumContent = premiumContent;
        this.tracksCount = tracksCount;
        this.playlistsCount = playlistsCount;
        this.usersCount = usersCount;
    }

    Optional<SearchModelCollection<T>> premiumContent() {
        return premiumContent;
    }

    int resultsCount() {
        return tracksCount + playlistsCount + usersCount;
    }

    @Override
    public <S> SearchModelCollection<S> transform(Function<T, S> function) {
        return new SearchModelCollection<S>(newArrayList(Iterables.transform(collection, function)),
                                            links,
                                            queryUrn,
                                            premiumContent.transform(model -> model.transform(function)),
                                            tracksCount,
                                            playlistsCount,
                                            usersCount);
    }
}
