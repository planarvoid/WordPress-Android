package com.soundcloud.android.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class UserProfileOperationsProfileTest extends AndroidUnitTest {
    private UserProfileOperations operations;
    private TestSubscriber<UserProfileRecord> subscriber;

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

        subscriber = new TestSubscriber<>();
    }

    @Test
    public void userProfileCallsApiAndStoresResponse() {
        final ApiUserProfile profile = new UserProfileFixtures.Builder().build();
        final Urn userUrn = profile.getUser().toPropertySet().get(UserProperty.URN);
        when(profileApi.userProfile(userUrn)).thenReturn(Observable.just(profile));

        operations.userProfile(userUrn).subscribe(subscriber);

        verify(storeProfileCommand).call(profile);
    }
}
