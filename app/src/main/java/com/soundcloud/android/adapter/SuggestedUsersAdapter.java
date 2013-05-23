package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.model.GenreBucket;
import com.soundcloud.android.utils.Log;
import rx.util.functions.Action1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

public class SuggestedUsersAdapter extends BaseAdapter {

    private final List<GenreBucket> mGenreBuckets;

    public SuggestedUsersAdapter() {
        mGenreBuckets = new LinkedList<GenreBucket>();
    }

    public void addItem(GenreBucket bucket) {
        mGenreBuckets.add(bucket);
    }

    @Override
    public int getCount() {
        return mGenreBuckets.size();
    }

    @Override
    public GenreBucket getItem(int position) {
        return mGenreBuckets.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemLayout = inflater.inflate(R.layout.genre_list_item, parent, false);
        TextView genreTitle = (TextView) itemLayout.findViewById(android.R.id.text1);
        TextView genreSubtitle = (TextView) itemLayout.findViewById(android.R.id.text2);

        GenreBucket bucket = mGenreBuckets.get(position);

        genreTitle.setText(bucket.getGenre().getName());
        if (bucket.hasUsers()) {
            genreSubtitle.setText(bucket.getUsers().size() + " users");
        }

        return itemLayout;
    }

    public Action1<GenreBucket> onNextGenreBucket() {
        return new Action1<GenreBucket>() {
            @Override
            public void call(GenreBucket bucket) {
                Log.d(SuggestedUsersAdapter.this, "adapter: got " + bucket);
                addItem(bucket);
            }
        };
    }
}
