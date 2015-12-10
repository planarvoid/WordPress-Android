package com.soundcloud.android.facebookinvites;

import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;

import android.app.Activity;

import javax.inject.Inject;

public class FacebookInvitesDialogPresenter {

    private static final String FACEBOOK_INVITES_PREVIEW_IMAGE_URL = "https://soundcloud.com/app-invite-preview.png";
    private static final String FACEBOOK_INVITES_APP_LINK_URL = "https://soundcloud.com/";

    private final ImageOperations imageOperations;

    @Inject
    FacebookInvitesDialogPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    public void showForListeners(final Activity activity) {
        show(activity, FACEBOOK_INVITES_APP_LINK_URL, FACEBOOK_INVITES_PREVIEW_IMAGE_URL);
    }

    public void showForCreators(final Activity activity, String url, Urn urn) {
        show(activity, url, imageOperations.getImageUrl(urn, ApiImageSize.T500));
    }

    private void show(final Activity activity, String url, String imageUrl) {
        if (AppInviteDialog.canShow()) {
            AppInviteContent content = new AppInviteContent.Builder()
                    .setApplinkUrl(url)
                    .setPreviewImageUrl(imageUrl)
                    .build();
            AppInviteDialog.show(activity, content);
        }
    }
}
