package com.soundcloud.android.fragment;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.soundcloud.android.adapter.SuggestedTracksAdapter;
import com.soundcloud.android.api.SuggestedTracksOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedTracksFragmentTest {

    SuggestedTracksFragment suggestedTracksFragment;

    @Mock
    SuggestedTracksOperations suggestedTracksOperations;
    @Mock
    SuggestedTracksAdapter suggestedTracksAdapter;
    @Mock
    Observable<Track> suggestedTrackObservable;

    @Before
    public void setup() {
        suggestedTracksFragment = new SuggestedTracksFragment(suggestedTracksAdapter, suggestedTracksOperations);

        Robolectric.shadowOf(suggestedTracksFragment).setActivity(new SherlockFragmentActivity());
        Robolectric.shadowOf(suggestedTracksFragment).setAttached(true);
    }

    @Test
    public void testShowsLoadingState() {
        when(suggestedTracksOperations.getSuggestedTracks()).thenReturn(Observable.<Observable<Track>>never());
        suggestedTracksFragment.onCreate(null);

        final EmptyListView emptyView = (EmptyListView) initFragmentView().findViewById(R.id.empty);
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.WAITING);
    }

    @Test
    public void testShowsErrorState() {
        when(suggestedTracksOperations.getSuggestedTracks()).thenReturn(Observable.create(new Func1<Observer<Observable<Track>>, Subscription>() {
            @Override
            public Subscription call(Observer<Observable<Track>> observableObserver) {
                observableObserver.onNext(Observable.<Track>error(new Exception()));
                observableObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));
        suggestedTracksFragment.onCreate(null);

        final EmptyListView emptyView = (EmptyListView) initFragmentView().findViewById(R.id.empty);
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.ERROR);
    }

    @Test
    public void testShowsEmptyState() {
        when(suggestedTracksOperations.getSuggestedTracks()).thenReturn(Observable.create(new Func1<Observer<Observable<Track>>, Subscription>() {
            @Override
            public Subscription call(Observer<Observable<Track>> observableObserver) {
                observableObserver.onNext(Observable.<Track>empty());
                observableObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));
        suggestedTracksFragment.onCreate(null);

        final EmptyListView emptyView = (EmptyListView) initFragmentView().findViewById(R.id.empty);
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.OK);

        verify(suggestedTracksAdapter, never()).addSuggestedTrack(any(Track.class));
    }

    @Test
    public void testShowsContent() {
        final Track suggestedTrack = new Track();
        when(suggestedTracksOperations.getSuggestedTracks()).thenReturn(Observable.create(new Func1<Observer<Observable<Track>>, Subscription>() {
            @Override
            public Subscription call(Observer<Observable<Track>> observableObserver) {
                observableObserver.onNext(Observable.just(suggestedTrack));
                observableObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));
        suggestedTracksFragment.onCreate(null);

        final EmptyListView emptyView = (EmptyListView) initFragmentView().findViewById(R.id.empty);
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.OK);

        verify(suggestedTracksAdapter).addSuggestedTrack(suggestedTrack);
    }


    private View initFragmentView() {
        final View fragmentView = suggestedTracksFragment.onCreateView(LayoutInflater.from(Robolectric.application), mock(ViewGroup.class), null);
        suggestedTracksFragment.onViewCreated(fragmentView, null);
        return fragmentView;
    }
}
