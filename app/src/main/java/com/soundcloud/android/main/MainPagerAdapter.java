package com.soundcloud.android.main;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

/**
 * This class is a modified version of {@link android.support.v4.app.FragmentPagerAdapter}:
 * - uses NavigationModel
 * - tags are generated for each fragment are following a known pattern
 * - supports resetting scroll position on a Fragment that implements ScrollContent
 */
public class MainPagerAdapter extends PagerAdapter {

    private static final String FRAGMENT_NAME = "soundcloud:main:";

    private final NavigationModel navigationModel;
    private final FragmentManager fragmentManager;
    private FragmentTransaction currentTransaction;
    private Fragment currentPrimaryItem;

    public MainPagerAdapter(FragmentManager fragmentManager, NavigationModel navigationModel) {
        this.fragmentManager = checkNotNull(fragmentManager);
        this.navigationModel = checkNotNull(navigationModel);
        this.currentTransaction = null;
        this.currentPrimaryItem = null;
    }

    @Override
    public int getCount() {
        return navigationModel.getItemCount();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return String.valueOf(position);
    }

    @Override
    public void startUpdate(ViewGroup container) {
        // no op
    }

    // currentTransaction is started here and committed in finishUpdate()
    @SuppressLint("CommitTransaction")
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (currentTransaction == null) {
            currentTransaction = fragmentManager.beginTransaction();
        }

        String name = makeFragmentName(position);
        Fragment fragment = fragmentManager.findFragmentByTag(name);
        if (fragment != null) {
            currentTransaction.attach(fragment);
        } else {
            fragment = createFragment(position);
            currentTransaction.add(container.getId(), fragment, name);
        }
        if (!fragment.equals(currentPrimaryItem)) {
            fragment.setMenuVisibility(false);
            fragment.setUserVisibleHint(false);
        }
        return fragment;
    }

    private Fragment createFragment(int position) {
        return navigationModel.getItem(position).createFragment();
    }

    public void resetScroll(int position) {
        final Fragment fragment = fragmentManager.findFragmentByTag(makeFragmentName(position));
        if (fragment instanceof ScrollContent) {
            ((ScrollContent) fragment).resetScroll();
        }
    }

    private String makeFragmentName(int position) {
        return FRAGMENT_NAME + position;
    }

    // currentTransaction is started here and committed in finishUpdate()
    @SuppressLint("CommitTransaction")
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (currentTransaction == null) {
            currentTransaction = fragmentManager.beginTransaction();
        }
        currentTransaction.detach((Fragment) object);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (object instanceof Fragment) {
            setPrimaryItem((Fragment) object);
        }
    }

    private void setPrimaryItem(Fragment fragment) {
        if (!fragment.equals(currentPrimaryItem)) {
            if (currentPrimaryItem != null) {
                currentPrimaryItem.setMenuVisibility(false);
                currentPrimaryItem.setUserVisibleHint(false);
            }
            fragment.setMenuVisibility(true);
            fragment.setUserVisibleHint(true);
            currentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (currentTransaction != null) {
            currentTransaction.commitAllowingStateLoss();
            currentTransaction = null;
            fragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        // no op
    }

    public static class Factory {

        private final NavigationModel navigationModel;

        @Inject
        public Factory(NavigationModel navigationModel) {
            this.navigationModel = navigationModel;
        }

        public MainPagerAdapter create(FragmentManager fm) {
            return new MainPagerAdapter(fm, navigationModel);
        }

    }

}
