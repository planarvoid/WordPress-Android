package com.soundcloud.android.sync.posts;

import com.soundcloud.android.testsupport.AndroidUnitTest;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

// uses AndroidUnitTest because of PropertySet
public class PostsSyncerTest extends AndroidUnitTest {

    private PostsSyncer syncer;

    @Mock private LoadLocalPostsCommand loadPostedPlaylistUrns;
    @Mock private FetchPostsCommand fetcyMyPlaylists;
    @Mock private StorePostsCommand storePlaylistPosts;
    @Mock private RemovePostsCommand removePlaylistPosts;
    @Mock private BulkFetchCommand<ApiPlaylist> fetchPostResources;
    @Mock private StorePlaylistsCommand storePostResources;
    private TestEventBus eventBus = new TestEventBus();

    private PropertySet post1;
    private PropertySet post2;

    @Before
    public void setUp() throws Exception {
        syncer = new PostsSyncer<>(loadPostedPlaylistUrns, fetcyMyPlaylists,
                storePlaylistPosts, removePlaylistPosts, fetchPostResources, storePostResources, eventBus);

        post1 = createPost(Urn.forPlaylist(123L), new Date(100L), true);
        post2 = createPost(Urn.forPlaylist(456L), new Date(200L), false);
    }

    @Test
    public void doesNothingIfLocalAndRemoteStateTheSame() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post1);

        assertThat(syncer.call()).isFalse();

        verifyZeroInteractions(removePlaylistPosts);
        verifyZeroInteractions(storePlaylistPosts);
    }

    @Test
    public void storesNewPlaylistPost() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post1, post2);

        assertThat(syncer.call()).isTrue();

        verify(storePlaylistPosts).call(singleton(post2));
        verifyZeroInteractions(removePlaylistPosts);
    }

    @Test
    public void removesInvalidPost() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts();

        assertThat(syncer.call()).isTrue();

        verify(storePlaylistPosts, never()).call(any(Collection.class));
        verify(removePlaylistPosts).call(singleton(post1));
    }

    @Test
    public void storesNewPostAndRemovesInvalidPost() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post2);

        assertThat(syncer.call()).isTrue();

        verify(storePlaylistPosts).call(singleton(post2));
        verify(removePlaylistPosts).call(singleton(post1));
    }

    @Test
    public void sendsEntityChangedEventsForRepostAdditionAndRemoval() throws Exception {
        final Urn localPostUrn = Urn.forPlaylist(123L);
        PropertySet post1 = createPost(localPostUrn, new Date(100L), true);
        final Urn remotePostUrn = Urn.forPlaylist(456L);
        PropertySet post2 = createPost(remotePostUrn, new Date(200L), true);

        // ignored, as they are not reposts
        final PropertySet ignoredLocalPost = createPost(Urn.forPlaylist(7L), new Date(100L), false);
        final PropertySet ignoredRemotePost = createPost(Urn.forPlaylist(8L), new Date(100L), false);

        withLocalPlaylistPosts(post1, ignoredLocalPost);
        withRemotePlaylistPosts(post2, ignoredRemotePost);

        assertThat(syncer.call()).isTrue();

        final EntityStateChangedEvent expectedEntityChangedSet = EntityStateChangedEvent.fromSync(
                Sets.newHashSet(
                        createRepostedEntityChangedProperty(remotePostUrn),
                        createUnrepostedEntityChangedProperty(localPostUrn)
                )
        );
        assertThat(eventBus.eventsOn(EventQueue.ENTITY_STATE_CHANGED)).containsExactly(expectedEntityChangedSet);
    }

    private PropertySet createRepostedEntityChangedProperty(Urn urn) {
        return PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_USER_REPOST.bind(true)
        );
    }

    private PropertySet createUnrepostedEntityChangedProperty(Urn urn) {
        return PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_USER_REPOST.bind(false)
        );
    }

    @Test
    public void fetchesAndStoresEntitiesForNewAdditions() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post1, post2);

        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);
        when(fetchPostResources.call()).thenReturn(playlists);

        assertThat(syncer.call()).isTrue();

        verify(storePostResources).call(playlists);

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
