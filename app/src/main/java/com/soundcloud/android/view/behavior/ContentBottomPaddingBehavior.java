package com.soundcloud.android.view.behavior;

import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

import javax.inject.Inject;

public class ContentBottomPaddingBehavior extends CoordinatorLayout.Behavior<View> {

    @Inject
    ContentBottomPaddingHelper helper;

    public ContentBottomPaddingBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return helper.isPlayer(dependency);
    }

    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View player) {
        helper.onPlayerChanged(player, child);
        return false;
    }
}
