package com.soundcloud.android.view.adapters;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

@RunWith(SoundCloudTestRunner.class)
public class RemoveEntityListSubscriberTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private ItemAdapter<PropertySet> adapter;
    private PublishSubject<EntityStateChangedEvent> observable;

    @Before
    public void setUp() throws Exception {
        RemoveEntityListSubscriber subscriber = new RemoveEntityListSubscriber(adapter);
        observable = PublishSubject.create();
        observable.subscribe(subscriber);
    }

    @Test
    public void onNextShouldRemoveEntityFromAdapter() {
        PropertySet item = PropertySet.from(EntityProperty.URN.bind(TRACK_URN));
        when(adapter.getCount()).thenReturn(1);
        when(adapter.getItem(0)).thenReturn(item);

        observable.onNext(EntityStateChangedEvent.fromLike(TRACK_URN, false, 5));

        verify(adapter).removeAt(0);
    }

    @Test
    public void onNextShouldNotifyDataSetChangedOnSuccessfulRemoval() {
        PropertySet item = PropertySet.from(EntityProperty.URN.bind(TRACK_URN));
        when(adapter.getCount()).thenReturn(1);
        when(adapter.getItem(0)).thenReturn(item);

        observable.onNext(EntityStateChangedEvent.fromLike(TRACK_URN, false, 5));

        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void onNextShouldNotNotifyDataSetChangedIfNoRemovalWasMade() {
        PropertySet item = PropertySet.from(EntityProperty.URN.bind(TRACK_URN));
        when(adapter.getCount()).thenReturn(1);
        when(adapter.getItem(0)).thenReturn(item);

        observable.onNext(EntityStateChangedEvent.fromLike(Urn.forTrack(9L), false, 5));

        verify(adapter, never()).removeAt(anyInt());
        verify(adapter, never()).notifyDataSetChanged();
    }
}