package com.soundcloud.android.onboarding;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class OnboardingOperationsTest {

    private OnboardingOperations ops;

    @Test
    public void shouldRetrieveAllGenreBuckets() throws CreateModelException {
        //TODO: mock against the real SC API interface
        OnboardingOperations.FakeApi api = mock(OnboardingOperations.FakeApi.class);
        when(api.getCategoryGroups()).thenReturn(Lists.newArrayList(
                TestHelper.buildCategoryGroup(CategoryGroup.KEY_MUSIC, 1),
                TestHelper.buildCategoryGroup(CategoryGroup.KEY_SPEECH_AND_SOUNDS, 1)));
        Observer<CategoryGroup> observer = mock(Observer.class);

        ops = new OnboardingOperations(api);

        Observable<CategoryGroup> genreBuckets = ops.getCategoryGroups();
        genreBuckets.subscribe(observer);

        verify(observer, times(2)).onNext(any(CategoryGroup.class));
        verify(observer, times(1)).onCompleted();
    }

}
