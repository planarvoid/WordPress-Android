package com.soundcloud.android.screens.elements;

import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.AddToPlaylistScreen;

public class TrackItemMenuElement extends PopupMenuElement {

    private static final int PLAY_RELATED_IDX = 2;

    public TrackItemMenuElement(Han solo) {
        super(solo);
    }

    public void toggleLike() {
        likeItem().click();
    }

    public AddToPlaylistScreen clickAddToPlaylist() {
        clickItemWithText(testDriver.getString(R.string.add_to_playlist));
        return new AddToPlaylistScreen(testDriver);
    }

    public VisualPlayerElement clickPlayRelatedTracks() {
        clickItemWithText(testDriver.getString(R.string.play_related_tracks));
        return new VisualPlayerElement(testDriver);
    }

    public void clickRemoveFromPlaylist() {
        clickItemWithText(testDriver.getString(R.string.remove_from_playlist));
    }

    public boolean isLiked() {
        return getElementText(likeItem()).equals("Unlike");
    }

    private ViewElement likeItem() {
        return getRootMenuElements().get(0);
    }

    private void clickItemWithText(String text) {
        final ViewElement item = findElement(With.text(text));
        assertThat(item, is(visible()));
        item.click();
    }
}
