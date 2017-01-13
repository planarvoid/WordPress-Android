package com.soundcloud.android.discovery.welcomeuser;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.android.utils.DateUtils;
import rx.Observable;

import javax.inject.Inject;

public class WelcomeUserOperations {

    private final AccountOperations accountOperations;
    private final UserRepository userRepository;
    private final WelcomeUserStorage welcomeUserStorage;
    private final FeatureFlags featureFlags;

    @Inject
    WelcomeUserOperations(AccountOperations accountOperations,
                          UserRepository userRepository,
                          WelcomeUserStorage welcomeUserStorage,
                          FeatureFlags featureFlags) {
        this.accountOperations = accountOperations;
        this.userRepository = userRepository;
        this.welcomeUserStorage = welcomeUserStorage;
        this.featureFlags = featureFlags;
    }

    public Observable<DiscoveryItem> welcome() {
        Urn userUrn = accountOperations.getLoggedInUserUrn();
        return userRepository.userInfo(userUrn)
                         .flatMap(user -> {
                             if (shouldShowWelcomeForUser(user)) {
                                 welcomeUserStorage.onWelcomeUser();
                                 return Observable.just(WelcomeUserItem.create(user));
                             }
                             return Observable.empty();
                         });
    }

    private boolean shouldShowWelcomeForUser(User user) {
        return welcomeUserStorage.shouldShowWelcome() && isRealUsername(user) && hasAvatar(user) && signedUpRecently(user);
    }

    private boolean signedUpRecently(User user) {
        return featureFlags.isEnabled(Flag.FORCE_SHOW_WELCOME_USER) || DateUtils.isInLastDays(user.signupDate(), 7);
    }

    private static boolean hasAvatar(User user) {
        return user.avatarUrl().isPresent();
    }

    private static boolean isRealUsername(User user) {
        return !user.username().startsWith("user-") || user.firstName().isPresent();
    }
}
