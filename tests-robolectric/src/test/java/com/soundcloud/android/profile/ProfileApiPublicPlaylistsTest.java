package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
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
    @Mock private StoreUsersCommand storeUsersCommand;

    private ProfileApiPublic api;
    private final TestObserver<ModelCollection<ApiPlaylist>> observer = new TestObserver<>();
    private final PublicApiPlaylist publicApiPlaylist1 = ModelFixtures.create(PublicApiPlaylist.class);
    private final PublicApiPlaylist publicApiPlaylist2 = ModelFixtures.create(PublicApiPlaylist.class);
    private final CollectionHolder<PublicApiPlaylist> publicApiCollection = new CollectionHolder<>(
            Arrays.asList(
                    publicApiPlaylist1,
                    publicApiPlaylist2),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiPublic(apiClientRx);
    }

    @Test
    public void returnsUserPlaylistsByUrnFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiPlaylist>> results = Observable.just(publicApiCollection);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/users/123/playlists")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);

        api.userPlaylists(Urn.forUser(123L)).subscribe(observer);
        assertAllItemsEmitted();
    }

    @Test
    public void returnsUserPlaylistsByNextPageLinkFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiPlaylist>> results = Observable.just(publicApiCollection);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);

        api.userPlaylists(NEXT_HREF).subscribe(observer);
        assertAllItemsEmitted();
    }

    private void assertAllItemsEmitted() {
        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).getNextLink().get().getHref()).toEqual(NEXT_HREF);
        expect(observer.getOnNextEvents().get(0).getCollection()).toContain(
                publicApiPlaylist1.toApiMobilePlaylist(),
                publicApiPlaylist2.toApiMobilePlaylist()
        );
    }
}