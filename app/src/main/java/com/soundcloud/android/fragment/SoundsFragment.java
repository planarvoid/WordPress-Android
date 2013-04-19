package com.soundcloud.android.fragment;

import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.adapter.SoundAssociationAdapter;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;

import android.net.Uri;
import android.os.Bundle;

public class SoundsFragment extends ReactiveListFragment<SoundAssociation> {

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
    protected ScBaseAdapter<SoundAssociation> newAdapter() {
        return new SoundAssociationAdapter(getActivity(), mContentUri);
    }

    @Override
    protected void configureEmptyListView(EmptyListView emptyView) {
    }

}
