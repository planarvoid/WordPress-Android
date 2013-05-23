package com.soundcloud.android.onboarding;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Genre;
import com.soundcloud.android.model.GenreBucket;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class OnboardingOperationsTest {

    private OnboardingOperations ops;

    @Test
    public void shouldRetrieveAllGenreBuckets() {
        //TODO: mock against the real SC API interface
        OnboardingOperations.FakeApi api = mock(OnboardingOperations.FakeApi.class);
        when(api.getGenreBuckets()).thenReturn(Lists.newArrayList(new GenreBucket(new Genre()), new GenreBucket(new Genre())));
        Observer<GenreBucket> observer = mock(Observer.class);

        ops = new OnboardingOperations(api);

        Observable<GenreBucket> genreBuckets = ops.getGenreBuckets();
        genreBuckets.subscribe(observer);

        verify(observer, times(2)).onNext(any(GenreBucket.class));
        verify(observer, times(1)).onCompleted();
    }

}
