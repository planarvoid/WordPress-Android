package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;

public class UserProfileOperationsProfileTest extends AndroidUnitTest {

    private static final ModelCollection<ApiPlaylistPost> API_ALBUMS = new ModelCollection<>();
    private UserProfileOperations operations;
    private TestSubscriber<UserProfile> subscriber = new TestSubscriber<>();
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

    private ModelCollection<ApiPlayableSource> spotlight;
    private ModelCollection<ApiTrackPost> tracks;
    private ModelCollection<ApiPlaylistPost> albums;
    private ModelCollection<ApiPlaylistPost> playlists;
    private ModelCollection<ApiPlayableSource> reposts;
    private ModelCollection<ApiPlayableSource> likes;
    private EntityItemCreator entityItemCreator;


    @Before
    public void setUp() {
        entityItemCreator = ModelFixtures.entityItemCreator();

        operations = new UserProfileOperations(
                profileApi,
                Schedulers.immediate(),
                loadPlaylistLikedStatuses,
                userRepository,
                writeMixedRecordsCommand,
                storeProfileCommand,
                storeUsersCommand,
                spotlightItemStatusLoader,
                ModelFixtures.entityItemCreator(),
                eventBus);

        spotlight = new ModelCollection<>(Collections.singletonList(ModelFixtures.apiPlaylistHolder()));
        tracks = new ModelCollection<>(Collections.singletonList(new ApiTrackPost(ModelFixtures.create(ApiTrack.class))));
        albums = new ModelCollection<>(Collections.singletonList(new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class))));
        playlists = new ModelCollection<>(Collections.singletonList(new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class))));
        reposts = new ModelCollection<>(Collections.singletonList(ModelFixtures.apiTrackHolder()));
        likes = new ModelCollection<>(Collections.singletonList(ModelFixtures.apiTrackHolder()));
        profile = new UserProfileRecordFixtures.Builder()
                .albums(albums)
                .spotlight(spotlight)
                .tracks(tracks)
                .playlists(playlists)
                .reposts(reposts)
                .likes(likes)
                .build();

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

        assertThat(onNextEvents).hasSize(1);

        UserProfile actualUserProfile = onNextEvents.get(0);
        assertThat(actualUserProfile.getUser()).isEqualTo(ModelFixtures.entityItemCreator().userItem(profile.getUser()));
        assertThat(actualUserProfile.getSpotlight()).isEqualTo(spotlight.transform(entityItemCreator::playableItem));
        assertThat(actualUserProfile.getTracks()).isEqualTo(tracks.transform(entityItemCreator::trackItem));
        assertThat(actualUserProfile.getAlbums()).isEqualTo(albums.transform(entityItemCreator::playlistItem));
        assertThat(actualUserProfile.getPlaylists()).isEqualTo(playlists.transform(entityItemCreator::playlistItem));
        assertThat(actualUserProfile.getReposts()).isEqualTo(reposts.transform(entityItemCreator::playableItem));
        assertThat(actualUserProfile.getLikes()).isEqualTo(likes.transform(entityItemCreator::playableItem));
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
