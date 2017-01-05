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

import java.util.Date;
import java.util.concurrent.TimeUnit;


@RunWith(MockitoJUnitRunner.class)
public class WelcomeUserOperationsTest {
    private static final Urn USER_URN = Urn.forUser(12398);
    private final TestSubscriber<DiscoveryItem> testSubscriber = new TestSubscriber<>();
    @Mock private AccountOperations accountOperations;
    @Mock private UserProfileOperations userProfileOperations;
    @Mock private WelcomeUserStorage welcomeUserStorage;
    @Mock private ProfileUser profileUser;
    private WelcomeUserOperations welcomeUserOperations;

    @Before
    public void setUp() throws Exception {
        welcomeUserOperations = new WelcomeUserOperations(accountOperations, userProfileOperations, welcomeUserStorage);

        when(welcomeUserStorage.shouldShowWelcome()).thenReturn(true);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        when(userProfileOperations.getLocalProfileUser(USER_URN)).thenReturn(Observable.just(profileUser));
        when(profileUser.getUrn()).thenReturn(USER_URN);
        when(profileUser.getName()).thenReturn("user-120398471");
        when(profileUser.getFirstName()).thenReturn(Optional.absent());
        when(profileUser.getSignupDate()).thenReturn(Optional.absent());
    }

    @Test
    public void completeUserReturnsUser() throws Exception {
        when(profileUser.getName()).thenReturn("Fancy Username");
        when(profileUser.getFirstName()).thenReturn(Optional.of("Fancy"));
        when(profileUser.getSignupDate()).thenReturn(Optional.of(new Date()));
        when(profileUser.getImageUrlTemplate()).thenReturn(Optional.of("https://images.soundcloud.com/fancyimage.bmp"));

        welcomeUserOperations.welcome().subscribe(testSubscriber);

        testSubscriber.assertValue(WelcomeUserItem.create(profileUser));
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void longTimeUserReturnsEmpty() throws Exception {
        when(profileUser.getName()).thenReturn("Fancy Username");
        when(profileUser.getSignupDate()).thenReturn(Optional.of(new Date(new Date().getTime() - TimeUnit.MILLISECONDS.convert(10, TimeUnit.DAYS))));
        when(profileUser.getImageUrlTemplate()).thenReturn(Optional.of("https://images.soundcloud.com/fancyimage.bmp"));

        welcomeUserOperations.welcome().subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void defaultUserNameReturnsEmpty() throws Exception {
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
