package com.soundcloud.android.dialog;

import static com.soundcloud.android.dialog.OnboardSuggestedUsersSyncFragment.FollowingsSyncObserver;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.service.sync.SyncOperations;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class OnboardSuggestedUsersSyncFragmentTest {

    OnboardSuggestedUsersSyncFragment fragment;

    @Mock
    SyncStateManager syncStateManager;
    @Mock
    SyncOperations syncOperations;
    @Mock
    Observable<Void> observable;

    @Before
    public void setup() {
        fragment = new OnboardSuggestedUsersSyncFragment(syncStateManager, syncOperations);
    }

    @Test
    public void shouldForceStreamToStaleOnFinish() {
        when(syncOperations.pushFollowings()).thenReturn(observable);
        fragment.onCreate(null);

        ArgumentCaptor<FollowingsSyncObserver> argumentCaptor = ArgumentCaptor.forClass(FollowingsSyncObserver.class);
        verify(observable).subscribe(argumentCaptor.capture());
        FollowingsSyncObserver observer = argumentCaptor.getValue();

        observer.onCompleted(fragment);
        verify(syncStateManager).forceToStale(Content.ME_SOUND_STREAM);
    }

    @Test
    public void shouldTryThreeTimes() {
        when(syncOperations.pushFollowings()).thenReturn(observable);
        fragment.onCreate(null);

        ArgumentCaptor<FollowingsSyncObserver> argumentCaptor = ArgumentCaptor.forClass(FollowingsSyncObserver.class);
        verify(observable).subscribe(argumentCaptor.capture());
        FollowingsSyncObserver observer = argumentCaptor.getValue();

        observer.onError(fragment, new Exception());
        verify(syncOperations, times(2)).pushFollowings();

        observer.onError(fragment, new Exception());
        verify(syncOperations, times(3)).pushFollowings();

        observer.onError(fragment, new Exception());
        verifyNoMoreInteractions(syncOperations);
    }
}
