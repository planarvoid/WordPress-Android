package com.soundcloud.android.discovery.welcomeuser;

import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.ProfileUser;
import com.soundcloud.android.profile.UserProfileOperations;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;


@RunWith(MockitoJUnitRunner.class)
public class WelcomeUserOperationsTest {
    private static final Urn USER_URN = Urn.forUser(12398);
    
    @Mock private AccountOperations accountOperations;
    @Mock private UserProfileOperations userProfileOperations;
    @Mock private WelcomeUserStorage welcomeUserStorage;
    @Mock private ProfileUser profileUser;

    private final TestSubscriber<DiscoveryItem> testSubscriber = new TestSubscriber<>();
    private WelcomeUserOperations welcomeUserOperations;

    @Before
    public void setUp() throws Exception {
        welcomeUserOperations = new WelcomeUserOperations(accountOperations, userProfileOperations, welcomeUserStorage);

        when(welcomeUserStorage.shouldShowWelcome()).thenReturn(true);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        when(userProfileOperations.getLocalProfileUser(USER_URN)).thenReturn(Observable.just(profileUser));
        when(profileUser.getUrn()).thenReturn(USER_URN);
    }

    @Test
    public void completeUserReturnsUser() throws Exception {
        when(profileUser.getName()).thenReturn("Fancy Username");
        when(profileUser.getImageUrlTemplate()).thenReturn(Optional.of("https://images.soundcloud.com/fancyimage.bmp"));

        welcomeUserOperations.welcome().subscribe(testSubscriber);

        testSubscriber.assertValue(WelcomeUserItem.create(profileUser, TimeOfDay.getCurrent()));
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void defaultUserNameReturnsEmpty() throws Exception {
        when(profileUser.getName()).thenReturn("user-120398471");

        welcomeUserOperations.welcome().subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void noUserAvatarReturnsEmpty() throws Exception {
        when(profileUser.getName()).thenReturn("Fancy Username");
        when(profileUser.getImageUrlTemplate()).thenReturn(Optional.absent());

        welcomeUserOperations.welcome().subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void emptyUserReturnsEmpty() throws Exception {
        when(profileUser.getName()).thenReturn("user-120398471");

        welcomeUserOperations.welcome().subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void emptyUserStorageReturnsFalse() throws Exception {
        when(welcomeUserStorage.shouldShowWelcome()).thenReturn(false);

        welcomeUserOperations.welcome().subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

}
