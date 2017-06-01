package com.soundcloud.android.search.topresults;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.strings.Strings;

import java.util.List;

final class TopResultsMapper {
    private TopResultsMapper() {
    }

    static TopResultsViewModel toViewModel(TopResults topResults) {
        final List<TopResultsBucketViewModel> viewModelItems = Lists.newArrayList();
        final List<TopResults.Bucket> buckets = topResults.buckets();
        for (TopResults.Bucket bucket : buckets) {
            final List<DomainSearchItem> apiUniversalSearchItems = bucket.items();
            final int bucketPosition = buckets.indexOf(bucket);
            final List<SearchItem> result = transformDomainSearchItems(apiUniversalSearchItems, bucketPosition, bucket.kind());
            viewModelItems.add(TopResultsBucketViewModel.create(result, bucket.kind(), bucket.totalResults()));
        }
        return TopResultsViewModel.create(topResults.queryUrn(), viewModelItems);
    }

    private static List<SearchItem> transformDomainSearchItems(List<DomainSearchItem> apiUniversalSearchItems, int bucketPosition, TopResults.Bucket.Kind kind) {
        return Lists.transform(apiUniversalSearchItems, item -> transformSearchItem(item, bucketPosition, kind));
    }

    private static SearchItem transformSearchItem(DomainSearchItem searchItem,
                                                  int bucketPosition,
                                                  TopResults.Bucket.Kind kind) {
        if (searchItem.trackItem().isPresent()) {
            final TrackSourceInfo trackSourceInfo = getTrackSourceInfo(kind);
            TrackItem trackItem = searchItem.trackItem().get();
            return SearchItem.Track.create(trackItem, bucketPosition, trackSourceInfo);
        } else if (searchItem.playlistItem().isPresent()) {
            final PlaylistItem playlistItem = searchItem.playlistItem().get();
            return SearchItem.Playlist.create(playlistItem, bucketPosition, kind.toClickSource());
        } else if (searchItem.userItem().isPresent()) {
            final UserItem userItem = searchItem.userItem().get();
            return SearchItem.User.create(userItem, bucketPosition, kind.toClickSource());
        }
        throw new IllegalArgumentException("ApiSearchItem has to contain either track or playlist or user");
    }

    private static TrackSourceInfo getTrackSourceInfo(TopResults.Bucket.Kind kind) {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.SEARCH_EVERYTHING.get(), true);
        trackSourceInfo.setSource(kind.toClickSource().key, Strings.EMPTY);
        return trackSourceInfo;
    }
}
