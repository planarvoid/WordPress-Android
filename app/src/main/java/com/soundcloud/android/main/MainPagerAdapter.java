package com.soundcloud.android.main;

import org.jetbrains.annotations.NotNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
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
    private final Context context;
    private final FragmentManager fragmentManager;
    private FragmentTransaction currentTransaction;
    private Fragment currentPrimaryItem;

    MainPagerAdapter(Context context,
                     @NotNull FragmentManager fragmentManager,
                     @NotNull NavigationModel navigationModel) {
        this.fragmentManager = fragmentManager;
        this.navigationModel = navigationModel;
        this.context = context;
        this.currentTransaction = null;
        this.currentPrimaryItem = null;
    }

    @Override
    public int getCount() {
        return navigationModel.getItemCount();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return context.getString(navigationModel.getItem(position).getName());
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

    private void setFocus(Fragment fragment, boolean hasFocus) {
        if (fragment instanceof FocusListener) {
            ((FocusListener) fragment).onFocusChange(hasFocus);
        }
        fragment.setMenuVisibility(hasFocus);
        fragment.setUserVisibleHint(hasFocus);
    }

    void setCurrentFragmentFocused() {
        setFocus(currentPrimaryItem, true);
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
            if (currentPrimaryItem == null) {
                // First time this adapter is getting a primary item set so,
                // inform it that it's focused
                setFocus(fragment, true);
            } else {
                setFocus(currentPrimaryItem, false);
            }

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

        public MainPagerAdapter create(AppCompatActivity activity) {
            return new MainPagerAdapter(activity, activity.getSupportFragmentManager(), navigationModel);
        }

    }

    public interface FocusListener {

        void onFocusChange(boolean hasFocus);

    }

    public interface ScrollContent {

        void resetScroll();

    }
}
