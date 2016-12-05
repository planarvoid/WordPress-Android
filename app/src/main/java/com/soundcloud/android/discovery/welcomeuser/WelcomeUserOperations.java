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

    @Inject
    WelcomeUserOperations(AccountOperations accountOperations,
                          UserProfileOperations userProfileOperations) {
        this.accountOperations = accountOperations;
        this.userProfileOperations = userProfileOperations;
    }

    public Observable<DiscoveryItem> welcome() {
        Urn userUrn = accountOperations.getLoggedInUserUrn();
        return userProfileOperations.getLocalProfileUser(userUrn)
                         .flatMap(user -> {
                             if (shouldShowWelcomeForUser(user)) {
                                 return Observable.just(WelcomeUserItem.create(user, TimeOfDay.getCurrent()));
                             }
                             return Observable.empty();
                         });
    }

    private static boolean shouldShowWelcomeForUser(ProfileUser user) {
        return isRealUsername(user.getName()) && user.getImageUrlTemplate().isPresent();
    }

    private static boolean isRealUsername(String userName) {
        return !userName.startsWith("user-");
    }
}
