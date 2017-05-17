package com.soundcloud.android.view.menu;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;

import android.content.Context;

import javax.inject.Inject;

public class ChangeLikeToSaveExperimentMenuHelper {

    private final Context context;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    @Inject
    public ChangeLikeToSaveExperimentMenuHelper(Context context,
                                                ChangeLikeToSaveExperiment changeLikeToSaveExperiment) {
        this.context = context;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
    }

    public String getTitleForLikeAction(boolean isLiked) {
        return changeLikeToSaveExperiment.isEnabled()
               ? getAddActionTitle(isLiked)
               : getLikeActionTitle(isLiked);
    }

    private String getAddActionTitle(boolean isLiked) {
        return isLiked
               ? context.getString(R.string.btn_remove_from_collection)
               : context.getString(R.string.btn_add_to_collection);
    }

    private String getLikeActionTitle(boolean isLiked) {
        return isLiked
               ? context.getString(R.string.btn_unlike)
               : context.getString(R.string.btn_like);
    }
}
