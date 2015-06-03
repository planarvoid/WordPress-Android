package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.presentation.RecyclerViewAdapter;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v7.widget.RecyclerView;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class UpdateEntityListSubscriberTest {

    public static final String UPDATED_CREATOR = "Jamie Macdonald";

    private UpdateEntityListSubscriber updateEntityListSubscriber;

    @Mock private RecyclerViewAdapter<TrackItem, RecyclerView.ViewHolder> adapter;

    @Before
    public void setUp() throws Exception {
        updateEntityListSubscriber = new UpdateEntityListSubscriber(adapter);
    }

    @Test
    public void updatesItemWithTheSameUrnAndNotifies() throws Exception {

        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));

        PropertySet changeSet = PropertySet.from(
            PlayableProperty.URN.bind(track1.getEntityUrn()),
            PlayableProperty.CREATOR_NAME.bind(UPDATED_CREATOR));

        when(adapter.getItems()).thenReturn(Arrays.asList(track1, track2));

        final EntityStateChangedEvent event = EntityStateChangedEvent.fromSync(Arrays.asList(changeSet));
        updateEntityListSubscriber.onNext(event);

        expect(track1.getCreatorName()).toEqual(UPDATED_CREATOR);
        verify(adapter).notifyDataSetChanged();

    }

    @Test
    public void doesNotNotifyWithNoMatchingUrns() throws Exception {

        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));

        PropertySet changeSet = ModelFixtures.create(ApiTrack.class).toPropertySet();

        when(adapter.getItems()).thenReturn(Lists.newArrayList(track1, track2));

        final EntityStateChangedEvent event = EntityStateChangedEvent.fromSync(Arrays.asList(changeSet));
        updateEntityListSubscriber.onNext(event);

        verify(adapter, never()).notifyDataSetChanged();

    }
}