package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
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
import android.widget.Toast;

public class SuggestedTracksFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    private static final String LOG_TAG         = "suggested_tracks_frag";

    private Observable<Observable<SuggestedTrack>> mObservable;
    private Observable<SuggestedTrack> mNextPageObservable;

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
        loadNextPage();
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

    private void loadNextPage() {
        mNextPageObservable.observeOn(ScSchedulers.UI_SCHEDULER).subscribe(new SuggestedTrackObserver(this));
    }

    private void loadSuggestedTracks() {
        mEmptyListView.setStatus(EmptyListView.Status.WAITING);
        mSubscription = mObservable.subscribe(new GetNextPageObserver(this));
    }

    private static final class GetNextPageObserver extends ScFragmentObserver<SuggestedTracksFragment, Observable<SuggestedTrack>> {

        public GetNextPageObserver(SuggestedTracksFragment fragment) {
            super(fragment);
        }

        @Override
        public void onNext(final SuggestedTracksFragment fragment, Observable<SuggestedTrack> nextPageObservable) {
            Toast.makeText(SoundCloudApplication.instance, "More pages to go... ", Toast.LENGTH_SHORT).show();
            boolean firstPage = (fragment.mNextPageObservable == null);
            fragment.mNextPageObservable = nextPageObservable;
            if (firstPage){
                fragment.loadNextPage();
            }
        }

        @Override
        public void onCompleted() {
            super.onCompleted();
            Toast.makeText(SoundCloudApplication.instance, "Done Loading ", Toast.LENGTH_SHORT).show();
        }
    }

    private static final class SuggestedTrackObserver extends ScFragmentObserver<SuggestedTracksFragment, SuggestedTrack> {

        public SuggestedTrackObserver(SuggestedTracksFragment fragment) {
            super(fragment);
        }

        @Override
        public void onCompleted(SuggestedTracksFragment fragment) {
            fragment.mEmptyListView.setStatus(EmptyListView.Status.OK);
            fragment.mSuggestedTracksAdapter.notifyDataSetChanged();
            Log.d(LOG_TAG, "fragment: onCompleted");
        }

        @Override
        public void onError(SuggestedTracksFragment fragment, Exception error) {
            fragment.mEmptyListView.setStatus(EmptyListView.Status.ERROR);
        }

        @Override
        public void onNext(SuggestedTracksFragment fragment, SuggestedTrack suggestedTrack) {
            fragment.mSuggestedTracksAdapter.addSuggestedTrack(suggestedTrack);
        }
    }
}
