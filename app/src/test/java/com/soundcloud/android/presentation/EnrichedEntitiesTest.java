package com.soundcloud.android.presentation;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.associations.RepostStatuses;
import com.soundcloud.android.associations.RepostsStateProvider;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.BehaviorSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

public class EnrichedEntitiesTest extends AndroidUnitTest {

    @Mock LikesStateProvider likesStateProvider;
    @Mock RepostsStateProvider repostsStateProvider;
    @Mock PlaySessionStateProvider playSessionStateProvider;
    @Mock FollowingStateProvider followingsStateProvider;

    private EnrichedEntities enrichedEntities;

    private BehaviorSubject<LikedStatuses> likesStatusPublisher = BehaviorSubject.createDefault(LikedStatuses.create(Collections.emptySet()));
    private BehaviorSubject<RepostStatuses> repostsStatusPublisher = BehaviorSubject.createDefault(RepostStatuses.create(Collections.emptySet()));
    private BehaviorSubject<FollowingStatuses> followingsStatusPublisher = BehaviorSubject.createDefault(FollowingStatuses.create(Collections.emptySet()));
    private BehaviorSubject<Urn> nowPlayingUrnPublisher = BehaviorSubject.createDefault(Urn.NOT_SET);

    @Before
    public void setUp() throws Exception {
        enrichedEntities = new EnrichedEntities(new EntityItemCreator(),
                                                likesStateProvider,
                                                repostsStateProvider,
                                                playSessionStateProvider,
                                                followingsStateProvider);

        when(likesStateProvider.likedStatuses()).thenReturn(likesStatusPublisher);
        when(repostsStateProvider.repostedStatuses()).thenReturn(repostsStatusPublisher);
        when(followingsStateProvider.followingStatuses()).thenReturn(followingsStatusPublisher);
        when(playSessionStateProvider.nowPlayingUrn()).thenReturn(nowPlayingUrnPublisher);
    }

    @Test
    public void trackReemitsAfterLike() throws Exception {
        ApiTrack apiTrack1 = ModelFixtures.apiTrack();
        ApiTrack apiTrack2 = ModelFixtures.apiTrack();

        TestObserver<List<TrackItem>> testObserver = enrichedEntities.trackItems(asList(apiTrack1, apiTrack2)).test();

        likesStatusPublisher.onNext(LikedStatuses.create(singleton(apiTrack1.getUrn())));

        testObserver.assertValues(
                asList(ModelFixtures.trackItem(apiTrack1), ModelFixtures.trackItem(apiTrack2)),
                asList(ModelFixtures.trackItem(apiTrack1).updateLikeState(true), ModelFixtures.trackItem(apiTrack2))
        );
    }

    @Test
    public void trackReemitsAfterNowPlayingChange() throws Exception {
        ApiTrack apiTrack1 = ModelFixtures.apiTrack();
        ApiTrack apiTrack2 = ModelFixtures.apiTrack();

        TestObserver<List<TrackItem>> testObserver = enrichedEntities.trackItems(asList(apiTrack1, apiTrack2)).test();

        nowPlayingUrnPublisher.onNext(apiTrack1.getUrn());

        testObserver.assertValues(
                asList(ModelFixtures.trackItem(apiTrack1), ModelFixtures.trackItem(apiTrack2)),
                asList(ModelFixtures.trackItem(apiTrack1).updateNowPlaying(apiTrack1.getUrn()), ModelFixtures.trackItem(apiTrack2))
        );
    }

    @Test
    public void playlistReemitsAfterLike() throws Exception {
        ApiPlaylist apiPlaylist1 = ModelFixtures.apiPlaylist();
        ApiPlaylist apiPlaylist2 = ModelFixtures.apiPlaylist();

        TestObserver<List<PlaylistItem>> testObserver = enrichedEntities.playlistItems(asList(apiPlaylist1, apiPlaylist2)).test();

        likesStatusPublisher.onNext(LikedStatuses.create(singleton(apiPlaylist1.getUrn())));

        testObserver.assertValues(
                asList(ModelFixtures.playlistItem(apiPlaylist1), ModelFixtures.playlistItem(apiPlaylist2)),
                asList(ModelFixtures.playlistItem(apiPlaylist1).updateLikeState(true), ModelFixtures.playlistItem(apiPlaylist2))
        );
    }

    @Test
    public void playlistReemitsAfterRepost() throws Exception {
        ApiPlaylist apiPlaylist1 = ModelFixtures.apiPlaylist();
        ApiPlaylist apiPlaylist2 = ModelFixtures.apiPlaylist();

        TestObserver<List<PlaylistItem>> testObserver = enrichedEntities.playlistItems(asList(apiPlaylist1, apiPlaylist2)).test();

        repostsStatusPublisher.onNext(RepostStatuses.create(singleton(apiPlaylist1.getUrn())));

        testObserver.assertValues(
                asList(ModelFixtures.playlistItem(apiPlaylist1), ModelFixtures.playlistItem(apiPlaylist2)),
                asList(ModelFixtures.playlistItem(apiPlaylist1).updatedWithLikeAndRepostStatus(false, true), ModelFixtures.playlistItem(apiPlaylist2))
        );
    }

    @Test
    public void userReemitsAfterFollowing() throws Exception {
        ApiUser apiUser1 = ModelFixtures.apiUser();
        ApiUser apiUser2 = ModelFixtures.apiUser();

        TestObserver<List<UserItem>> testObserver = enrichedEntities.userItems(asList(apiUser1, apiUser2)).test();

        followingsStatusPublisher.onNext(FollowingStatuses.create(singleton(apiUser1.getUrn())));

        testObserver.assertValues(
                asList(ModelFixtures.userItem(apiUser1), ModelFixtures.userItem(apiUser2)),
                asList(ModelFixtures.userItem(apiUser1).copyWithFollowing(true), ModelFixtures.userItem(apiUser2))
        );
    }
}
