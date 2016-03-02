package com.soundcloud.android.testsupport;

import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ActivityController;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

public class TestFragmentController {

    private final FragmentActivity activity;
    private final Fragment fragment;

    private TestFragmentController(Fragment fragment) {
        this.fragment = fragment;
        this.activity = ActivityController.of(Robolectric.getShadowsAdapter(), new FragmentActivity()).create().get();
    }

    public static TestFragmentController of(Fragment fragment) {
        return new TestFragmentController(fragment);
    }

    public void create() {
        if (!addFragment(fragment)) {
            create(null);
        }
    }

    public void create(Bundle saveInstanceState) {
        if (!addFragment(fragment)) {
            fragment.onCreate(saveInstanceState);
        }
    }

    public View createView() {
        final Bundle instanceState = new Bundle();
        final FrameLayout fragmentView = new FrameLayout(RuntimeEnvironment.application);
        fragment.onCreateView(LayoutInflater.from(RuntimeEnvironment.application), fragmentView, instanceState);
        fragment.onViewCreated(fragmentView, instanceState);
        return fragmentView;
    }

    public void destroy() {
        fragment.onDestroy();
    }

    public FragmentActivity getActivity() {
        return activity;
    }

    private boolean addFragment(Fragment fragment) {
        final FragmentManager supportFragmentManager = activity.getSupportFragmentManager();
        if (supportFragmentManager.getFragments() == null) {
            supportFragmentManager.beginTransaction().add(fragment, "TAG").commit();
            return true;
        }
        return false;
    }
}
