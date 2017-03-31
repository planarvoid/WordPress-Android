package com.soundcloud.android.testsupport;

import com.soundcloud.android.R;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

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
        fragment = createFragment(fragmentLayout, fragmentArgs);
        SupportFragmentTestUtil.startVisibleFragment(fragment, DummyFragmentActivity.class, R.id.container);
        return statement;
    }

    protected Fragment createFragment(@LayoutRes int fragmentLayout, Bundle fragmentArgs) {
        return new DummyFragment(fragmentLayout, fragmentArgs);
    }

    private static class DummyFragmentActivity extends FragmentActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            FrameLayout view = new FrameLayout(this);
            view.setId(R.id.container);
            view.addView(getToolbar());

            setContentView(view);
        }

        @NonNull
        private Toolbar getToolbar() {
            Toolbar toolbar = new Toolbar(this);
            toolbar.setId(R.id.toolbar_id);
            return toolbar;
        }
    }

    @SuppressLint("ValidFragment")
    public static class DummyFragment extends Fragment {

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
