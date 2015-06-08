package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.SoundAssociationHolder;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ProfileApiPublicPostTest {

    private static final String NEXT_HREF = "next-href";

    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;

    private ProfileApiPublic api;
    private final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();
    private final SoundAssociation publicApiTrackPost = new SoundAssociation(ModelFixtures.create(PublicApiTrack.class));
    private final SoundAssociation publicApiPlaylistPost = new SoundAssociation(ModelFixtures.create(PublicApiPlaylist.class));
    private final SoundAssociation publicApiTrackRepost = new SoundAssociation(ModelFixtures.create(PublicApiTrack.class), new Date(), Association.Type.TRACK_REPOST);
    private final SoundAssociation publicApiPlaylistRepost = new SoundAssociation(ModelFixtures.create(PublicApiPlaylist.class), new Date(), Association.Type.PLAYLIST_REPOST);
    private final SoundAssociationHolder postsResults = new SoundAssociationHolder(
            Arrays.asList(
                    publicApiTrackPost,
                    publicApiPlaylistPost,
                    publicApiTrackRepost,
                    publicApiPlaylistRepost),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiPublic(apiClientRx, storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void returnsUserPostsByUrnFromApi() throws Exception {
        final Observable<SoundAssociationHolder> results = Observable.just(postsResults);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/e1/users/123/sounds")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                eq(SoundAssociationHolder.class))).thenReturn(results);


        api.userPosts(Urn.forUser(123L)).subscribe(observer);
        assertAllPostsEmitted();
    }

    @Test
    public void writesPostsToDatabase() throws Exception {
        final Observable<SoundAssociationHolder> results = Observable.just(postsResults);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/e1/users/123/sounds")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                eq(SoundAssociationHolder.class))).thenReturn(results);


        api.userPosts(Urn.forUser(123L)).subscribe(observer);
        final List<TrackRecord> input = Arrays.asList((TrackRecord) publicApiTrackPost.getPlayable(), (TrackRecord) publicApiTrackRepost.getPlayable());
        verify(storeTracksCommand).call(input);
        verify(storePlaylistsCommand).call(Arrays.asList((PlaylistRecord) publicApiPlaylistPost.getPlayable(), (PlaylistRecord) publicApiPlaylistRepost.getPlayable()));
    }

    @Test
    public void returnsUserPostsByNextPageLinkFromApi() throws Exception {
        final Observable<SoundAssociationHolder> results = Observable.just(postsResults);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                eq(SoundAssociationHolder.class))).thenReturn(results);

        api.userPosts(NEXT_HREF).subscribe(observer);
        assertAllPostsEmitted();
    }

    private void assertAllPostsEmitted() {
        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).nextPageLink()).toEqual(Optional.of(NEXT_HREF));
        expect(observer.getOnNextEvents().get(0)).toContainExactly(
                publicApiTrackPost.toPropertySet(),
                publicApiPlaylistPost.toPropertySet(),
                publicApiTrackRepost.toPropertySet(),
                publicApiPlaylistRepost.toPropertySet()
        );
    }

}