package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.EmptyView;

import javax.inject.Inject;

class ProfileEmptyViewHelper {

    private final AccountOperations accountOperations;

    @Inject
    public ProfileEmptyViewHelper(AccountOperations accountOperations) {
        this.accountOperations = accountOperations;
    }

    public void configureBuilderForUserDetails(EmptyView emptyView, Urn userUrn){
        if (isLoggedInUser(userUrn)){
            emptyView.setMessageText(R.string.info_empty_you_message);
            emptyView.setSecondaryText(R.string.info_empty_you_secondary);
            emptyView.setImage(R.drawable.empty_profile);
        } else {
            emptyView.setMessageText(R.string.info_empty_other_message);
            emptyView.setImage(R.drawable.empty_info);
        }
    }

    private boolean isLoggedInUser(Urn userUrn) {
        return accountOperations.isLoggedInUser(userUrn);
    }

}
