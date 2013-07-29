package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedTracksAdapter;
import com.soundcloud.android.api.SuggestedTracksOperations;
import com.soundcloud.android.model.SuggestedTrack;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ScFragmentObserver;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.Subscription;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class SuggestedTracksFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    private static final String LOG_TAG         = "suggested_tracks_frag";

    private Observable<SuggestedTrack> mObservable;
    private SuggestedTracksAdapter mSuggestedTracksAdapter;
    private Subscription mSubscription;
    private GridView mGridView;
    private EmptyListView mEmptyListView;
    private SuggestedTracksOperations mSuggestedTracksOperations;

    public SuggestedTracksFragment() {
        this(new SuggestedTracksAdapter(), new SuggestedTracksOperations());
    }

    public SuggestedTracksFragment(SuggestedTracksAdapter adapter, SuggestedTracksOperations suggestedTracksOperations) {
        mSuggestedTracksAdapter = adapter;
        mSuggestedTracksOperations = suggestedTracksOperations;
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mObservable = mSuggestedTracksOperations.getPopMusic().observeOn(ScSchedulers.UI_SCHEDULER).cache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscription.unsubscribe();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_track_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mGridView = (GridView) view.findViewById(R.id.gridview);
        mGridView.setOnItemClickListener(this);
        mGridView.setAdapter(mSuggestedTracksAdapter);
        mGridView.setEmptyView(mEmptyListView);
        loadSuggestedTracks();
    }

    private void loadSuggestedTracks() {
        mEmptyListView.setStatus(EmptyListView.Status.WAITING);
        mSubscription = mObservable.subscribe(new SuggestedTracksObserver(this));
    }

    private static final class SuggestedTracksObserver extends ScFragmentObserver<SuggestedTracksFragment, SuggestedTrack> {

        public SuggestedTracksObserver(SuggestedTracksFragment fragment) {
            super(fragment);
        }

        @Override
        public void onNext(SuggestedTracksFragment fragment, SuggestedTrack suggestedTrack) {
            fragment.mSuggestedTracksAdapter.addSuggestedTrack(suggestedTrack);
            fragment.mSuggestedTracksAdapter.notifyDataSetChanged();
        }

        @Override
        public void onCompleted(SuggestedTracksFragment fragment) {
            fragment.mEmptyListView.setStatus(EmptyListView.Status.OK);
            Log.d(LOG_TAG, "fragment: onCompleted");
        }

        @Override
        public void onError(SuggestedTracksFragment fragment, Exception error) {
            fragment.mEmptyListView.setStatus(EmptyListView.Status.ERROR);
        }
    }
}
