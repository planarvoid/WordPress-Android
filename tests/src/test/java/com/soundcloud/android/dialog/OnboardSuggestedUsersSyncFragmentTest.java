package com.soundcloud.android.dialog;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.dialog.OnboardSuggestedUsersSyncFragment.FollowingsSyncObserver;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class OnboardSuggestedUsersSyncFragmentTest {

    private OnboardSuggestedUsersSyncFragment fragment;

    @Mock
    private FollowingOperations followingOperations;
    @Mock
    private Observable<Boolean> observable;

    @Before
    public void setup() {
        fragment = new OnboardSuggestedUsersSyncFragment(followingOperations);
        Robolectric.shadowOf(fragment).setActivity(new SherlockFragmentActivity());
        Robolectric.shadowOf(fragment).setAttached(true);
    }

    @Test
    public void shouldTriggerBulkFollowInOnCreate() {
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);
        fragment.onCreate(null);
        verify(observable).subscribe(any(FollowingsSyncObserver.class));
    }

    @Test
    public void shouldStartActivityStreamWithSuccessFlagOnSuccess() {
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);
        fragment.onCreate(null);

        ArgumentCaptor<FollowingsSyncObserver> argumentCaptor = ArgumentCaptor.forClass(FollowingsSyncObserver.class);
        verify(observable).subscribe(argumentCaptor.capture());
        FollowingsSyncObserver observer = argumentCaptor.getValue();

        boolean success = true;
        observer.onNext(fragment, success);

        Intent activity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(activity).not.toBeNull();
        expect(activity.getBooleanExtra(Home.EXTRA_ONBOARDING_USERS_RESULT, !success)).toBeTrue();
    }

    @Test
    public void shouldStartActivityStreamWithFailureFlagOnNoSuccess() {
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);
        fragment.onCreate(null);

        ArgumentCaptor<FollowingsSyncObserver> argumentCaptor = ArgumentCaptor.forClass(FollowingsSyncObserver.class);
        verify(observable).subscribe(argumentCaptor.capture());
        FollowingsSyncObserver observer = argumentCaptor.getValue();

        boolean success = false;
        observer.onNext(fragment, success);

        Intent activity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(activity).not.toBeNull();
        expect(activity.getBooleanExtra(Home.EXTRA_ONBOARDING_USERS_RESULT, !success)).toBeFalse();
    }

    @Test
    public void shouldStartActivityStreamWithFailureFlagOnGeneralError() {
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);
        fragment.onCreate(null);

        ArgumentCaptor<FollowingsSyncObserver> argumentCaptor = ArgumentCaptor.forClass(FollowingsSyncObserver.class);
        verify(observable).subscribe(argumentCaptor.capture());
        FollowingsSyncObserver observer = argumentCaptor.getValue();

        observer.onError(fragment, new Exception());

        Intent activity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(activity).not.toBeNull();
        expect(activity.getBooleanExtra(Home.EXTRA_ONBOARDING_USERS_RESULT, true)).toBeFalse();
    }

    @Test
    public void shouldFinishWithSuccessIfObservableCallsOnCompleted() {
        when(followingOperations.waitForActivities(fragment.getActivity())).thenReturn(observable);
        fragment.onCreate(null);

        ArgumentCaptor<FollowingsSyncObserver> argumentCaptor = ArgumentCaptor.forClass(FollowingsSyncObserver.class);
        verify(observable).subscribe(argumentCaptor.capture());
        FollowingsSyncObserver observer = argumentCaptor.getValue();

        observer.onCompleted(fragment);

        Intent activity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(activity).not.toBeNull();
        expect(activity.getBooleanExtra(Home.EXTRA_ONBOARDING_USERS_RESULT, false)).toBeTrue();
    }
}
