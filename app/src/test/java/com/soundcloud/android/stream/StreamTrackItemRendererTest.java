package com.soundcloud.android.stream;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Date;
import java.util.Locale;

public class StreamTrackItemRendererTest extends AndroidUnitTest {

    @Mock private ImageOperations imageOperations;
    @Mock private HeaderSpannableBuilder spannableBuilder;
    @Mock private StreamItemViewHolder viewHolder;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());
    private StreamTrackItemRenderer renderer;
    private View itemView;

    @Before
    public void setUp() {
        final LayoutInflater layoutInflater = LayoutInflater.from(context());
        itemView = layoutInflater.inflate(R.layout.stream_track_item, new FrameLayout(context()), false);
        itemView.setTag(viewHolder);

        renderer = new StreamTrackItemRenderer(
                imageOperations, numberFormatter, null, resources(), spannableBuilder);
    }

    @Test
    public void bindsHeaderAvatarForTrackWithReposter() {
        TrackItem repostedTrack = repostedTrack();
        renderer.bindItemView(0, itemView, singletonList(repostedTrack));

        verify(imageOperations)
                .displayInAdapterView(
                        eq(repostedTrack.getReposterUrn()), any(ApiImageSize.class),
                        any(ImageView.class));
    }

    @Test
    public void buildsRepostedHeaderStringForTrackWithReposter() {
        TrackItem repostedTrack = repostedTrack();
        renderer.bindItemView(0, itemView, singletonList(repostedTrack));

        InOrder inOrder = Mockito.inOrder(spannableBuilder);
        inOrder.verify(spannableBuilder).trackUserAction(repostedTrack.getReposter().get(), repostedString());
        inOrder.verify(spannableBuilder).withIconSpan(viewHolder);
        inOrder.verify(spannableBuilder).get();
    }

    @Test
    public void bindsHeaderViewPropertiesToViewHolder() {
        TrackItem repostedTrack = repostedTrack();
        renderer.bindItemView(0, itemView, singletonList(repostedTrack));

        verify(viewHolder).setCreatedAt(formattedDate(repostedTrack.getCreatedAt()));
        verify(viewHolder).togglePrivateIndicator(repostedTrack.isPrivate());
    }

    @Test
    public void bindsHeaderAvatarForPostedTrack() {
        TrackItem postedTrack = postedTrack();
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(imageOperations)
                .displayInAdapterView(
                        eq(postedTrack.getCreatorUrn()),
                        any(ApiImageSize.class),
                        any(ImageView.class));
    }

    @Test
    public void buildsPostedHeaderStringForPostedTrack() {
        TrackItem postedTrack = postedTrack();
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        InOrder inOrder = Mockito.inOrder(spannableBuilder);
        inOrder.verify(spannableBuilder).trackUserAction(postedTrack.getCreatorName(), "posted");
        inOrder.verify(spannableBuilder).get();
    }

    @Test
    public void bindsArtworkView() {
        TrackItem postedTrack = postedTrack();
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(imageOperations)
                .displayInAdapterView(
                        eq(postedTrack.getEntityUrn()),
                        any(ApiImageSize.class),
                        any(ImageView.class));

        verify(viewHolder).setTitle(postedTrack.getTitle());
        verify(viewHolder).setCreator(postedTrack.getCreatorName());
    }

    @Test
    public void resetsEngagementsBar() {
        TrackItem postedTrack = postedTrack();
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(viewHolder).resetAdditionalInformation();
    }

    @Test
    public void bindsEngagementsBar() {
        TrackItem postedTrack = postedTrack();
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(viewHolder).showLikeStats(formattedStats(postedTrack.getLikesCount()), postedTrack.isLiked());
        verify(viewHolder).showRepostStats(formattedStats(postedTrack.getRepostCount()), postedTrack.isReposted());
        verify(viewHolder).showPlayCount(formattedStats(postedTrack.getPlayCount()));
    }

    @Test
    public void bindsNowPlaying() {
        TrackItem postedTrack = postedTrack();
        postedTrack.setIsPlaying(true);
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(viewHolder).showNowPlaying();
    }

    private TrackItem repostedTrack() {
        final PropertySet track = ModelFixtures.create(ApiTrack.class).toPropertySet();
        track.put(PostProperty.REPOSTER, "reposter");
        track.put(PostProperty.REPOSTER_URN, Urn.forUser(123L));

        return TrackItem.from(track);
    }

    private String repostedString() {
        return resources().getString(R.string.stream_reposted_action);
    }

    private TrackItem postedTrack() {
        return TrackItem.from(ModelFixtures.create(ApiTrack.class));
    }

    private String formattedDate(Date createdAt) {
        return ScTextUtils.formatTimeElapsedSince(resources(), createdAt.getTime(), true);
    }

    private String formattedStats(int stat) {
        return numberFormatter.format(stat);
    }
}
