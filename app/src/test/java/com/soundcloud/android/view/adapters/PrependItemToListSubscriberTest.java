package com.soundcloud.android.view.adapters;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

@RunWith(SoundCloudTestRunner.class)
public class PrependItemToListSubscriberTest {

    private static final PropertySet ITEM = PropertySet.from(TrackProperty.URN.bind(Urn.forTrack(123L)));

    private PrependItemToListSubscriber subscriber;

    @Mock private ItemAdapter<PropertySet> adapter;
    private PublishSubject<PropertySet> observable;

    @Before
    public void setUp() throws Exception {
        subscriber = new PrependItemToListSubscriber(adapter);
        observable = PublishSubject.create();
        observable.subscribe(subscriber);
    }

    @Test
    public void onNextShouldPrependItemToAdapter() {
        observable.onNext(ITEM);
        verify(adapter).prependItem(ITEM);
    }

    @Test
    public void onNextShouldNotifyDataSetChanged() {
        observable.onNext(ITEM);
        verify(adapter).notifyDataSetChanged();
    }
}