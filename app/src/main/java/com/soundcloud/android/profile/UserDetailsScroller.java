package com.soundcloud.android.profile;

import com.soundcloud.android.view.EmptyView;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.view.View;

import javax.inject.Inject;

class UserDetailsScroller extends ProfileEmptyViewScroller {

    @Nullable private View userDetailsHolder;

    @Inject
    public UserDetailsScroller(Resources resources) {
        super(resources);
    }

    public void setViews(View userDetailsHolder, EmptyView emptyView){
        super.setView(emptyView);
        this.userDetailsHolder = userDetailsHolder;
    }

    public void clearViews(){
        super.clearViews();
        userDetailsHolder = null;
    }

    @Override
    protected void configureTopEdges(int currentHeight) {
        super.configureTopEdges(currentHeight);
        if (userDetailsHolder != null) {
            userDetailsHolder.setPadding(0, currentHeight, 0, 0);
        }
    }
}
