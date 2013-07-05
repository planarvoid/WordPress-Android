package com.soundcloud.android.dialog;

import static com.soundcloud.android.dialog.OnboardSuggestedUsersSyncFragment.FollowingsSyncObserver;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;

import java.util.Collection;

@RunWith(SoundCloudTestRunner.class)
public class OnboardSuggestedUsersSyncFragmentTest {

    OnboardSuggestedUsersSyncFragment fragment;

    @Mock
    SyncStateManager syncStateManager;
    @Mock
    FollowingOperations followingOperations;
    @Mock
    Observable<Collection<UserAssociation>> observable;

    @Before
    public void setup() {
        fragment = new OnboardSuggestedUsersSyncFragment(syncStateManager, followingOperations);
    }

    @Test
    @Ignore
    public void shouldForceStreamToStaleOnFinish() {
        when(followingOperations.bulkFollowAssociations(anyCollection())).thenReturn(observable);
        fragment.onCreate(null);
        fragment.onAttach(new SherlockFragmentActivity());

        ArgumentCaptor<FollowingsSyncObserver> argumentCaptor = ArgumentCaptor.forClass(FollowingsSyncObserver.class);
        verify(observable).subscribe(argumentCaptor.capture());
        FollowingsSyncObserver observer = argumentCaptor.getValue();

        observer.onCompleted(fragment);
        verify(syncStateManager).forceToStale(Content.ME_SOUND_STREAM);
    }

}
