package com.soundcloud.android.screens.search;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.search.CombinedSearchActivity;
import com.soundcloud.android.tests.Han;

import android.widget.TextView;

import java.util.List;

public class SearchPlaylistTagsScreen extends Screen {

    private static final Class ACTIVITY = CombinedSearchActivity.class;

    public SearchPlaylistTagsScreen(Han solo) {
        super(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public PlaylistResultsScreen clickOnTag(int index) {
        solo.getSolo().clickOnView(getTagViews().get(index));
        return new PlaylistResultsScreen(solo);
    }

    public List<String> getTags() {
        return Lists.transform(getTagViews(), new Function<TextView, String>() {
            @Override
            public String apply(TextView input) {
                return input.getText().toString();
            }
        });
    }

    public List<TextView> getTagViews() {
        return solo.getSolo().getCurrentViews(TextView.class, solo.getView(R.id.tags));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForElement(R.id.playlistTagsContainer);
    }

}
