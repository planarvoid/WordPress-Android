package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.TableColumns.Sounds.TYPE_PLAYLIST;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MyPlaylistLikesStateProviderTest {

    private MyPlaylistLikesStateProvider myPlaylistLikesStateProvider;

    @Mock private LoadLikesPendingAdditionCommand loadLikesPendingAddition;
    @Mock private LoadLikesPendingRemovalCommand loadLikesPendingRemoval;

    @Before
    public void setUp() throws Exception {
        myPlaylistLikesStateProvider = new MyPlaylistLikesStateProvider(loadLikesPendingAddition, loadLikesPendingRemoval);
    }

    @Test
    public void hasLocalChangesIsTrueIfPlaylistAdditionsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(buildPlaylist());
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());

        assertThat(myPlaylistLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueIfPlaylistRemovalsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(buildPlaylist());

        assertThat(myPlaylistLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsFalseIfNoPendingAdditionsOrRemovals() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());

        assertThat(myPlaylistLikesStateProvider.hasLocalChanges()).isFalse();
    }

    private List<PropertySet> buildPlaylist() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        return singletonList(TestPropertySets.fromApiPlaylist(playlist, true, false, false, false));
    }
}
