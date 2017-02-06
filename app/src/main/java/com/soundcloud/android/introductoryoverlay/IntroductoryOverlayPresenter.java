package com.soundcloud.android.introductoryoverlay;

import static com.soundcloud.android.view.CustomFontLoader.SOUNDCLOUD_INTERSTATE_LIGHT;
import static com.soundcloud.android.view.CustomFontLoader.getFont;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.soundcloud.android.R;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.IdRes;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

public class IntroductoryOverlayPresenter {

    private final IntroductoryOverlayOperations introductoryOverlayOperations;

    @Inject
    public IntroductoryOverlayPresenter(IntroductoryOverlayOperations introductoryOverlayOperations) {
        this.introductoryOverlayOperations = introductoryOverlayOperations;
    }

    public void showIfNeeded(String overlayKey,
                             final View targetView, CharSequence title, CharSequence description) {
        if (!introductoryOverlayOperations.wasOverlayShown(overlayKey)) {
            final Activity activity = getActivity(targetView);
            if (activity != null) {
                show(activity, targetView, title, description);
                introductoryOverlayOperations.setOverlayShown(overlayKey);
            }
        }
    }

    public void showForMenuItemIfNeeded(String overlayKey, Toolbar toolbar, @IdRes int menuItemIdRes,
                                        CharSequence title, CharSequence description) {
        Optional<MenuItem> menuItem = findMenuItem(toolbar, menuItemIdRes);
        if (menuItem.isPresent()) {
            View view = menuItem.get().getActionView();
            showIfNeeded(overlayKey, view, title, description);
        }
    }

    private Optional<MenuItem> findMenuItem(Toolbar toolbar, @IdRes int menuItemIdRes) {
        if (toolbar != null && toolbar.getMenu() != null) {
            return Optional.fromNullable(toolbar.getMenu().findItem(menuItemIdRes));
        } else {
            return Optional.absent();
        }
    }

    private void show(Activity activity,
                      final View targetView, CharSequence title, CharSequence description) {
        Context context = targetView.getContext();
        TapTarget target = TapTarget.forView(targetView, title, description)
                                    .outerCircleColor(R.color.white)
                                    .targetCircleColor(R.color.ak_sc_orange)
                                    .textColor(R.color.black)
                                    .transparentTarget(false)
                                    .titleTextDimen(R.dimen.shrinkwrap_medium_primary_text_size)
                                    .descriptionTextDimen(R.dimen.shrinkwrap_medium_secondary_text_size)
                                    .textTypeface(getFont(context, SOUNDCLOUD_INTERSTATE_LIGHT));
        TapTargetView.showFor(activity, target, new TapTargetView.Listener() {
            @Override
            public void onTargetClick(TapTargetView view) {
                super.onTargetClick(view);
                targetView.performClick();
            }

            @Override
            public void onOuterCircleClick(TapTargetView view) {
                super.onOuterCircleClick(view);
                view.dismiss(false);
            }
        });
    }

    @Nullable
    private Activity getActivity(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
