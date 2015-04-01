package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistLikesStorageTest extends StorageIntegrationTest {

    private static final Date LIKED_DATE_1 = new Date(100);
    private static final Date LIKED_DATE_2 = new Date(200);

    private PlaylistLikesStorage playlistLikesStorage;

    private PropertySet playlist1;
    private PropertySet playlist2;

    @Before
    public void setUp() throws Exception {
        playlistLikesStorage = new PlaylistLikesStorage(propellerRx());
        playlist1 = testFixtures().insertLikedPlaylist(LIKED_DATE_1).toPropertySet();
        playlist2 = testFixtures().insertLikedPlaylist(LIKED_DATE_2).toPropertySet();
    }

    @Test
    public void loadAllLikedPlaylists() throws Exception {
        TestObserver<List<PropertySet>> observer = new TestObserver<>();
        
        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE).subscribe(observer);

        final List<PropertySet> propertySets = Lists.newArrayList(
                expectedLikedPlaylistFor(playlist2, LIKED_DATE_2),
                expectedLikedPlaylistFor(playlist1, LIKED_DATE_1));

        expect(observer.getOnNextEvents()).toContainExactly(propertySets);
    }

    @Test
    public void loadLikedPlaylistsAdhereToLimit() throws Exception {
        TestObserver<List<PropertySet>> observer = new TestObserver<>();
        
        playlistLikesStorage.loadLikedPlaylists(1, Long.MAX_VALUE).subscribe(observer);

        final List<PropertySet> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist2, LIKED_DATE_2));

        expect(observer.getOnNextEvents()).toContainExactly(propertySets);
    }

    @Test
    public void loadLikedPlaylistsAdhereToTimestamp() throws Exception {
        TestObserver<List<PropertySet>> observer = new TestObserver<>();
        
        playlistLikesStorage.loadLikedPlaylists(1, LIKED_DATE_2.getTime()).subscribe(observer);

        final List<PropertySet> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist1, LIKED_DATE_1));

        expect(observer.getOnNextEvents()).toContainExactly(propertySets);
    }

    @Test
    public void loadLikedPlaylistShouldEmitEmptyPropertySetIfLikeDoesNotExist() throws Exception {
        TestObserver<PropertySet> observer = new TestObserver<>();
        ApiPlaylist playlist = testFixtures().insertPlaylist();

        playlistLikesStorage.loadLikedPlaylist(playlist.getUrn()).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(PropertySet.create());
    }

    @Test
    public void loadsPlaylistLike() throws Exception {
        TestObserver<PropertySet> observer = new TestObserver<>();
        PropertySet playlist = testFixtures().insertLikedPlaylist(LIKED_DATE_1).toPropertySet();

        playlistLikesStorage.loadLikedPlaylist(playlist.get(TrackProperty.URN)).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(expectedLikedPlaylistFor(playlist, LIKED_DATE_1));
    }

    @Test
    public void loadsPlaylistLikeWithTrackCountAsMaximumOfLocalAndRemoteFromDatabase() throws Exception {
        TestObserver<PropertySet> observer = new TestObserver<>();
        ApiPlaylist playlist = testFixtures().insertLikedPlaylist(LIKED_DATE_1);

        expect(playlist.getTrackCount()).toEqual(2);

        final Urn playlistUrn = playlist.getUrn();
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        playlistLikesStorage.loadLikedPlaylist(playlistUrn).subscribe(observer);

        expect(observer.getOnNextEvents().get(0).get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(observer.getOnNextEvents().get(0).get(PlaylistProperty.TRACK_COUNT)).toEqual(3);
    }

    static PropertySet expectedLikedPlaylistFor(PropertySet playlist, Date likedAt) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlist.get(PlaylistProperty.URN)),
                PlaylistProperty.TITLE.bind(playlist.get(PlaylistProperty.TITLE)),
                PlaylistProperty.CREATOR_NAME.bind(playlist.get(PlaylistProperty.CREATOR_NAME)),
                PlaylistProperty.TRACK_COUNT.bind(playlist.get(PlaylistProperty.TRACK_COUNT)),
                PlaylistProperty.LIKES_COUNT.bind(playlist.get(PlaylistProperty.LIKES_COUNT)),
                LikeProperty.CREATED_AT.bind((likedAt)),
                PlaylistProperty.IS_PRIVATE.bind(playlist.get(PlaylistProperty.IS_PRIVATE)),
                PlaylistProperty.IS_LIKED.bind(true));
    }
}