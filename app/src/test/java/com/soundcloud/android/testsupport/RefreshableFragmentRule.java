package com.soundcloud.android.testsupport;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public class RefreshableFragmentRule extends FragmentRule {

    public RefreshableFragmentRule(int fragmentLayout) {
        super(fragmentLayout);
    }

    public RefreshableFragmentRule(int fragmentLayout, Bundle fragmentArgs) {
        super(fragmentLayout, fragmentArgs);
    }

    @Override
    protected Fragment createFragment(int fragmentLayout, Bundle fragmentArgs) {
        return new DummyRefreshableFragment(fragmentLayout, fragmentArgs);
    }

    @SuppressLint("ValidFragment")
    public static class DummyRefreshableFragment extends DummyFragment implements RefreshableScreen {

        DummyRefreshableFragment(int layoutId, Bundle arguments) {
            super(layoutId, arguments);
        }

        @Override
        public MultiSwipeRefreshLayout getRefreshLayout() {
            MultiSwipeRefreshLayout view = (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
            checkState(view != null);
            return view;
        }

        @Override
        public View[] getRefreshableViews() {
            return new View[0];
        }
    }
}
