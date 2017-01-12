package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;

public class UserProfileOperationsProfileTest extends AndroidUnitTest {
    private UserProfileOperations operations;
    private TestSubscriber<UserProfile> subscriber;
    private ApiUserProfile profile;
    private Urn userUrn;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Mock private StoreProfileCommand storeProfileCommand;
    @Mock private SpotlightItemStatusLoader spotlightItemStatusLoader;
    @Mock private EventBus eventBus;

    @Before
    public void setUp() {
        operations = new UserProfileOperations(
                profileApi,
                Schedulers.immediate(),
                loadPlaylistLikedStatuses,
                userRepository,
                writeMixedRecordsCommand,
                storeProfileCommand,
                spotlightItemStatusLoader,
                eventBus);

        subscriber = new TestSubscriber<>();
        profile = new UserProfileRecordFixtures.Builder().build();
        userUrn = profile.getUser().getUrn();
        when(profileApi.userProfile(userUrn)).thenReturn(Observable.just(profile));
    }

    @Test
    public void userProfileCallsApiAndStoresResponse() {
        operations.userProfile(userUrn).subscribe(subscriber);

        verify(storeProfileCommand).call(profile);
    }

    @Test
    public void shouldMapUserProfileRecordToUserProfile() throws Exception {
        operations.userProfile(userUrn).subscribe(subscriber);

        List<UserProfile> onNextEvents = subscriber.getOnNextEvents();
        UserProfile expectedUserProfile = UserProfile.fromUserProfileRecord(profile);

        assertThat(onNextEvents).hasSize(1);

        UserProfile actualUserProfile = onNextEvents.get(0);
        assertThat(actualUserProfile.getUser()).isEqualTo(expectedUserProfile.getUser());
        assertThat(actualUserProfile.getSpotlight()).isEqualTo(expectedUserProfile.getSpotlight());
        assertThat(actualUserProfile.getTracks()).isEqualTo(expectedUserProfile.getTracks());
        assertThat(actualUserProfile.getAlbums()).isEqualTo(expectedUserProfile.getAlbums());
        assertThat(actualUserProfile.getPlaylists()).isEqualTo(expectedUserProfile.getPlaylists());
        assertThat(actualUserProfile.getReposts()).isEqualTo(expectedUserProfile.getReposts());
        assertThat(actualUserProfile.getLikes()).isEqualTo(expectedUserProfile.getLikes());
    }

    @Test
    public void shouldUpdateSpotlightItemsWithPlayableInfo() throws Exception {
        operations.userProfile(userUrn).subscribe(subscriber);

        verify(spotlightItemStatusLoader).call(any(UserProfile.class));
    }

    @Test
    public void shouldPublishEntityChangedEvent() {
        User user = User.fromApiUser(profile.getUser());

        operations.userProfile(userUrn).subscribe(subscriber);

        verify(eventBus).publish(EventQueue.USER_CHANGED, UserChangedEvent.forUpdate(user));
    }
}
