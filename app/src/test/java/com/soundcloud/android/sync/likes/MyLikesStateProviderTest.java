package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class MyLikesStateProviderTest {

    private MyLikesStateProvider myLikesStateProvider;

    @Mock private LoadLikesPendingAdditionCommand loadLikesPendingAddition;
    @Mock private LoadLikesPendingRemovalCommand loadLikesPendingRemoval;

    @Before
    public void setUp() throws Exception {
        myLikesStateProvider = new MyLikesStateProvider(loadLikesPendingAddition, loadLikesPendingRemoval);
    }

    @Test
    public void hasLocalChangesIsTrueIfTrackAdditionsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(singletonList(TestPropertySets.fromApiTrack()));
        when(loadLikesPendingRemoval.call(TYPE_TRACK)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueIfTrackRemovalsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_TRACK)).thenReturn(singletonList(TestPropertySets.fromApiTrack()));
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueIfPlaylistAdditionsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_TRACK)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(singletonList(TestPropertySets.fromApiTrack()));
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueIfPlaylistRemovalsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_TRACK)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(singletonList(TestPropertySets.fromApiTrack()));

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsFalseIfNoPendingAdditionsOrRemovals() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_TRACK)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(Collections.<PropertySet>emptyList());

        assertThat(myLikesStateProvider.hasLocalChanges()).isFalse();
    }
}
