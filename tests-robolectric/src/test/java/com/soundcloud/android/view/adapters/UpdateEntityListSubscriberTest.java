package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class UpdateEntityListSubscriberTest {

    public static final String UPDATED_CREATOR = "Jamie Macdonald";

    private UpdateEntityListSubscriber updateEntityListSubscriber;

    @Mock private ItemAdapter<PropertySet> adapter;

    @Before
    public void setUp() throws Exception {
        updateEntityListSubscriber = new UpdateEntityListSubscriber(adapter);
    }

    @Test
    public void updatesItemWithTheSameUrnAndNotifies() throws Exception {

        PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
        PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();

        PropertySet updated = ModelFixtures.create(ApiTrack.class).toPropertySet();
        updated.put(PlayableProperty.URN, track1.get(PlayableProperty.URN));
        updated.put(PlayableProperty.CREATOR_NAME, UPDATED_CREATOR);

        when(adapter.getItems()).thenReturn(Lists.newArrayList(track1, track2));

        final EntityStateChangedEvent event = EntityStateChangedEvent.fromSync(Arrays.asList(updated));
        updateEntityListSubscriber.onNext(event);

        expect(track1.get(PlayableProperty.CREATOR_NAME)).toEqual(UPDATED_CREATOR);
        verify(adapter).notifyDataSetChanged();

    }

    @Test
    public void doesNotNotifyWithNoMatchingUrns() throws Exception {

        PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
        PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();

        PropertySet updated = ModelFixtures.create(ApiTrack.class).toPropertySet();

        when(adapter.getItems()).thenReturn(Lists.newArrayList(track1, track2));

        final EntityStateChangedEvent event = EntityStateChangedEvent.fromSync(Arrays.asList(updated));
        updateEntityListSubscriber.onNext(event);

        verify(adapter, never()).notifyDataSetChanged();

    }
}