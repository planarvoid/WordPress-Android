package com.soundcloud.android.search;

import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static com.google.common.base.Preconditions.checkArgument;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyViewController;
import com.soundcloud.android.view.FlowLayout;
import com.soundcloud.android.view.ListenableScrollView;
import com.soundcloud.android.view.ReactiveComponent;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PlaylistTagsFragment extends LightCycleSupportFragment implements ListenableScrollView.OnScrollListener,
        ReactiveComponent<ConnectableObservable<List<String>>> {

    public static final String TAG = "playlist_tags";

    @Inject PlaylistDiscoveryOperations operations;
    @Inject EventBus eventBus;
    @Inject @LightCycle EmptyViewController emptyViewController;

    private Subscription connectionSubscription = Subscriptions.empty();
    private CompositeSubscription viewSubscriptions;
    private ConnectableObservable<List<String>> allTagsObservable;
    private Observable<List<String>> recentTagsObservable;

    private final OnClickListener recentTagClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.recentTagSearch((String) v.getTag()));
            selectTag(v);
        }
    };

    private final OnClickListener popularTagClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.popularTagSearch((String) v.getTag()));
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        checkArgument(activity instanceof TagEventsListener, "Host activity must be a " + TagEventsListener.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recentTagsObservable = operations.recentPlaylistTags().observeOn(mainThread());
        connectObservable(buildAllTagsObservable());
    }

    @Override
    public ConnectableObservable<List<String>> buildObservable() {
        ConnectableObservable<List<String>> observable = buildAllTagsObservable();
        viewSubscriptions.add(observable.subscribe(new TagsSubscriber()));
        return observable;
    }

    private ConnectableObservable<List<String>> buildAllTagsObservable() {
        return operations.popularPlaylistTags().observeOn(mainThread()).replay();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<String>> observable) {
        allTagsObservable = observable;
        connectionSubscription = allTagsObservable.connect();
        return connectionSubscription;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_tags_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emptyViewController.connect(this, allTagsObservable);

        ListenableScrollView scrollView = (ListenableScrollView) view.findViewById(R.id.playlist_tags_scroll_container);
        scrollView.setOnScrollListener(this);

        viewSubscriptions = new CompositeSubscription();
        viewSubscriptions.add(allTagsObservable.subscribe(new TagsSubscriber()));
        viewSubscriptions.add(recentTagsObservable.subscribe(new RecentsSubscriber()));
    }

    @Override
    public void onDestroyView() {
        viewSubscriptions.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onScroll(int top, int oldTop) {
        ((TagEventsListener) getActivity()).onTagsScrolled();
    }

    private void displayPopularTags(List<String> tags) {
        displayTags(getLayoutInflater(null), getView(), tags, R.id.all_tags, popularTagClickListener);
    }

    private void displayRecentTags(List<String> tags) {
        displayTags(getLayoutInflater(null), getView(), tags, R.id.recent_tags, recentTagClickListener);
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

    private final class TagsSubscriber extends DefaultSubscriber<List<String>> {

        @Override
        public void onNext(List<String> tags) {
            displayPopularTags(tags);
        }

    }

    private final class RecentsSubscriber extends DefaultSubscriber<List<String>> {

        @Override
        public void onNext(List<String> tags) {
            if (!tags.isEmpty()) {
                getView().findViewById(R.id.recent_tags_container).setVisibility(VISIBLE);
                displayRecentTags(tags);
            }
        }
    }

}
