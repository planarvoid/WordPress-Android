package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksAdapterTest {

    private ExploreTracksAdapter adapter;

    @Test
    public void shouldCreateItemView() {
        adapter = new ExploreTracksAdapter(mock(Observable.class), mock(ListFragmentObserver.class));
        View itemView = adapter.createItemView(0, new FrameLayout(Robolectric.application));
        expect(itemView).not.toBeNull();
        expect(itemView.getTag()).not.toBeNull(); // contains the private ViewHolder instance
        expect(itemView.findViewById(R.id.suggested_track_image)).not.toBeNull();
        expect(itemView.findViewById(R.id.username)).not.toBeNull();
        expect(itemView.findViewById(R.id.title)).not.toBeNull();
    }

    @Test
    public void shouldBindItemView() throws CreateModelException {
        adapter = new ExploreTracksAdapter(mock(Observable.class), mock(ListFragmentObserver.class));
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);
        adapter.addItem(track);

        View itemView = mock(View.class);
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());
        ExploreTracksAdapter.ItemViewHolder viewHolder = createItemViewHolder();
        when(itemView.getTag()).thenReturn(viewHolder);

        adapter.bindItemView(0, itemView);

        expect(viewHolder.title.getText()).toEqual(track.getTitle());
        expect(viewHolder.username.getText()).toEqual(track.getUserName());
    }

    @Test
    public void shouldHideGenreIfNoGenreAvailable() throws CreateModelException {
        adapter = new ExploreTracksAdapter(mock(Observable.class), mock(ListFragmentObserver.class));
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);
        track.setGenre(null);
        adapter.addItem(track);

        View itemView = mock(View.class);
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());
        ExploreTracksAdapter.ItemViewHolder viewHolder = createItemViewHolder();
        when(itemView.getTag()).thenReturn(viewHolder);

        adapter.bindItemView(0, itemView);

        expect(viewHolder.genre.getVisibility()).toEqual(View.GONE);
    }

    private ExploreTracksAdapter.ItemViewHolder createItemViewHolder() {
        ExploreTracksAdapter.ItemViewHolder viewHolder = mock(ExploreTracksAdapter.ItemViewHolder.class);
        viewHolder.imageView = new ImageView(Robolectric.application);
        viewHolder.title = new TextView(Robolectric.application);
        viewHolder.username = new TextView(Robolectric.application);
        viewHolder.genre = new TextView(Robolectric.application);
        viewHolder.playcount = new TextView(Robolectric.application);
        return viewHolder;
    }
}
