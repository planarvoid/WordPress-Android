package com.soundcloud.android.stream;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
import java.util.concurrent.TimeUnit;

public class StreamPlaylistItemRendererTest extends AndroidUnitTest {

    @Mock private ImageOperations imageOperations;
    @Mock private PlaylistItemMenuPresenter menuPresenter;
    @Mock private HeaderSpannableBuilder spannableBuilder;
    @Mock private StreamPlaylistItemRenderer.StreamPlaylistViewHolder viewHolder;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());
    private StreamPlaylistItemRenderer renderer;
    private View itemView;

    @Before
    public void setUp() {

        final LayoutInflater layoutInflater = LayoutInflater.from(context());
        itemView = layoutInflater.inflate(R.layout.stream_playlist_item, new FrameLayout(context()), false);
        itemView.setTag(viewHolder);


        renderer = new StreamPlaylistItemRenderer(
                imageOperations, numberFormatter, menuPresenter, resources(), spannableBuilder);
    }

    @Test
    public void bindsHeaderAvatarForPlaylistWithReposter() {
        PlaylistItem playlistItem = repostedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        verify(imageOperations)
                .displayInAdapterView(
                        eq(playlistItem.getReposterUrn()), any(ApiImageSize.class),
                        any(ImageView.class));
    }

    @Test
    public void buildsRepostedHeaderStringForPlaylistWithReposter() {
        PlaylistItem playlistItem = repostedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        InOrder inOrder = Mockito.inOrder(spannableBuilder);
        inOrder.verify(spannableBuilder).playlistUserAction("reposter", "reposted");
        inOrder.verify(spannableBuilder).withIconSpan(viewHolder);
        inOrder.verify(spannableBuilder).get();
    }

    @Test
    public void bindsHeaderViewPropertiesToViewHolder() {
        PlaylistItem playlistItem = repostedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        verify(viewHolder).setCreatedAt(formattedDate(playlistItem.getCreatedAt()));
        verify(viewHolder).togglePrivateIndicator(playlistItem.isPrivate());
    }

    @Test
    public void bindsHeaderAvatarForPostedPlaylist() {
        PlaylistItem playlistItem = postedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        verify(imageOperations)
                .displayInAdapterView(
                        eq(playlistItem.getCreatorUrn()),
                        any(ApiImageSize.class),
                        any(ImageView.class));
    }

    @Test
    public void buildsPostedHeaderStringForPostedPlaylist() {
        PlaylistItem playlistItem = postedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        InOrder inOrder = Mockito.inOrder(spannableBuilder);
        inOrder.verify(spannableBuilder).playlistUserAction(playlistItem.getCreatorName(), "posted");
        inOrder.verify(spannableBuilder).get();
    }

    @Test
    public void bindsArtworkView() {
        PlaylistItem playlistItem = postedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        verify(imageOperations)
                .displayInAdapterView(
                        eq(playlistItem.getEntityUrn()),
                        any(ApiImageSize.class),
                        any(ImageView.class));

        verify(viewHolder).setTitle(playlistItem.getTitle());
        verify(viewHolder).setCreator(playlistItem.getCreatorName());
        verify(viewHolder).setTrackCount(
                String.valueOf(playlistItem.getTrackCount()),
                tracksString(playlistItem.getTrackCount()));
    }

    @Test
    public void resetsEngagementsBar() {
        PlaylistItem playlistItem = postedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        verify(viewHolder).resetAdditionalInformation();
    }

    @Test
    public void bindsEngagementsBar() {
        PlaylistItem playlistItem = postedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        verify(viewHolder).showDuration(formattedTime(playlistItem.getDuration()));
        verify(viewHolder).showLikeStats(formattedStats(playlistItem.getLikesCount()));
        verify(viewHolder).showRepostStats(formattedStats(playlistItem.getRepostCount()));
    }

    private PlaylistItem repostedPlaylist() {
        final PropertySet playlist = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        playlist.put(PlayableProperty.REPOSTER, "reposter");
        playlist.put(PlayableProperty.REPOSTER_URN, Urn.forUser(123L));

        return PlaylistItem.from(playlist);
    }

    private PlaylistItem postedPlaylist() {
        return PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
    }

    private String formattedDate(Date createdAt) {
        return ScTextUtils.formatTimeElapsedSince(resources(), createdAt.getTime(), true);
    }

    private String tracksString(int trackCount) {
        return resources().getQuantityString(R.plurals.number_of_tracks, trackCount);
    }

    private String formattedTime(long time) {
        return ScTextUtils.formatTimestamp(time, TimeUnit.MILLISECONDS);
    }

    private String formattedStats(int stat) {
        return numberFormatter.format(stat);
    }
}
