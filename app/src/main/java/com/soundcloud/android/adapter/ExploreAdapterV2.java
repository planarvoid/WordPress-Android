package com.soundcloud.android.adapter;

import com.soundcloud.android.model.Track;

import android.view.View;
import android.view.ViewGroup;


public class ExploreAdapterV2 extends ScAdapter<Track>{

    public ExploreAdapterV2(int pageSize){
        super(pageSize);
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        return null;
    }

    @Override
    protected void bindItemView(int position, View itemView) {

    }
}
