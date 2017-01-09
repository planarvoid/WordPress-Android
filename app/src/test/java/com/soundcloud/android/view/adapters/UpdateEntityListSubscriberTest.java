package com.soundcloud.android.view.adapters;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.widget.RecyclerView;

import java.util.Arrays;
import java.util.Collections;

public class UpdateEntityListSubscriberTest extends AndroidUnitTest {

    public static final String UPDATED_CREATOR = "Jamie Macdonald";

    private UpdateEntityListSubscriber updateEntityListSubscriber;

    @Mock private RecyclerItemAdapter<TrackItem, RecyclerView.ViewHolder> adapter;

    @Before
    public void setUp() throws Exception {
        updateEntityListSubscriber = new UpdateEntityListSubscriber(adapter);
    }

    @Test
    public void updatesItemWithTheSameUrnAndNotifies() {
        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));

        PropertySet changeSet = PropertySet.from(
                PlayableProperty.URN.bind(track1.getUrn()),
                PlayableProperty.CREATOR_NAME.bind(UPDATED_CREATOR));

        when(adapter.getItems()).thenReturn(Arrays.asList(track1, track2));

        final EntityStateChangedEvent event = EntityStateChangedEvent.forUpdate(Collections.singletonList(changeSet));
        updateEntityListSubscriber.onNext(event);

        assertThat(track1.getCreatorName()).isEqualTo(UPDATED_CREATOR);
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void doesNotNotifyWithNoMatchingUrns() {
        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));

        ApiTrack changeSet = ModelFixtures.create(ApiTrack.class);

        when(adapter.getItems()).thenReturn(newArrayList(track1, track2));

        final EntityStateChangedEvent event = changeSet.toUpdateEvent();
        updateEntityListSubscriber.onNext(event);

        verify(adapter, never()).notifyDataSetChanged();
    }
}
