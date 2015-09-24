package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.tracks.TrackGridRenderer.ItemViewHolder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Locale;

@RunWith(SoundCloudTestRunner.class)
public class TrackGridRendererTest {

    private TrackGridRenderer renderer;

    @Mock private ImageOperations imageOperations;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, Robolectric.application.getResources());

    @Before
    public void setUp() throws Exception {
        renderer = new TrackGridRenderer(imageOperations, numberFormatter);
    }

    @Test
    public void shouldCreateItemView() {
        View itemView = renderer.createItemView(new FrameLayout(Robolectric.application));
        expect(itemView).not.toBeNull();
        expect(itemView.getTag()).not.toBeNull(); // contains the private ViewHolder instance
        expect(itemView.findViewById(R.id.image)).not.toBeNull();
        expect(itemView.findViewById(R.id.username)).not.toBeNull();
        expect(itemView.findViewById(R.id.title)).not.toBeNull();
    }

    @Test
    public void shouldBindItemView() throws CreateModelException {
        TrackItem trackItem = TrackItem.from(ModelFixtures.create(ApiTrack.class));

        View itemView = mock(View.class);
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());
        ItemViewHolder viewHolder = createItemViewHolder();
        when(itemView.getTag()).thenReturn(viewHolder);

        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(viewHolder.title.getText()).toEqual(trackItem.getTitle());
        expect(viewHolder.username.getText()).toEqual(trackItem.getCreatorName());
    }

    @Test
    public void shouldHideGenreIfNoGenreAvailable() throws CreateModelException {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setGenre(null);
        TrackItem trackItem = TrackItem.from(apiTrack);

        View itemView = mock(View.class);
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());
        TrackGridRenderer.ItemViewHolder viewHolder = createItemViewHolder();
        when(itemView.getTag()).thenReturn(viewHolder);

        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

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
}
