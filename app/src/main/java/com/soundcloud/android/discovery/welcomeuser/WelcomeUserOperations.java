package com.soundcloud.android.discovery.welcomeuser;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.ProfileUser;
import com.soundcloud.android.profile.UserProfileOperations;
import rx.Observable;

import javax.inject.Inject;

public class WelcomeUserOperations {

    private final AccountOperations accountOperations;
    private final UserProfileOperations userProfileOperations;
    private final WelcomeUserStorage welcomeUserStorage;

    @Inject
    WelcomeUserOperations(AccountOperations accountOperations,
                          UserProfileOperations userProfileOperations,
                          WelcomeUserStorage welcomeUserStorage) {
        this.accountOperations = accountOperations;
        this.userProfileOperations = userProfileOperations;
        this.welcomeUserStorage = welcomeUserStorage;
    }

    public Observable<DiscoveryItem> welcome() {
        Urn userUrn = accountOperations.getLoggedInUserUrn();
        return userProfileOperations.getLocalProfileUser(userUrn)
                         .flatMap(user -> {
                             if (shouldShowWelcomeForUser(user)) {
                                 welcomeUserStorage.onWelcomeUser();
                                 return Observable.just(WelcomeUserItem.create(user, TimeOfDay.getCurrent()));
                             }
                             return Observable.empty();
                         });
    }

    private boolean shouldShowWelcomeForUser(ProfileUser user) {
        return welcomeUserStorage.shouldShowWelcome() && isRealUsername(user.getName()) && user.getImageUrlTemplate().isPresent();
    }

    private static boolean isRealUsername(String userName) {
        return !userName.startsWith("user-");
    }
}
