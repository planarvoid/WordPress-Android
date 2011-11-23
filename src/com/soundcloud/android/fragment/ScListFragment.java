package com.soundcloud.android.fragment;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.ScListView;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class ScListFragment extends ListFragment {

    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ScActivity activity = (ScActivity) getActivity();

        FrameLayout lframe = new FrameLayout(activity);

        /*
        EmptyCollection emptyCollection = new EmptyCollection(activity);
        emptyCollection.setId(android.R.id.empty);
        lframe.addView(emptyCollection, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
*/

        ScListView lv = new ScListView((ScActivity) getActivity());
        lv.setId(android.R.id.list);
        lv.setDrawSelectorOnTop(false);
        lframe.addView(lv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        //lv.setEmptyView(emptyCollection);

        lframe.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        lv.setOnScrollListener(mOnScrollListener);

        return lframe;
    }

    public ScListView getListView() {
        return (ScListView) super.getListView();
    }

    abstract void loadMore();

    private AbsListView.OnScrollListener mOnScrollListener = new ListView.OnScrollListener() {
        @Override public void onScrollStateChanged(AbsListView absListView, int i) {}
        @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (firstVisibleItem + visibleItemCount >= totalItemCount) {
                loadMore();
            }
        }
    };

}
