package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.SoundAssociationHolder;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PropertySetSource;
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
import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class ProfileApiPublicPostTest {

    private static final String NEXT_HREF = "next-href";
    public static final Date REPOST_DATE = new Date();

    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;

    private ProfileApiPublic api;
    private final TestObserver<ModelCollection<PropertySetSource>> observer = new TestObserver<>();
    private final PublicApiTrack publicApiTrack = ModelFixtures.create(PublicApiTrack.class);
    private final PublicApiPlaylist publicApiPlaylist = ModelFixtures.create(PublicApiPlaylist.class);
    private final SoundAssociationHolder publicApiCollection = new SoundAssociationHolder(
            Arrays.asList(
                    new SoundAssociation(publicApiTrack),
                    new SoundAssociation(publicApiPlaylist),
                    new SoundAssociation(publicApiTrack, REPOST_DATE, Association.Type.TRACK_REPOST),
                    new SoundAssociation(publicApiPlaylist, REPOST_DATE, Association.Type.PLAYLIST_REPOST)),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiPublic(apiClientRx);
    }

    @Test
    public void returnsUserPostsByUrnFromApi() throws Exception {
        final Observable<SoundAssociationHolder> results = Observable.just(publicApiCollection);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/e1/users/123/sounds")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                eq(SoundAssociationHolder.class))).thenReturn(results);

        api.userPosts(Urn.forUser(123L)).subscribe(observer);
        assertAllPostsEmitted();
    }

    @Test
    public void returnsUserPostsByNextPageLinkFromApi() throws Exception {
        final Observable<SoundAssociationHolder> results = Observable.just(publicApiCollection);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                eq(SoundAssociationHolder.class))).thenReturn(results);

        api.userPosts(NEXT_HREF).subscribe(observer);
        assertAllPostsEmitted();
    }

    private void assertAllPostsEmitted() {
        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).getNextLink().get().getHref()).toEqual(NEXT_HREF);
        expect(observer.getOnNextEvents().get(0).getCollection()).toContain(
                new ApiTrackPost(publicApiTrack.toApiMobileTrack()),
                new ApiPlaylistPost(publicApiPlaylist.toApiMobilePlaylist()),
                new ApiTrackRepost(publicApiTrack.toApiMobileTrack(), REPOST_DATE),
                new ApiPlaylistRepost(publicApiPlaylist.toApiMobilePlaylist(), REPOST_DATE)
        );
    }

}