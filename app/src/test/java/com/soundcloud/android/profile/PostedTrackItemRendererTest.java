package com.soundcloud.android.profile;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemView;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.widget.ImageView;

import java.util.Locale;

public class PostedTrackItemRendererTest extends AndroidUnitTest {

    @Mock TrackItemView trackItemView;
    @Mock ImageView imageView;
    @Mock View itemView;

    private TrackItem trackItem;
    private PropertySet propertySet;
    private PostedTrackItemRenderer renderer;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(
                TrackProperty.TITLE.bind("title"),
                TrackProperty.CREATOR_NAME.bind("creator"),
                TrackProperty.SNIPPET_DURATION.bind(227000L),
                TrackProperty.FULL_DURATION.bind(227000L),
                TrackProperty.SNIPPED.bind(true),
                TrackProperty.URN.bind(Urn.forTrack(123)),
                TrackProperty.PLAY_COUNT.bind(870)
        );
        trackItem = TestPropertySets.trackWith(propertySet);

        when(trackItemView.getImage()).thenReturn(imageView);
        when(trackItemView.getResources()).thenReturn(resources());
        when(imageView.getContext()).thenReturn(context());
        when(itemView.getContext()).thenReturn(context());
        when(itemView.getTag()).thenReturn(trackItemView);

        renderer = new PostedTrackItemRenderer(mock(ImageOperations.class),
                                               numberFormatter,
                                               null,
                                               mock(EventBus.class),
                                               mock(ScreenProvider.class),
                                               mock(Navigator.class),
                                               mock(FeatureOperations.class),
                                               mock(TrackItemView.Factory.class));
    }

    @Test
    public void shouldBindReposterIfAny() {
        propertySet.put(PostProperty.REPOSTER, "reposter");
        trackItem = TestPropertySets.trackWith(propertySet);
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showReposter("reposter");
    }

    @Test
    public void shouldNotBindReposterIfNone() {
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).hideReposter();
    }
}
