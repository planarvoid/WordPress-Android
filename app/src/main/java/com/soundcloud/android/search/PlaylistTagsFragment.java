package com.soundcloud.android.search;

import static com.google.common.base.Preconditions.checkArgument;
import static rx.android.observables.AndroidObservable.fromFragment;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.PlaylistTagsCollection;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.FlowLayout;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

@SuppressLint("ValidFragment")
public class PlaylistTagsFragment extends Fragment implements EmptyViewAware {

    public static final String TAG = "playlist_tags";

    @Inject
    SearchOperations mSearchOperations;

    private Observable<PlaylistTagsCollection> mObservable;
    private Subscription mSubscription = Subscriptions.empty();
    private EmptyListView mEmptyView;
    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    private final View.OnClickListener mTagClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TagClickListener listener = (TagClickListener) getActivity();
            listener.onTagSelected(((TextView) v).getText().toString());
        }
    };

    public interface TagClickListener {
        void onTagSelected(String tag);
    }

    public PlaylistTagsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @VisibleForTesting
    PlaylistTagsFragment(SearchOperations searchOperations) {
        mSearchOperations = searchOperations;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        checkArgument(activity instanceof TagClickListener, "Host activity must be a " + TagClickListener.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mObservable = fromFragment(this, mSearchOperations.getPlaylistTags()).cache();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_tags_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEmptyView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyView.setVisibility(View.VISIBLE);
        mEmptyView.setStatus(mEmptyViewStatus);

        mSubscription = mObservable.subscribe(new TagsObserver());
    }

    @Override
    public void onDestroy() {
        mSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyView != null) {
            mEmptyView.setStatus(status);
        }
    }

    private void displayTags(LayoutInflater inflater, View layout, List<String> tags) {
        ViewGroup tagFlowLayout = (ViewGroup) layout.findViewById(R.id.tags);
        tagFlowLayout.removeAllViews();

        int padding = ViewUtils.dpToPx(getActivity(), 10);
        FlowLayout.LayoutParams flowLP = new FlowLayout.LayoutParams(padding, padding);

        tagFlowLayout.setVisibility(tags.isEmpty() ? View.GONE : View.VISIBLE);
        for (final String tag : tags) {
            if (!TextUtils.isEmpty(tag)) {
                TextView tagView = ((TextView) inflater.inflate(R.layout.tag_text, null));
                tagView.setText(tag);
                tagView.setOnClickListener(mTagClickListener);
                tagFlowLayout.addView(tagView, flowLP);
            }
        }
    }

    private final class TagsObserver extends ListFragmentObserver<PlaylistTagsCollection> {
        public TagsObserver() {
            super(PlaylistTagsFragment.this);
        }

        @Override
        public void onNext(PlaylistTagsCollection tags) {
            displayTags(getLayoutInflater(null), getView(), tags.getCollection());
        }

        @Override
        public void onCompleted() {
            super.onCompleted();
            mEmptyView.setVisibility(View.GONE);
        }
    }
}
