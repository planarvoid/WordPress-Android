package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedTracksAdapter;
import com.soundcloud.android.api.SuggestedTrackOperations;
import com.soundcloud.android.model.SuggestedTrack;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.android.RxFragmentObserver;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class SuggestedTracksFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    private static final String KEY_OBSERVABLE  = "observable";
    private static final String FRAGMENT_TAG    = "suggested_tracks_fragment";
    private static final String LOG_TAG         = "suggested_tracks_frag";

    private DisplayMode mMode = DisplayMode.LOADING;
    private enum DisplayMode {
        LOADING, ERROR, CONTENT
    }

    private final SuggestedTrackOperations mSuggestedTrackOperations;
    private SuggestedTracksAdapter mSuggestedTracksAdapter;

    private Subscription mSubscription;
    private Observer<SuggestedTrack> mObserver;

    private GridView mGridView;
    private EmptyListView mEmptyListView;

    public SuggestedTracksFragment() {
        this(new SuggestedTrackOperations(), new SuggestedTracksAdapter());
    }

    public SuggestedTracksFragment(SuggestedTrackOperations trackOperations, SuggestedTracksAdapter adapter){
        super();
        mSuggestedTrackOperations = trackOperations;
        mSuggestedTracksAdapter = adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_track_fragment, container, false);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGridView = null;
        mEmptyListView = null;
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

        StateHolderFragment savedState = StateHolderFragment.obtain(getFragmentManager(), FRAGMENT_TAG);
        Observable<?> observable;
        if (savedState.has(KEY_OBSERVABLE)){
            observable = savedState.get(KEY_OBSERVABLE);
        } else {
            observable = mSuggestedTrackOperations.getPopMusic(getActivity()).cache().observeOn(ScSchedulers.UI_SCHEDULER);
            savedState.put(KEY_OBSERVABLE, observable);
        }
        loadSuggestedTracks(observable);
    }

    private void loadSuggestedTracks(Observable<?> observable) {
        if (mObserver == null) mObserver = new SuggestedTracksObserver(this);
        mSubscription = observable.subscribe(mObserver);
        setDisplayMode(DisplayMode.LOADING);
    }

    private void setDisplayMode(DisplayMode mode){
        mMode = mode;
        switch (mMode){
            case LOADING:
                mEmptyListView.setStatus(EmptyListView.Status.WAITING);
                mEmptyListView.setVisibility(View.VISIBLE);
                mGridView.setVisibility(View.GONE);
                break;

            case CONTENT:
                mGridView.setVisibility(View.VISIBLE);
                mEmptyListView.setVisibility(View.GONE);
                break;

            case ERROR:
                mEmptyListView.setStatus(EmptyListView.Status.ERROR);
                mEmptyListView.setVisibility(View.VISIBLE);
                mGridView.setVisibility(View.GONE);
                break;
        }
    }

    private static final class SuggestedTracksObserver extends RxFragmentObserver<SuggestedTracksFragment, SuggestedTrack> {

        public SuggestedTracksObserver(SuggestedTracksFragment fragment) {
            super(fragment);
        }

        @Override
        public void onNext(SuggestedTracksFragment fragment, SuggestedTrack suggestedTrack) {
            fragment.mSuggestedTracksAdapter.addSuggestedTrack(suggestedTrack);
            fragment.mSuggestedTracksAdapter.notifyDataSetChanged();
            fragment.setDisplayMode(DisplayMode.CONTENT);
        }

        @Override
        public void onCompleted(SuggestedTracksFragment fragment) {
            Log.d(LOG_TAG, "fragment: onCompleted");
        }

        @Override
        public void onError(SuggestedTracksFragment fragment, Exception error) {
            error.printStackTrace();
            fragment.setDisplayMode(DisplayMode.ERROR);
        }
    }
}
