package com.soundcloud.android.view.adapters;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

@RunWith(SoundCloudTestRunner.class)
public class PrependItemToListSubscriberTest {

    @Mock private ListItemAdapter<ListItem> adapter;
    @Mock private ListItem item;
    private PublishSubject<ListItem> observable;

    @Before
    public void setUp() throws Exception {
        observable = PublishSubject.create();
        observable.subscribe(new PrependItemToListSubscriber<>(adapter));
    }

    @Test
    public void onNextShouldPrependItemToAdapter() {
        observable.onNext(item);
        verify(adapter).prependItem(item);
    }

    @Test
    public void onNextShouldNotifyDataSetChanged() {
        observable.onNext(item);
        verify(adapter).notifyDataSetChanged();
    }
}