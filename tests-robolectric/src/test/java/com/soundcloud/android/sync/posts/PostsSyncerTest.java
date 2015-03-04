package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

@RunWith(SoundCloudTestRunner.class)
public class PostsSyncerTest {

    private PostsSyncer syncer;

    @Mock private LoadLocalPostsCommand loadPostedPlaylistUrns;
    @Mock private FetchPostsCommand fetcyMyPlaylists;
    @Mock private StorePostsCommand storePlaylistPosts;
    @Mock private RemovePostsCommand removePlaylistPosts;
    @Mock private BulkFetchCommand<ApiPlaylist> fetchPostResources;
    @Mock private StoreCommand<Iterable<ApiPlaylist>> storePostResources;

    private PropertySet post1;
    private PropertySet post2;

    @Before
    public void setUp() throws Exception {
        syncer = new PostsSyncer<>(loadPostedPlaylistUrns, fetcyMyPlaylists,
                storePlaylistPosts, removePlaylistPosts, fetchPostResources, storePostResources);

        post1 = createPost(Urn.forPlaylist(123L), new Date(100L), true);
        post2 = createPost(Urn.forPlaylist(456L), new Date(200L), false);
    }

    @Test
    public void doesNothingIfLocalAndRemoteStateTheSame() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post1);

        expect(syncer.call()).toBe(false);

        verifyZeroInteractions(removePlaylistPosts);
        verifyZeroInteractions(storePlaylistPosts);
    }

    @Test
    public void storesNewPlaylistPost() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post1, post2);

        expect(syncer.call()).toBe(true);

        verify(storePlaylistPosts).call();
        expect(storePlaylistPosts.getInput()).toContainExactly(post2);
        verifyZeroInteractions(removePlaylistPosts);
    }

    @Test
    public void removesInvalidPost() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts();

        expect(syncer.call()).toBe(true);

        verify(storePlaylistPosts, never()).call();
        verify(removePlaylistPosts).call();
        expect(removePlaylistPosts.getInput()).toContainExactly(post1);
    }

    @Test
    public void storesNewPostAndRemovesInvalidPost() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post2);

        expect(syncer.call()).toBe(true);

        verify(storePlaylistPosts).call();
        expect(storePlaylistPosts.getInput()).toContainExactly(post2);
        verify(removePlaylistPosts).call();
        expect(removePlaylistPosts.getInput()).toContainExactly(post1);
    }

    @Test
    public void fetchesAndStoresEntitiesForNewAdditions() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post1, post2);

        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);
        when(fetchPostResources.call()).thenReturn(playlists);

        expect(syncer.call()).toBe(true);

        verify(storePostResources).call();
        expect(storePostResources.getInput()).toEqual(playlists);

    }

    private void withLocalPlaylistPosts(PropertySet... playlistPosts) throws Exception {
        when(loadPostedPlaylistUrns.call()).thenReturn(Arrays.asList(playlistPosts));
    }

    private void withRemotePlaylistPosts(PropertySet... playlistPosts) throws Exception {
        final TreeSet<PropertySet> propertySets = new TreeSet<>(PostProperty.COMPARATOR);
        propertySets.addAll(Arrays.asList(playlistPosts));
        when(fetcyMyPlaylists.call()).thenReturn(propertySets);
    }

    private PropertySet createPost(Urn urn, Date date, boolean isRepost) {
        return PropertySet.from(
                PostProperty.TARGET_URN.bind(urn),
                PostProperty.CREATED_AT.bind(date),
                PostProperty.IS_REPOST.bind(isRepost)
        );
    }
}