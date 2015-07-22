package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
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
        when(loadLikesPendingAddition.call()).thenReturn(Arrays.asList(TestPropertySets.fromApiTrack()));
        when(loadLikesPendingRemoval.call()).thenReturn(Collections.<PropertySet>emptyList());

        expect(myLikesStateProvider.hasLocalChanges()).toBeTrue();
    }

    @Test
    public void hasLocalChangesIsTrueIfRemovalsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call()).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call()).thenReturn(Arrays.asList(TestPropertySets.fromApiTrack()));

        expect(myLikesStateProvider.hasLocalChanges()).toBeTrue();
    }

    @Test
    public void hasLocalChangesIsFalseIfNoPendingAdditionsOrRemovals() throws Exception {
        when(loadLikesPendingAddition.call()).thenReturn(Collections.<PropertySet>emptyList());
        when(loadLikesPendingRemoval.call()).thenReturn(Collections.<PropertySet>emptyList());

        expect(myLikesStateProvider.hasLocalChanges()).toBeFalse();
    }
}