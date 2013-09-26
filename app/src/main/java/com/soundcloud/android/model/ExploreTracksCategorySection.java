package com.soundcloud.android.model;


import com.soundcloud.android.R;

public enum ExploreTracksCategorySection {
    MUSIC(R.string.discover_category_header_music),
    AUDIO(R.string.discover_category_header_audio);

    private final int mSectionTitleId;

    ExploreTracksCategorySection(int sectionTitleId) {
        mSectionTitleId = sectionTitleId;
    }

    public int getTitleId(){
        return mSectionTitleId;
    }
}
