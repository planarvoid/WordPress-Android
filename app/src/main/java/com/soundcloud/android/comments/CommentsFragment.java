package com.soundcloud.android.comments;

import static com.soundcloud.android.comments.CommentsOperations.TO_COMMENT_VIEW_MODEL;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.ReactiveListComponent;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import java.util.List;

public class CommentsFragment extends LightCycleSupportFragment implements ReactiveListComponent<Observable<List<Comment>>> {

    static final String EXTRA_TRACK_URN = "track_urn";

    @Inject CommentsOperations operations;
    @Inject PagingItemAdapter<Comment> adapter;
    @Inject Navigator navigator;
    @Inject @LightCycle ListViewController listViewController;

    private ConnectableObservable<List<Comment>> comments;
    private Subscription subscription = Subscriptions.empty();

    public static CommentsFragment create(Urn trackUrn) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_TRACK_URN, trackUrn);
        CommentsFragment fragment = new CommentsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public CommentsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponents();
    }

    private void addLifeCycleComponents() {
        listViewController.setAdapter(adapter, operations.pager(), TO_COMMENT_VIEW_MODEL);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        navigator.openProfile(getActivity(), adapter.getItem(position).getUserUrn());
    }

    @Override
    public Observable<List<Comment>> buildObservable() {
        final Urn trackUrn = getArguments().getParcelable(EXTRA_TRACK_URN);
        comments = operations.pager().page(operations.comments(trackUrn))
                .map(TO_COMMENT_VIEW_MODEL)
                .observeOn(mainThread())
                .replay(1);
        comments.subscribe(adapter);
        return comments;
    }

    @Override
    public Subscription connectObservable(Observable<List<Comment>> observable) {
        subscription = comments.connect();
        return subscription;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectObservable(buildObservable());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.comments_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listViewController.connect(this, comments);
    }

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }
}
