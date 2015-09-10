package com.soundcloud.android.facebookinvites;

import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;

import android.app.Activity;

import javax.inject.Inject;

public class FacebookInvitesDialogPresenter {

    private static final String FACEBOOK_INVITES_PREVIEW_IMAGE_URL = "https://soundcloud.com/app-invite-preview.png";
    private static final String FACEBOOK_INVITES_APP_LINK_URL = "https://soundcloud.com/";

    @Inject
    FacebookInvitesDialogPresenter() {
        // dagger
    }

    public void show(final Activity activity) {
        if (AppInviteDialog.canShow()) {
            AppInviteContent content = new AppInviteContent.Builder()
                    .setApplinkUrl(FACEBOOK_INVITES_APP_LINK_URL)
                    .setPreviewImageUrl(FACEBOOK_INVITES_PREVIEW_IMAGE_URL)
                    .build();
            AppInviteDialog.show(activity, content);
        }
    }
}
