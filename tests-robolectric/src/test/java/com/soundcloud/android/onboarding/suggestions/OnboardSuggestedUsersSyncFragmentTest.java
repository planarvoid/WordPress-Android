package com.soundcloud.android.onboarding.suggestions;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncInitiator;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class OnboardSuggestedUsersSyncFragmentTest {

    private OnboardSuggestedUsersSyncFragment fragment;

    @Mock private FollowingOperations followingOperations;
    @Mock private SyncInitiator syncInitiator;

    @Before
    public void setup() {
        fragment = new OnboardSuggestedUsersSyncFragment(followingOperations, syncInitiator);
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
        Robolectric.shadowOf(fragment).setAttached(true);
    }

    @Test
    public void shouldStartActivityStreamWithSuccessFlagOnSuccess() {
        Observable<Boolean> observable = Observable.just(true);
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);
        fragment.onCreate(null);

        Intent activity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(activity).not.toBeNull();
        expect(activity.getBooleanExtra(MainActivity.EXTRA_ONBOARDING_USERS_RESULT, false)).toBeTrue();
    }

    @Test
    public void shouldStartActivityStreamWithFailureFlagOnNoSuccess() {
        Observable<Boolean> observable = Observable.just(false);
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);
        fragment.onCreate(null);

        Intent activity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(activity).not.toBeNull();
        expect(activity.getBooleanExtra(MainActivity.EXTRA_ONBOARDING_USERS_RESULT, true)).toBeFalse();
    }

    @Test
    public void shouldStartActivityStreamWithFailureFlagOnGeneralError() {
        Observable<Boolean> observable = Observable.error(new Exception());
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);
        fragment.onCreate(null);

        Intent activity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(activity).not.toBeNull();
        expect(activity.getBooleanExtra(MainActivity.EXTRA_ONBOARDING_USERS_RESULT, true)).toBeFalse();
    }

    @Test
    public void shouldFinishWithSuccessIfObservableCallsOnCompletedBeforeOnNextWasCalled() {
        Observable<Boolean> observable = Observable.empty();
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);
        fragment.onCreate(null);

        Intent activity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(activity).not.toBeNull();
        expect(activity.getBooleanExtra(MainActivity.EXTRA_ONBOARDING_USERS_RESULT, false)).toBeTrue();
    }

    // catch case where both onNext and onCompleted would try to finish the activity
    @Test
    public void shouldIgnoreOnCompletedIfActivityIsAlreadyFinishing() {
        FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.isFinishing()).thenReturn(true);
        Robolectric.shadowOf(fragment).setActivity(activity);

        Observable<Boolean> observable = Observable.just(true);
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);

        fragment.onCreate(null);

        verify(activity, atMost(1)).finish();
    }
}
