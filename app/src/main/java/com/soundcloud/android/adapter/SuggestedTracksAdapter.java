package com.soundcloud.android.adapter;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.model.SuggestedTrack;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

public class SuggestedTracksAdapter extends BaseAdapter {

    public static final int INITIAL_LIST_SIZE = 20;

    private List<SuggestedTrack> mSuggestedTracks = Lists.newArrayListWithCapacity(INITIAL_LIST_SIZE);

    public void addSuggestedTrack(SuggestedTrack suggestedTrack) {
        mSuggestedTracks.add(suggestedTrack);
    }

    @Override
    public int getCount() {
        return mSuggestedTracks.size();
    }

    @Override
    public Object getItem(int position) {
        return mSuggestedTracks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mSuggestedTracks.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null){
            convertView = View.inflate(parent.getContext(), R.layout.suggested_track_grid_item, null);
        }
        return convertView;
    }
}
