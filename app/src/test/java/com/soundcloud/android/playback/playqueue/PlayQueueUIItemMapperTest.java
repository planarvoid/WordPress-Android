package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class PlayQueueUIItemMapperTest extends AndroidUnitTest {

    private PlayQueueUIItemMapper mapper;

    @Before
    public void setUp() {
        mapper = new PlayQueueUIItemMapper();
    }

    @Test
    public void getItemIdWhenOneOccurrence() {
        final TrackItem trackItem = getPreviewTrackItem();

        PlayQueueUIItem item = mapper.call(Lists.newArrayList(trackItem)).get(0);

        assertThat(item.getId()).isEqualTo(trackItem.getUrn().getNumericId());
    }

    @Test
    public void getItemIdWhenTwoOccurrence() {
        final TrackItem trackItem = getPreviewTrackItem();

        List<PlayQueueUIItem> items = mapper.call(Lists.newArrayList(trackItem, trackItem));

        assertThat(items.get(0).getId()).isEqualTo(trackItem.getUrn().getNumericId());
        assertThat(items.get(1).getId()).isEqualTo(-1 * trackItem.getUrn().getNumericId());
    }

    @Test
    public void getItemIdWhenThreeOccurrence() {
        final TrackItem trackItem = getPreviewTrackItem();

        List<PlayQueueUIItem> items = mapper.call(Lists.newArrayList(trackItem, trackItem, trackItem));

        assertThat(items.get(0).getId()).isEqualTo(trackItem.getUrn().getNumericId());
        assertThat(items.get(1).getId()).isEqualTo(-1 * trackItem.getUrn().getNumericId());
        assertThat(items.get(2).getId()).isEqualTo(-2 * trackItem.getUrn().getNumericId());
    }

    @Test
    public void mapCorrectly() {
        final TrackItem trackItem = getPreviewTrackItem();
        PlayQueueUIItem item = mapper.call(Lists.newArrayList(trackItem)).get(0);

        assertThat(item.getId()).isEqualTo(trackItem.getUrn().getNumericId());
        assertThat(item.getUrn()).isEqualTo(trackItem.getUrn());
        assertThat(item.getTitle()).isEqualTo(trackItem.getTitle());
        assertThat(item.getCreator()).isEqualTo(trackItem.getCreatorName());
        assertThat(item.isBlocked()).isEqualTo(trackItem.isBlocked());
        assertThat(item.getStatusLableId()).isEqualTo(R.layout.preview);
        PropertySet propertySet = trackItem.getSource();
        Optional<String> templateUrl = propertySet.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE,
                                                             Optional.<String>absent());
        ImageResource imageResource = SimpleImageResource.create(trackItem.getUrn(), templateUrl);
        assertThat(item.getImageResource()).isEqualTo(imageResource);
        assertThat(item.getTrackItem()).isEqualTo(trackItem);
    }

    @Test
    public void mapToPreviewId() {
        final TrackItem trackItem = getPreviewTrackItem();

        PlayQueueUIItem item = mapper.call(Lists.newArrayList(trackItem)).get(0);

        assertThat(item.getStatusLableId()).isEqualTo(R.layout.preview);
    }

    @Test
    public void mapToBlockedId() {
        final TrackItem trackItem = getBlockedTrackItem();

        PlayQueueUIItem item = mapper.call(Lists.newArrayList(trackItem)).get(0);

        assertThat(item.getStatusLableId()).isEqualTo(R.layout.not_available);
    }

    @Test
    public void mapToGoLabelId() {
        final TrackItem trackItem = getGoTrackItem();

        PlayQueueUIItem item = mapper.call(Lists.newArrayList(trackItem)).get(0);

        assertThat(item.getStatusLableId()).isEqualTo(R.layout.go_label);
    }

    @Test
    public void mapToPrivateLabelId() {
        final TrackItem trackItem = getPrivateTrackItem();

        PlayQueueUIItem item = mapper.call(Lists.newArrayList(trackItem)).get(0);

        assertThat(item.getStatusLableId()).isEqualTo(R.layout.private_label);
    }


    private TrackItem getPreviewTrackItem() {
        return TrackItem.from(trackProperties(false, true, true, false));
    }

    private TrackItem getBlockedTrackItem() {
        return TrackItem.from(trackProperties(true, true, true, false));
    }

    private TrackItem getGoTrackItem() {
        return TrackItem.from(trackProperties(false, false, true, false));
    }

    private TrackItem getPrivateTrackItem() {
        return TrackItem.from(trackProperties(false, false, false, true));
    }

    private PropertySet trackProperties(boolean isBlocked,
                                        boolean isSnipped,
                                        boolean isSubHighTier,
                                        boolean isPrivate) {
        return TestPropertySets.fromApiTrack()
                               .put(TrackProperty.BLOCKED, isBlocked)
                               .put(TrackProperty.SNIPPED, isSnipped)
                               .put(TrackProperty.SUB_HIGH_TIER, isSubHighTier)
                               .put(TrackProperty.IS_PRIVATE, isPrivate);
    }
}
