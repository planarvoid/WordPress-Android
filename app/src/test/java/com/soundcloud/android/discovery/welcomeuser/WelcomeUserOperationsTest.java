package com.soundcloud.android.discovery.welcomeuser;

import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;

import java.util.Date;
import java.util.concurrent.TimeUnit;


@RunWith(MockitoJUnitRunner.class)
public class WelcomeUserOperationsTest {
    private static final Urn USER_URN = Urn.forUser(12398);
    @Mock private AccountOperations accountOperations;
    @Mock private UserRepository userRepository;
    @Mock private WelcomeUserStorage welcomeUserStorage;
    @Mock private FeatureFlags featureFlags;

    private WelcomeUserOperations welcomeUserOperations;
    private User.Builder userBuilder = ModelFixtures.userBuilder(false).urn(USER_URN);

    @Before
    public void setUp() throws Exception {
        welcomeUserOperations = new WelcomeUserOperations(accountOperations,
                                                          userRepository,
                                                          welcomeUserStorage,
                                                          featureFlags);

        when(featureFlags.isEnabled(Flag.FORCE_SHOW_WELCOME_USER)).thenReturn(false);
        when(welcomeUserStorage.shouldShowWelcome()).thenReturn(true);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        userBuilder.urn(USER_URN);
        userBuilder.username("user-12341234123");
    }

    @Test
    public void completeUserReturnsUser() throws Exception {
        userBuilder.username("Fancy Username");
        userBuilder.firstName(Optional.of("Fancy"));
        userBuilder.signupDate(Optional.of(new Date()));
        userBuilder.avatarUrl(Optional.of("https://images.soundcloud.com/fancyimage.bmp"));
        User profileUser = userBuilder.build();
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(profileUser));

        welcomeUserOperations.welcome().test()
                             .assertValue(WelcomeUserItem.create(profileUser))
                             .assertNoErrors()
                             .assertCompleted();
    }

    @Test
    public void longTimeUserReturnsEmpty() throws Exception {
        userBuilder.username("Fancy Username");
        userBuilder.signupDate(Optional.of(new Date(new Date().getTime() - TimeUnit.MILLISECONDS.convert(10, TimeUnit.DAYS))));
        userBuilder.avatarUrl(Optional.of("https://images.soundcloud.com/fancyimage.bmp"));
        User profileUser = userBuilder.build();
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(profileUser));

        welcomeUserOperations.welcome().test()
                             .assertNoValues()
                             .assertNoErrors()
                             .assertCompleted();
    }

    @Test
    public void defaultUserNameReturnsEmpty() throws Exception {
        userBuilder.avatarUrl(Optional.of("https://images.soundcloud.com/fancyimage.bmp"));
        User profileUser = userBuilder.build();
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(profileUser));

        welcomeUserOperations.welcome().test()
                             .assertNoValues()
                             .assertNoErrors()
                             .assertCompleted();
    }

    @Test
    public void noUserAvatarReturnsEmpty() throws Exception {
        userBuilder.username("Fancy Username");
        userBuilder.avatarUrl(Optional.absent());
        User profileUser = userBuilder.build();
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(profileUser));

        welcomeUserOperations.welcome().test()
                             .assertNoValues()
                             .assertNoErrors()
                             .assertCompleted();
    }

    @Test
    public void emptyUserReturnsEmpty() throws Exception {
        userBuilder.username("user-120398471");
        userBuilder.avatarUrl(Optional.absent());
        User profileUser = userBuilder.build();
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(profileUser));

        welcomeUserOperations.welcome().test()
                             .assertNoValues()
                             .assertNoErrors()
                             .assertCompleted();
    }

    @Test
    public void emptyUserStorageReturnsFalse() throws Exception {
        User profileUser = userBuilder.build();
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(profileUser));
        when(welcomeUserStorage.shouldShowWelcome()).thenReturn(false);

        welcomeUserOperations.welcome().test()
                             .assertNoValues()
                             .assertNoErrors()
                             .assertCompleted();
    }

    @Test
    public void featureFlagForcesWelcomeForOldUser() throws Exception {
        long longTimeAgo = new Date().getTime() - TimeUnit.MILLISECONDS.convert(900, TimeUnit.DAYS);

        userBuilder.username("TestPerson");
        userBuilder.signupDate(Optional.of(new Date(longTimeAgo)));
        User profileUser = userBuilder.build();
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(profileUser));
        when(featureFlags.isEnabled(Flag.FORCE_SHOW_WELCOME_USER)).thenReturn(true);

        welcomeUserOperations.welcome().test()
                             .assertValue(WelcomeUserItem.create(profileUser))
                             .assertNoErrors()
                             .assertCompleted();
    }
}
