package com.soundcloud.android.search;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static com.google.common.base.Preconditions.checkArgument;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.PlaylistTagsCollection;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentSubscriber;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.FlowLayout;
import com.soundcloud.android.view.ListenableScrollView;
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
public class PlaylistTagsFragment extends Fragment implements EmptyViewAware, ListenableScrollView.OnScrollListener {

    public static final String TAG = "playlist_tags";

    @Inject
    SearchOperations searchOperations;

    @Inject
    EventBus eventBus;

    private CompositeSubscription subscription;
    private Observable<PlaylistTagsCollection> allTagsObservable;
    private Observable<PlaylistTagsCollection> recentTagsObservable;

    private EmptyListView emptyView;
    private int emptyViewStatus = EmptyListView.Status.WAITING;

    private final OnClickListener recentTagClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            eventBus.publish(EventQueue.SEARCH, SearchEvent.recentTagSearch((String) v.getTag()));
            selectTag(v);
        }
    };

    private final OnClickListener popularTagClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            eventBus.publish(EventQueue.SEARCH, SearchEvent.popularTagSearch((String) v.getTag()));
            selectTag(v);
        }
    };

    public interface TagEventsListener {
        void onTagSelected(String tag);
        void onTagsScrolled();
    }

    public PlaylistTagsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @VisibleForTesting
    PlaylistTagsFragment(SearchOperations searchOperations, EventBus eventBus) {
        this.searchOperations = searchOperations;
        this.eventBus = eventBus;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        checkArgument(activity instanceof TagEventsListener, "Host activity must be a " + TagEventsListener.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        allTagsObservable = searchOperations.getPlaylistTags().observeOn(mainThread()).cache();
        recentTagsObservable = searchOperations.getRecentPlaylistTags().observeOn(mainThread());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_tags_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emptyView = (EmptyListView) view.findViewById(android.R.id.empty);
        emptyView.setVisibility(VISIBLE);
        emptyView.setStatus(emptyViewStatus);

        ListenableScrollView scrollView = (ListenableScrollView) view.findViewById(R.id.playlist_tags_scroll_container);
        scrollView.setOnScrollListener(this);

        subscription = new CompositeSubscription();
        subscription.add(allTagsObservable.subscribe(new TagsSubscriber()));
        subscription.add(recentTagsObservable.subscribe(new RecentsSubscriber()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        subscription.unsubscribe();
    }

    @Override
    public void setEmptyViewStatus(int status) {
        emptyViewStatus = status;
        if (emptyView != null) {
            emptyView.setStatus(status);
        }
    }

    @Override
    public void onScroll(int top, int oldTop) {
        ((TagEventsListener) getActivity()).onTagsScrolled();
    }

    private void displayPopularTags(PlaylistTagsCollection tags) {
        displayTags(getLayoutInflater(null), getView(), tags.getCollection(), R.id.all_tags, popularTagClickListener);
    }

    private void displayRecentTags(PlaylistTagsCollection tags) {
        displayTags(getLayoutInflater(null), getView(), tags.getCollection(), R.id.recent_tags, recentTagClickListener);
    }

    private void displayTags(LayoutInflater inflater, View layout, List<String> tags,
                             int layoutId, OnClickListener tagClickListener) {
        ViewGroup tagFlowLayout = (ViewGroup) layout.findViewById(layoutId);
        tagFlowLayout.removeAllViews();

        int padding = ViewUtils.dpToPx(getActivity(), 5);
        FlowLayout.LayoutParams flowLP = new FlowLayout.LayoutParams(padding, padding);

        for (final String tag : tags) {
            if (!TextUtils.isEmpty(tag)) {
                TextView tagView = ((TextView) inflater.inflate(R.layout.btn_tag, null));
                tagView.setText("#" + tag);
                tagView.setTag(tag);
                tagView.setOnClickListener(tagClickListener);
                tagFlowLayout.addView(tagView, flowLP);
            }
        }
    }

    private void selectTag(View v) {
        TagEventsListener listener = (TagEventsListener) getActivity();
        listener.onTagSelected((String) v.getTag());
    }

    private final class TagsSubscriber extends ListFragmentSubscriber<PlaylistTagsCollection> {
        public TagsSubscriber() {
            super(PlaylistTagsFragment.this);
        }

        @Override
        public void onNext(PlaylistTagsCollection tags) {
            displayPopularTags(tags);
        }

        @Override
        public void onCompleted() {
            super.onCompleted();
            emptyView.setVisibility(GONE);
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
