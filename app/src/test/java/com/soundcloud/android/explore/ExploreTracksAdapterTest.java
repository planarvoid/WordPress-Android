package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.explore.ExploreTracksAdapter.ItemViewHolder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.injection.MockInjector;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import dagger.Module;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Singleton;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksAdapterTest {

    @Inject
    ExploreTracksAdapter mAdapter;
    @Mock
    ImageOperations imageOperations;

    @Before
    public void setUp() {
        MockInjector.create(new TestModule()).inject(this);
    }

    @Test
    public void shouldCreateItemView() {
        View itemView = mAdapter.createItemView(0, new FrameLayout(Robolectric.application));
        expect(itemView).not.toBeNull();
        expect(itemView.getTag()).not.toBeNull(); // contains the private ViewHolder instance
        expect(itemView.findViewById(R.id.suggested_track_image)).not.toBeNull();
        expect(itemView.findViewById(R.id.username)).not.toBeNull();
        expect(itemView.findViewById(R.id.title)).not.toBeNull();
    }

    @Test
    public void shouldBindItemView() throws CreateModelException {
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);
        mAdapter.addItem(track);

        View itemView = mock(View.class);
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());
        ItemViewHolder viewHolder = createItemViewHolder();
        when(itemView.getTag()).thenReturn(viewHolder);

        mAdapter.bindItemView(0, itemView);

        expect(viewHolder.title.getText()).toEqual(track.getTitle());
        expect(viewHolder.username.getText()).toEqual(track.getUserName());
    }

    @Test
    public void shouldHideGenreIfNoGenreAvailable() throws CreateModelException {
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);
        track.setGenre(null);
        mAdapter.addItem(track);

        View itemView = mock(View.class);
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());
        ExploreTracksAdapter.ItemViewHolder viewHolder = createItemViewHolder();
        when(itemView.getTag()).thenReturn(viewHolder);

        mAdapter.bindItemView(0, itemView);

        expect(viewHolder.genre.getVisibility()).toEqual(View.GONE);
    }

    private ItemViewHolder createItemViewHolder() {
        ItemViewHolder viewHolder = mock(ItemViewHolder.class);
        viewHolder.imageView = new ImageView(Robolectric.application);
        viewHolder.title = new TextView(Robolectric.application);
        viewHolder.username = new TextView(Robolectric.application);
        viewHolder.genre = new TextView(Robolectric.application);
        viewHolder.playcount = new TextView(Robolectric.application);
        return viewHolder;
    }

    @Module(complete = false, includes = ExploreModule.class, injects = ExploreTracksAdapterTest.class, overrides = true)
    public class TestModule {
        @Singleton
        @Provides
        public ImageOperations provideImageOperations() {
            return imageOperations;
        }
    }
}
