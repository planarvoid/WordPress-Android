package com.soundcloud.android.profile;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Locale;

public class PostedPlaylistItemRendererTest extends AndroidUnitTest {

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());

    private View itemView;
    private PlaylistItem playlistItem;

    private PostedPlaylistItemRenderer renderer;

    @Before
    public void setUp() throws Exception {
        final Playlist playlist = ModelFixtures.playlistBuilder()
                                               .urn(Urn.forPlaylist(123))
                                               .title("title")
                                               .creatorName("creator")
                                               .likesCount(5)
                                               .trackCount(11)
                                               .build();
        playlistItem = PlaylistItem.builder(playlist).isUserLike(false).build();

        final LayoutInflater layoutInflater = LayoutInflater.from(context());
        itemView = layoutInflater.inflate(R.layout.playlist_list_item, new FrameLayout(context()), false);
        renderer = new PostedPlaylistItemRenderer(resources(),
                                                  mock(ImageOperations.class), numberFormatter,
                                                  mock(PlaylistItemMenuPresenter.class), mock(EventBus.class),
                                                  mock(ScreenProvider.class), mock(Navigator.class));
    }

    @Test
    public void shouldBindReposterIfAny() {
        final String reposter = "reposter";
        PlaylistItem repostedPlaylist = playlistItem.updateWithReposter(reposter, Urn.NOT_SET);
        renderer.bindItemView(0, itemView, singletonList(repostedPlaylist));

        assertThat(textView(R.id.reposter).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(textView(R.id.reposter).getText()).isEqualTo(reposter);
    }

    @Test
    public void shouldNotBindReposterIfNone() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.reposter).getVisibility()).isEqualTo(View.GONE);
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}
