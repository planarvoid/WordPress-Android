package com.soundcloud.android.profile;

import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserProfileOperationsProfileTest extends AndroidUnitTest {
    private static final ApiUserProfile USER_PROFILE = new UserProfileFixtures.Builder().build();
    private static final Urn USER_URN = USER_PROFILE.getUser().getUrn();

    private UserProfileOperations operations;
    final TestSubscriber<ApiUserProfile> subscriber = new TestSubscriber<>();

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Mock private StoreProfileCommand storeProfileCommand;

    @Before
    public void setUp() {
        operations = new UserProfileOperations(
                profileApi,
                Schedulers.immediate(),
                loadPlaylistLikedStatuses,
                userRepository,
                writeMixedRecordsCommand,
                storeProfileCommand);

        when(profileApi.userProfile(USER_URN)).thenReturn(Observable.just(USER_PROFILE));
    }

    @Test
    public void userProfileCallsApiAndStoresResponse() {
        operations.userProfile(USER_URN).subscribe(subscriber);

        verify(profileApi).userProfile(USER_URN);
        verify(storeProfileCommand).call(USER_PROFILE);
    }
}
