package com.soundcloud.android.introductoryoverlay;

import static com.soundcloud.android.view.CustomFontLoader.SOUNDCLOUD_INTERSTATE_LIGHT;
import static com.soundcloud.android.view.CustomFontLoader.getFont;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.StringRes;
import android.view.View;

import javax.inject.Inject;

public class IntroductoryOverlayPresenter {

    private final IntroductoryOverlayOperations introductoryOverlayOperations;
    private final Resources resources;
    private final EventBusV2 eventBus;

    @Inject
    public IntroductoryOverlayPresenter(IntroductoryOverlayOperations introductoryOverlayOperations,
                                        Resources resources,
                                        EventBusV2 eventBus) {
        this.introductoryOverlayOperations = introductoryOverlayOperations;
        this.resources = resources;
        this.eventBus = eventBus;
    }

    public void showIfNeeded(IntroductoryOverlay introductoryOverlay) {
        final String overlayKey = introductoryOverlay.overlayKey();
        if (!introductoryOverlayOperations.wasOverlayShown(overlayKey)) {
            final View targetView = introductoryOverlay.targetView();
            final Activity activity = getActivity(targetView);
            if (activity != null) {
                show(activity, targetView, introductoryOverlay.title(), introductoryOverlay.description(), introductoryOverlay.icon());
                introductoryOverlayOperations.setOverlayShown(overlayKey);
                introductoryOverlay.event().ifPresent(event -> eventBus.publish(EventQueue.TRACKING, event));
            }
        }
    }

    private void show(Activity activity, final View targetView,
                      @StringRes int title, @StringRes int description, Optional<Drawable> iconOptional) {
        Context context = targetView.getContext();
        TapTarget target = TapTarget.forView(targetView, resources.getString(title), resources.getString(description))
                                    .outerCircleColor(R.color.white)
                                    .targetCircleColor(R.color.soundcloud_orange)
                                    .textColor(R.color.black)
                                    .transparentTarget(false)
                                    .titleTextDimen(R.dimen.shrinkwrap_medium_primary_text_size)
                                    .descriptionTextDimen(R.dimen.shrinkwrap_medium_secondary_text_size)
                                    .textTypeface(getFont(context, SOUNDCLOUD_INTERSTATE_LIGHT));
        iconOptional.ifPresent(target::icon);

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
