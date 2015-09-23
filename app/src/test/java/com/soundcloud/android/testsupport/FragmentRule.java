package com.soundcloud.android.testsupport;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentRule implements TestRule {

    private final int fragmentLayout;

    private Fragment fragment;
    private Bundle fragmentArgs;

    public FragmentRule(@LayoutRes int fragmentLayout) {
        this(fragmentLayout, null);
    }

    public FragmentRule(@LayoutRes int fragmentLayout, Bundle fragmentArgs) {
        this.fragmentLayout = fragmentLayout;
        this.fragmentArgs = fragmentArgs;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public Activity getActivity() {
        return fragment.getActivity();
    }

    public View getView() {
        return fragment.getView();
    }

    public void setFragmentArguments(Bundle arguments) {
        if (fragmentArgs != null) {
            fragment.getArguments().putAll(arguments);
        } else {
            throw new IllegalArgumentException("Construct FragmentRule by passing an empty Bundle as a parameter");
        }
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        fragment = new DummyFragment(fragmentLayout, fragmentArgs);
        SupportFragmentTestUtil.startVisibleFragment(fragment);
        return statement;
    }

    @SuppressLint("ValidFragment")
    static class DummyFragment extends Fragment {

        @LayoutRes private final int layoutId;

        DummyFragment(int layoutId, Bundle arguments) {
            this.layoutId = layoutId;
            this.setArguments(arguments);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(layoutId, container, false);
        }
    }
}
