package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TrackGridRenderer.ItemViewHolder;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

public class TrackGridRendererTest extends AndroidUnitTest {

    private TrackGridRenderer renderer;

    @Mock private ImageOperations imageOperations;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());

    @Before
    public void setUp() throws Exception {
        renderer = new TrackGridRenderer(imageOperations, numberFormatter);
    }

    @Test
    public void shouldCreateItemView() {
        View itemView = renderer.createItemView(new FrameLayout(context()));
        assertThat(itemView).isNotNull();
        assertThat(itemView.getTag()).isNotNull(); // contains the private ViewHolder instance
        assertThat(itemView.findViewById(R.id.image)).isNotNull();
        assertThat(itemView.findViewById(R.id.username)).isNotNull();
        assertThat(itemView.findViewById(R.id.title)).isNotNull();
    }

    @Test
    public void shouldBindItemView() throws CreateModelException {
        TrackItem trackItem = TrackItem.from(ModelFixtures.create(ApiTrack.class));

        View itemView = mock(View.class);
        when(itemView.getResources()).thenReturn(resources());
        ItemViewHolder viewHolder = createItemViewHolder();
        when(itemView.getTag()).thenReturn(viewHolder);

        renderer.bindItemView(0, itemView, singletonList(trackItem));

        assertThat(viewHolder.title.getText()).isEqualTo(trackItem.getTitle());
        assertThat(viewHolder.username.getText()).isEqualTo(trackItem.getCreatorName());
    }

    @Test
    public void shouldHideGenreIfNoGenreAvailable() throws CreateModelException {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setGenre(null);
        TrackItem trackItem = TrackItem.from(apiTrack);

        View itemView = mock(View.class);
        when(itemView.getResources()).thenReturn(resources());
        TrackGridRenderer.ItemViewHolder viewHolder = createItemViewHolder();
        when(itemView.getTag()).thenReturn(viewHolder);

        renderer.bindItemView(0, itemView, singletonList(trackItem));

        assertThat(viewHolder.genre.getVisibility()).isEqualTo(View.GONE);
    }

    private ItemViewHolder createItemViewHolder() {
        ItemViewHolder viewHolder = mock(ItemViewHolder.class);
        viewHolder.imageView = new ImageView(context());
        viewHolder.title = new TextView(context());
        viewHolder.username = new TextView(context());
        viewHolder.genre = new TextView(context());
        viewHolder.playcount = new TextView(context());
        return viewHolder;
    }
}
