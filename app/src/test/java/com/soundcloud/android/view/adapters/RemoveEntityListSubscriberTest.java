package com.soundcloud.android.view.adapters;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

public class RemoveEntityListSubscriberTest extends AndroidUnitTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private RecyclerItemAdapter<ListItem, ?> adapter;
    private final TrackItem item = ModelFixtures.trackItem(TRACK_URN);

    private PublishSubject<Urn> observable;

    @Before
    public void setUp() throws Exception {
        RemoveEntityListSubscriber subscriber = new RemoveEntityListSubscriber(adapter);
        observable = PublishSubject.create();
        observable.subscribe(subscriber);
    }

    @Test
    public void onNextShouldRemoveEntityFromAdapter() {
        when(adapter.getItemCount()).thenReturn(1);
        when(adapter.getItem(0)).thenReturn(item);

        observable.onNext(TRACK_URN);

        verify(adapter).removeItem(0);
    }

    @Test
    public void onNextShouldNotifyDataSetChangedOnSuccessfulRemoval() {
        when(adapter.getItemCount()).thenReturn(1);
        when(adapter.getItem(0)).thenReturn(item);

        observable.onNext(TRACK_URN);

        verify(adapter).notifyItemRemoved(0);
    }

    @Test
    public void onNextShouldNotNotifyDataSetChangedIfNoRemovalWasMade() {
        when(adapter.getItemCount()).thenReturn(1);
        when(adapter.getItem(0)).thenReturn(item);

        observable.onNext(Urn.forTrack(9L));

        verify(adapter, never()).removeItem(anyInt());
        verify(adapter, never()).notifyDataSetChanged();
    }
}
