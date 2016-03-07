package com.soundcloud.android.sync.likes;

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
    public void hasLocalChangesIsTrueIfAdditionsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call()).thenReturn(singletonList(TestPropertySets.fromApiTrack()));
        when(loadLikesPendingRemoval.call()).thenReturn(Collections.<PropertySet>emptyList());

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueIfRemovalsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call()).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call()).thenReturn(singletonList(TestPropertySets.fromApiTrack()));

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsFalseIfNoPendingAdditionsOrRemovals() throws Exception {
        when(loadLikesPendingAddition.call()).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call()).thenReturn(Collections.<PropertySet>emptyList());

        assertThat(myLikesStateProvider.hasLocalChanges()).isFalse();
    }
}
