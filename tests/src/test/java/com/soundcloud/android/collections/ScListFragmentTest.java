package com.soundcloud.android.collections;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.Event;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.Subscription;

import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class ScListFragmentTest {

    private ScListFragment fragment;

    @Mock
    private Observer<String> observer;

    @Before
    public void setup() {
        fragment = new ScListFragment();
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
    }

    @Test
    public void shouldPublishScreenEnteredEventInOnCreate() {
        Subscription subscription = Event.SCREEN_ENTERED.subscribe(observer);

        fragment.onCreate(null);
        verify(observer).onNext("stream:main");

        subscription.unsubscribe();
    }

}
