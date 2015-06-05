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

@RunWith(SoundCloudTestRunner.class)
public class ProfileApiPublicLikesTest {

    private static final String NEXT_HREF = "next-href";

    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;

    private ProfileApiPublic api;
    private final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();
    private final SoundAssociation publicApiTrackLike = new SoundAssociation(ModelFixtures.create(PublicApiTrack.class), new Date(), Association.Type.TRACK_LIKE);
    private final SoundAssociation publicApiPlaylistLike = new SoundAssociation(ModelFixtures.create(PublicApiPlaylist.class), new Date(), Association.Type.PLAYLIST_LIKE);
    private final SoundAssociationHolder results = new SoundAssociationHolder(
            Arrays.asList(
                    publicApiTrackLike,
                    publicApiPlaylistLike),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiPublic(apiClientRx, storeTracksCommand, storePlaylistsCommand);
    }

    @Test
    public void returnsUserLikesByUrnFromApi() throws Exception {
        final Observable<SoundAssociationHolder> results = Observable.just(this.results);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/e1/users/123/likes")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                eq(SoundAssociationHolder.class))).thenReturn(results);


        api.userLikes(Urn.forUser(123L)).subscribe(observer);
        assertAllItemsEmitted();
    }

    @Test
    public void writesPostsToDatabase() throws Exception {
        final Observable<SoundAssociationHolder> results = Observable.just(this.results);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/e1/users/123/likes")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                eq(SoundAssociationHolder.class))).thenReturn(results);


        api.userLikes(Urn.forUser(123L)).subscribe(observer);
        verify(storeTracksCommand).call(Arrays.asList((TrackRecord) publicApiTrackLike.getPlayable()));
        verify(storePlaylistsCommand).call(Arrays.asList((PlaylistRecord) publicApiPlaylistLike.getPlayable()));
    }

    @Test
    public void returnsUserLikesByNextPageLinkFromApi() throws Exception {
        final Observable<SoundAssociationHolder> results = Observable.just(this.results);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                eq(SoundAssociationHolder.class))).thenReturn(results);

        api.userLikes(NEXT_HREF).subscribe(observer);
        assertAllItemsEmitted();
    }

    private void assertAllItemsEmitted() {
        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).nextPageLink()).toEqual(Optional.of(NEXT_HREF));
        expect(observer.getOnNextEvents().get(0)).toContainExactly(
                publicApiTrackLike.toPropertySet(),
                publicApiPlaylistLike.toPropertySet()
        );
    }

}