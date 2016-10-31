package com.soundcloud.android.view.adapters;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

public class PrependItemToListSubscriberTest extends AndroidUnitTest {

    @Mock private RecyclerItemAdapter<ListItem, ?> adapter;
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
        verify(adapter).notifyItemInserted(0);
    }
}
