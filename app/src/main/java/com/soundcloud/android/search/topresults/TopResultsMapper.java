package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.ApplicationModule.ENRICHED_ENTITY_ITEM_EMITTER;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.ALBUMS;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.GO_TRACKS;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.PLAYLISTS;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.TOP_RESULT;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.TRACKS;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.USERS;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.EntityItemEmitter;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TopResultsMapper {

    @VisibleForTesting
    static final String URN_BUCKET_TOP = "soundcloud:search-buckets:topresult";
    static final String URN_BUCKET_TRACKS = "soundcloud:search-buckets:freetiertracks";
    static final String URN_BUCKET_GO_TRACKS = "soundcloud:search-buckets:hightiertracks";
    static final String URN_BUCKET_PEOPLE = "soundcloud:search-buckets:users";
    static final String URN_BUCKET_PLAYLISTS = "soundcloud:search-buckets:playlists";
    static final String URN_BUCKET_ALBUMS = "soundcloud:search-buckets:albums";

    private static final Set<String> AVAILABLE_URNS = Sets.newHashSet(URN_BUCKET_TOP, URN_BUCKET_TRACKS, URN_BUCKET_GO_TRACKS, URN_BUCKET_PEOPLE, URN_BUCKET_PLAYLISTS, URN_BUCKET_ALBUMS);

    private final EntityItemEmitter enrichedEntities;

    @Inject
    TopResultsMapper(@Named(ENRICHED_ENTITY_ITEM_EMITTER) EntityItemEmitter enrichedEntities) {
        this.enrichedEntities = enrichedEntities;
    }

    Observable<TopResultsViewModel> toViewModel(ApiTopResults apiTopResults,
                                                       String userQuery) {

        final List<ApiTopResultsBucket> collection = Lists.newArrayList(Iterables.filter(apiTopResults.buckets().getCollection(), bucket -> isAvailableUrn(bucket.urn().toString())));
        final List<Urn> allTrackUrns = allTracksUrns(collection);

        return Observable.combineLatest(Observable.just(collection),
                                 enrichedEntities.trackItemsAsMap(allTracks(collection)),
                                 enrichedEntities.userItemsAsMap(allUsers(collection)),
                                 enrichedEntities.playlistItemsAsMap(allPlaylists(collection)),
                                 (apiTopResultsBuckets, trackItems, userItems, playlistItems) ->
                                                buildViewModelWithEntities(userQuery, collection, allTrackUrns, trackItems, userItems, playlistItems, apiTopResults.buckets().getQueryUrn()));
    }

    @NonNull
    private static TopResultsViewModel buildViewModelWithEntities(String userQuery,
                                                                  List<ApiTopResultsBucket> collection,
                                                                  List<Urn> allTrackUrns,
                                                                  Map<Urn, TrackItem> trackItems,
                                                                  Map<Urn, UserItem> userItems,
                                                                  Map<Urn, PlaylistItem> playlistItems, Optional<Urn> apiQueryUrn) {

        final List<TopResultsBucketViewModel> buckets = new ArrayList<>();
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(apiQueryUrn.or(Urn.NOT_SET), userQuery);

        int currentTrackPosition = 0;
        int currentItemPosition = 0;

        for (ApiTopResultsBucket apiBucket : collection) {
            TopResultsBucketViewModel.Kind kind = urnToKind(apiBucket.urn());

            List<SearchItem> bucketItems = new ArrayList<>();

            for (int i = 0; i < apiBucket.collection().getCollection().size(); i++) {
                ApiUniversalSearchItem apiItem = apiBucket.collection().getCollection().get(i);
                if (apiItem.track().isPresent()) {
                    currentTrackPosition++;
                    ClickParams clickParams = getClickParams(apiItem.track().get().getUrn(), userQuery, apiQueryUrn, currentItemPosition, kind, i);
                    bucketItems.add(getTrack(allTrackUrns, trackItems, currentTrackPosition, searchQuerySourceInfo, apiItem, clickParams));

                } else if (apiItem.playlist().isPresent()) {
                    ClickParams clickParams = getClickParams(apiItem.playlist().get().getUrn(), userQuery, apiQueryUrn, currentItemPosition, kind, i);
                    bucketItems.add(getPlaylist(playlistItems, apiItem, clickParams));

                } else if (apiItem.user().isPresent()) {
                    final ApiUser apiUser = apiItem.user().get();
                    ClickParams clickParams = getClickParams(apiUser.getUrn(), userQuery, apiQueryUrn, currentItemPosition, kind, i);
                    bucketItems.add(getUser(userItems, apiUser, clickParams));

                } else {
                    throw new IllegalArgumentException("ApiSearchItem has to contain either track or playlist or user");
                }
                currentItemPosition++;
            }

            ClickParams clickParams = getClickParams(Urn.NOT_SET, userQuery, apiQueryUrn, currentItemPosition, kind, 0);
            TopResultsBucketViewModel getBucket = TopResultsBucketViewModel.create(bucketItems, kind, apiBucket.totalResults(), userQuery, clickParams);

            buckets.add(getBucket);
        }
        return TopResultsViewModel.create(apiQueryUrn, buckets);
    }

    private static ClickParams getClickParams(Urn itemUrn, String userQuery, Optional<Urn> queryUrn, int currentItemPosition, TopResultsBucketViewModel.Kind kind, int i) {
        return ClickParams.create(itemUrn,
                                  userQuery,
                                  queryUrn,
                                  currentItemPosition,
                                  getContextFromBucket(kind, i),
                                  Screen.SEARCH_EVERYTHING,
                                  kind.toClickSource());
    }

    @NonNull
    private static SearchItem.User getUser(Map<Urn, UserItem> userItems,
                                           ApiUser apiUser,
                                           ClickParams clickParams) {
        UserItem userItem = userItems.get(apiUser.getUrn());
        return SearchItem.User.create(userItem, UiAction.UserClick.create(clickParams));
    }

    @NonNull
    private static SearchItem.Playlist getPlaylist(Map<Urn, PlaylistItem> playlistItems,
                                                   ApiUniversalSearchItem apiItem,
                                                   ClickParams clickParams) {
        final ApiPlaylist apiPlaylist = apiItem.playlist().get();
        PlaylistItem playlistItem = playlistItems.get(apiPlaylist.getUrn());
        return SearchItem.Playlist.create(playlistItem, UiAction.PlaylistClick.create(clickParams));
    }

    @NonNull
    private static SearchItem.Track getTrack(List<Urn> allTrackUrns,
                                             Map<Urn, TrackItem> trackItems,
                                             int currentTrackPosition,
                                             SearchQuerySourceInfo searchQuerySourceInfo,
                                             ApiUniversalSearchItem apiItem,
                                             ClickParams clickParams) {
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.SEARCH_TOP_RESULTS.get(), true);
        trackSourceInfo.setSearchQuerySourceInfo(searchQuerySourceInfo);

        ApiTrack apiTrack = apiItem.track().get();
        TrackItem trackItem = trackItems.get(apiTrack.getUrn());
        UiAction.TrackClick trackClick = UiAction.TrackClick.create(clickParams, trackItem.getUrn(), trackSourceInfo, allTrackUrns, currentTrackPosition);

        return SearchItem.Track.create(trackItem, trackClick);
    }

    private static boolean isAvailableUrn(String urn) {
        return AVAILABLE_URNS.contains(urn);
    }

    private static TopResultsBucketViewModel.Kind urnToKind(Urn urn) {
        switch (urn.toString()) {
            case URN_BUCKET_TOP:
                return TOP_RESULT;
            case URN_BUCKET_TRACKS:
                return TRACKS;
            case URN_BUCKET_GO_TRACKS:
                return GO_TRACKS;
            case URN_BUCKET_PEOPLE:
                return USERS;
            case URN_BUCKET_PLAYLISTS:
                return PLAYLISTS;
            case URN_BUCKET_ALBUMS:
                return ALBUMS;
            default:
                throw new IllegalArgumentException("Unexpected urn type for search: " + urn);
        }
    }

    private static List<Urn> allTracksUrns(List<ApiTopResultsBucket> buckets) {
        List<Urn> allTracks = Lists.newArrayList();
        for (ApiTopResultsBucket bucket : buckets) {
            for (ApiUniversalSearchItem apiUniversalSearchItem : bucket.collection()) {
                if (apiUniversalSearchItem.track().isPresent()) {
                    allTracks.add(apiUniversalSearchItem.track().get().getUrn());
                }
            }
        }
        return allTracks;
    }

    private static List<ApiTrack> allTracks(List<ApiTopResultsBucket> buckets) {
        List<ApiTrack> allTracks = Lists.newArrayList();
        for (ApiTopResultsBucket bucket : buckets) {
            for (ApiUniversalSearchItem apiUniversalSearchItem : bucket.collection()) {
                if (apiUniversalSearchItem.track().isPresent()) {
                    allTracks.add(apiUniversalSearchItem.track().get());
                }
            }
        }
        return allTracks;
    }

    private static List<ApiPlaylist> allPlaylists(List<ApiTopResultsBucket> buckets) {
        List<ApiPlaylist> allPlaylists = Lists.newArrayList();
        for (ApiTopResultsBucket bucket : buckets) {
            for (ApiUniversalSearchItem apiUniversalSearchItem : bucket.collection()) {
                if (apiUniversalSearchItem.playlist().isPresent()) {
                    allPlaylists.add(apiUniversalSearchItem.playlist().get());
                }
            }
        }
        return allPlaylists;
    }

    private static List<ApiUser> allUsers(List<ApiTopResultsBucket> buckets) {
        List<ApiUser> allUsers = Lists.newArrayList();
        for (ApiTopResultsBucket bucket : buckets) {
            for (ApiUniversalSearchItem apiUniversalSearchItem : bucket.collection()) {
                if (apiUniversalSearchItem.user().isPresent()) {
                    allUsers.add(apiUniversalSearchItem.user().get());
                }
            }
        }
        return allUsers;
    }

    private static Module getContextFromBucket(TopResultsBucketViewModel.Kind bucketKind, int positionInBucket) {
        switch (bucketKind) {
            case TOP_RESULT:
                return Module.create(Module.SEARCH_TOP_RESULT, positionInBucket);
            case TRACKS:
                return Module.create(Module.SEARCH_TRACKS, positionInBucket);
            case GO_TRACKS:
                return Module.create(Module.SEARCH_HIGH_TIER, positionInBucket);
            case USERS:
                return Module.create(Module.SEARCH_PEOPLE, positionInBucket);
            case PLAYLISTS:
                return Module.create(Module.SEARCH_PLAYLISTS, positionInBucket);
            case ALBUMS:
                return Module.create(Module.SEARCH_ALBUMS, positionInBucket);
            default:
                throw new IllegalArgumentException("Unexpected bucket type");
        }
    }

}
