package com.soundcloud.android.fragment;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.database.Subscriptions;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.view.ScListView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

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

        Observable<Activities> observable = Subscriptions.getActivities();
        subscription = observable.subscribe(this);

        Observable<Playlist> zip = Observable.zip(Subscriptions.getPlaylist(), Subscriptions.getPlaylistTracks(), new Func2<Playlist, List<Track>, Playlist>() {
            @Override
            public Playlist call(Playlist playlist, List<Track> tracks) {
                playlist.tracks = tracks;
                playlist.setTrackCount(tracks.size());
                return playlist;
            }
        });
        zip.subscribe(new Observer<Playlist>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onNext(Playlist args) {
                System.out.println(args);
            }
        });
        zip.subscribe(new Observer<Playlist>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onNext(Playlist args) {
                System.out.println(args);
            }
        });
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
