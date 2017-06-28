package com.soundcloud.android.view.behavior;

import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

import javax.inject.Inject;

public class ScrollingViewContentBottomPaddingBehavior extends AppBarLayout.ScrollingViewBehavior {

    @Inject
    ContentBottomPaddingHelper helper;

    public ScrollingViewContentBottomPaddingBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return super.layoutDependsOn(parent, child, dependency) || helper.isPlayer(dependency);
    }

    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View player) {
        if (helper.isPlayer(player)) {
            helper.onPlayerChanged(player, child);
        }
        return super.onDependentViewChanged(parent, child, player);
    }
}
