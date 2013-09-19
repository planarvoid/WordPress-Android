package com.soundcloud.android.showcases;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.soundcloud.android.R;

import android.app.Activity;
import android.view.View;

public enum Showcase {
    EXPLORE(1, ShowcaseView.TYPE_ONE_SHOT, R.string.showcase_explore_title, R.string.showcase_explore_message);

    private final int id;
    private final int shotType;
    private final int titleId;
    private final int messageId;

    Showcase(int id, int shotType, int titleId, int messageId) {
        this.id = id;
        this.shotType = shotType;
        this.titleId = titleId;
        this.messageId = messageId;
    }

    public void insertShowcase(Activity activity, View view){
        ShowcaseView.ConfigOptions co = createBaseConfigOptions(activity);
        co.shotType = shotType;
        co.showcaseId = id;

        ShowcaseView.insertShowcaseView(view, activity,
                activity.getResources().getString(titleId),
                activity.getResources().getString(messageId),
                co);
    }

    private ShowcaseView.ConfigOptions createBaseConfigOptions(Activity activity) {
        ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
        co.hideOnClickAnywhere = true;
        co.noButton = true;
        co.fadeInDuration = activity.getResources().getInteger(android.R.integer.config_longAnimTime);
        co.fadeOutDuration = activity.getResources().getInteger(android.R.integer.config_mediumAnimTime);
        return co;
    }
}
