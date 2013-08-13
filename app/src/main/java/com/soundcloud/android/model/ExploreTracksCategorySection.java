package com.soundcloud.android.model;


import com.soundcloud.android.R;
import com.soundcloud.android.model.behavior.Titled;

import android.content.res.Resources;

public enum ExploreTracksCategorySection implements Titled {
    MUSIC(R.string.explore_category_header_music),
    AUDIO(R.string.explore_category_header_audio);

    private final int mSectionTitleId;

    ExploreTracksCategorySection(int sectionTitleId) {
        mSectionTitleId = sectionTitleId;
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(mSectionTitleId);
    }
}
