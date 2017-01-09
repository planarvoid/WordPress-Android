package com.soundcloud.android.discovery.welcomeuser;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import rx.Observable;

import javax.inject.Inject;

public class WelcomeUserOperations {

    private final AccountOperations accountOperations;
    private final UserRepository userRepository;
    private final WelcomeUserStorage welcomeUserStorage;

    @Inject
    WelcomeUserOperations(AccountOperations accountOperations,
                          UserRepository userRepository,
                          WelcomeUserStorage welcomeUserStorage) {
        this.accountOperations = accountOperations;
        this.userRepository = userRepository;
        this.welcomeUserStorage = welcomeUserStorage;
    }

    public Observable<DiscoveryItem> welcome() {
        Urn userUrn = accountOperations.getLoggedInUserUrn();
        return userRepository.userInfo(userUrn)
                         .flatMap(user -> {
                             if (shouldShowWelcomeForUser(user)) {
                                 welcomeUserStorage.onWelcomeUser();
                                 return Observable.just(WelcomeUserItem.create(user, TimeOfDay.getCurrent()));
                             }
                             return Observable.empty();
                         });
    }

    private boolean shouldShowWelcomeForUser(User user) {
        return welcomeUserStorage.shouldShowWelcome() && isRealUsername(user.username()) && user.avatarUrl().isPresent();
    }

    private static boolean isRealUsername(String userName) {
        return !userName.startsWith("user-");
    }
}
