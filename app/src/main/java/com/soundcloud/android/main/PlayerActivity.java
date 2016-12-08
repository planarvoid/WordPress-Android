package com.soundcloud.android.main;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.cast.CastIntroductoryOverlayPresenter;
import com.soundcloud.android.comments.AddCommentArguments;
import com.soundcloud.android.comments.CommentController;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public abstract class PlayerActivity extends LoggedInActivity implements CastConnectionHelper.OnConnectionChangeListener {

    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle CommentController commentController;
    @Inject @LightCycle CastIntroductoryOverlayPresenter castIntroductoryOverlayPresenter;

    public PlayerActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onBackPressed() {
        if (accountOperations.isCrawler() || !playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }

    public void addComment(AddCommentArguments arguments) {
        commentController.addComment(arguments);
    }

    @Override
    protected void onResume() {
        super.onResume();
        castConnectionHelper.addOnConnectionChangeListener(this);
    }

    @Override
    protected void onPause() {
        castConnectionHelper.removeOnConnectionChangeListener(this);
        super.onPause();
    }

    @Override
    public void onCastAvailable() {
        castIntroductoryOverlayPresenter.showIntroductoryOverlayForCastIfNeeded();
    }

    @Override
    public void onCastUnavailable() {
        // default impl.: no-op
    }
}
