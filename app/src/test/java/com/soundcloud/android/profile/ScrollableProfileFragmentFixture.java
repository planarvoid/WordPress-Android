package com.soundcloud.android.profile;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ScrollableProfileFragmentFixture extends ScrollableProfileFragment {

    private final View view;

    public ScrollableProfileFragmentFixture(View view) {
        this.view = view;
    }

    public static ScrollableProfileFragment create(View view) {
        return new ScrollableProfileFragmentFixture(view);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return view;
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[0];
    }
}
