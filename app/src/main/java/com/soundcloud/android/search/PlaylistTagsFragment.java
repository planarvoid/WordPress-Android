package com.soundcloud.android.search;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static com.google.common.base.Preconditions.checkArgument;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.PlaylistTagsCollection;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentSubscriber;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.FlowLayout;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

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

    private CompositeSubscription mSubscription;
    private Observable<PlaylistTagsCollection> mAllTagsObservable;

    private EmptyListView mEmptyView;
    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    private final OnClickListener mTagClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            TagClickListener listener = (TagClickListener) getActivity();
            listener.onTagSelected((String) v.getTag());
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
        mAllTagsObservable = mSearchOperations.getPlaylistTags().observeOn(mainThread()).cache();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_tags_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEmptyView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyView.setVisibility(VISIBLE);
        mEmptyView.setStatus(mEmptyViewStatus);

        mSubscription = new CompositeSubscription();
        mSubscription.add(mAllTagsObservable.subscribe(new TagsSubscriber()));
        mSubscription.add(mSearchOperations.getRecentPlaylistTags().observeOn(mainThread())
                .subscribe(new RecentsSubscriber()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSubscription.unsubscribe();
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyView != null) {
            mEmptyView.setStatus(status);
        }
    }

    private void displayAllTags(PlaylistTagsCollection tags) {
        displayTags(getLayoutInflater(null), getView(), tags.getCollection(), R.id.all_tags);
    }

    private void displayRecentTags(PlaylistTagsCollection tags) {
        displayTags(getLayoutInflater(null), getView(), tags.getCollection(), R.id.recent_tags);
    }

    private void displayTags(LayoutInflater inflater, View layout, List<String> tags, int layoutId) {
        ViewGroup tagFlowLayout = (ViewGroup) layout.findViewById(layoutId);
        tagFlowLayout.removeAllViews();

        int padding = ViewUtils.dpToPx(getActivity(), 5);
        FlowLayout.LayoutParams flowLP = new FlowLayout.LayoutParams(padding, padding);

        for (final String tag : tags) {
            if (!TextUtils.isEmpty(tag)) {
                TextView tagView = ((TextView) inflater.inflate(R.layout.btn_tag, null));
                tagView.setText(tag);
                tagView.setTag(tag);
                tagView.setOnClickListener(mTagClickListener);
                tagFlowLayout.addView(tagView, flowLP);
            }
        }
    }

    private final class TagsSubscriber extends ListFragmentSubscriber<PlaylistTagsCollection> {
        public TagsSubscriber() {
            super(PlaylistTagsFragment.this);
        }

        @Override
        public void onNext(PlaylistTagsCollection tags) {
            displayAllTags(tags);
        }

        @Override
        public void onCompleted() {
            super.onCompleted();
            mEmptyView.setVisibility(GONE);
        }
    }

    private final class RecentsSubscriber extends DefaultSubscriber<PlaylistTagsCollection> {

        @Override
        public void onNext(PlaylistTagsCollection tags) {
            if (!tags.getCollection().isEmpty()) {
                getView().findViewById(R.id.recent_tags_container).setVisibility(VISIBLE);
                displayRecentTags(tags);
            }
        }
    }

}
