package com.soundcloud.android.fragment;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.adapter.SoundAssociationAdapter;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.rx.schedulers.ReactiveScheduler;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;

import android.net.Uri;
import android.os.Bundle;

import java.util.List;

public class SoundsFragment extends ReactiveListFragment<ScResource> {

    public static final String EXTRA_USER_STREAM_URI = "user_stream_uri";

    public static SoundsFragment create(final Uri contentUri) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_USER_STREAM_URI, contentUri);

        SoundsFragment fragment = new SoundsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private Uri mContentUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentUri = (Uri) getArguments().get(EXTRA_USER_STREAM_URI);
    }

    @Override
    protected IScAdapter<ScResource> newAdapter() {
        IScAdapter<? extends ScResource> adapter = new SoundAssociationAdapter(getActivity(), mContentUri);
        return  null;
    }

    @Override
    protected void configureEmptyListView(EmptyListView emptyView) {
    }

    @Override
    protected ReactiveScheduler<List<ScResource>> getListItemsScheduler() {
        return null;
    }

    @Override
    protected Observable<List<ScResource>> getListItemsObservable() {
        return null;
    }
}
