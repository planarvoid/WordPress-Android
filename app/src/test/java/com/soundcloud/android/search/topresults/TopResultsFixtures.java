package com.soundcloud.android.search.topresults;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;

public final class TopResultsFixtures {

    static final Urn QUERY_URN = new Urn("soundcloud:query_urn:123");

    static DomainSearchItem searchTrackItem(TrackItem trackItem) {
        return DomainSearchItem.track(trackItem);
    }

    static DomainSearchItem searchUserItem(UserItem user) {
        return DomainSearchItem.user(user);
    }

    static DomainSearchItem searchPlaylistItem(PlaylistItem playlist) {
        return DomainSearchItem.playlist(playlist);
    }

    static TopResults.Bucket trackResultsBucket(DomainSearchItem... domainSearchItems) {
        return TopResults.Bucket.create(TopResults.Bucket.Kind.TRACKS, domainSearchItems.length, Lists.newArrayList(domainSearchItems));
    }

    static TopResults.Bucket playlistResultsBucket(DomainSearchItem... domainSearchItems) {
        return TopResults.Bucket.create(TopResults.Bucket.Kind.PLAYLISTS, domainSearchItems.length, Lists.newArrayList(domainSearchItems));
    }

    static TopResults.Bucket peopleResultsBucket(DomainSearchItem... domainSearchItems) {
        return TopResults.Bucket.create(TopResults.Bucket.Kind.USERS, domainSearchItems.length, Lists.newArrayList(domainSearchItems));
    }

    static TopResults.Bucket topResultsBucket(DomainSearchItem... domainSearchItems) {
        return TopResults.Bucket.create(TopResults.Bucket.Kind.TOP_RESULT, domainSearchItems.length, Lists.newArrayList(domainSearchItems));
    }
}
