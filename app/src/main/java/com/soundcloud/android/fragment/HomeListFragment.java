package com.soundcloud.android.fragment;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.view.ScListView;
import rx.Observer;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HomeListFragment extends Fragment implements Observer<Activities> {

    private ScListView mListView;
    private ActivityAdapter mAdapter;

    private Subscription subscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new ActivityAdapter(getActivity(), null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.playlist_fragment, container, false);

        mListView = (ScListView) layout.findViewById(R.id.list);
        mListView.setAdapter(mAdapter);

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

        subscription.unsubscribe();
    }

    @Override
    public void onCompleted() {
        System.out.println("RX: onCompleted");
    }

    @Override
    public void onError(Exception e) {
        System.out.println("RX: onError");
    }

    @Override
    public void onNext(Activities activities) {
        System.out.println("RX: onNext, activities: " + activities.size());
        mAdapter.addItems(activities);
        mAdapter.notifyDataSetChanged();
    }
}
