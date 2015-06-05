package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ProfileApiPublicPlaylistsTest {

    private static final String NEXT_HREF = "next-href";

    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;

    private ProfileApiPublic api;
    private final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();
    private final PublicApiPlaylist publicApiPlaylist1 = ModelFixtures.create(PublicApiPlaylist.class);
    private final PublicApiPlaylist publicApiPlaylist2 = ModelFixtures.create(PublicApiPlaylist.class);
    private final CollectionHolder<PublicApiPlaylist> results = new CollectionHolder<>(
            Arrays.asList(
                    publicApiPlaylist1,
                    publicApiPlaylist2),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiPublic(apiClientRx, storeTracksCommand, storePlaylistsCommand);
    }

    @Test
    public void returnsUserPlaylistsByUrnFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiPlaylist>> results = Observable.just(this.results);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/users/123/playlists")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);


        api.userPlaylists(Urn.forUser(123L)).subscribe(observer);
        assertAllItemsEmitted();
    }

    @Test
    public void writesPlaylistsToDatabase() throws Exception {
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/users/123/playlists")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(Observable.just(this.results));

        api.userPlaylists(Urn.forUser(123L)).subscribe(observer);
        verify(storePlaylistsCommand).call(results);
    }

    @Test
    public void returnsUserLikesByNextPageLinkFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiPlaylist>> results = Observable.just(this.results);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);

        api.userPlaylists(NEXT_HREF).subscribe(observer);
        assertAllItemsEmitted();
    }

    private void assertAllItemsEmitted() {
        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).nextPageLink()).toEqual(Optional.of(NEXT_HREF));
        expect(observer.getOnNextEvents().get(0)).toContainExactly(
                publicApiPlaylist1.toPropertySet(),
                publicApiPlaylist2.toPropertySet()
        );
    }

}