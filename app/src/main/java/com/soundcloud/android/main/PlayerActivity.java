package com.soundcloud.android.main;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.comments.AddCommentArguments;
import com.soundcloud.android.comments.CommentController;
import com.soundcloud.android.view.status.StatusBarColorController;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public abstract class PlayerActivity extends LoggedInActivity {

    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle CommentController commentController;
    @Inject @LightCycle StatusBarColorController statusBarColorController;
    @Inject AccountOperations accountOperations;

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

}
