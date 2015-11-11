package com.soundcloud.android.screens.elements;

import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.StreamScreen;

public class TrackItemMenuElement extends PopupMenuElement {

    public TrackItemMenuElement(Han solo) {
        super(solo);
    }

    public StreamScreen toggleLike() {
        likeItem().click();
        return new StreamScreen(testDriver);
    }

    public StreamScreen toggleRepost() {
        repostItem().click();
        return new StreamScreen(testDriver);
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

    public boolean isReposted() {
        return getElementText(repostItem()).equals("Unpost");
    }

    private ViewElement likeItem() {
        return findElement(With.text(testDriver.getString(R.string.btn_like), testDriver.getString(R.string.btn_unlike)));
    }

    private ViewElement repostItem() {
        return findElement(With.text(testDriver.getString(R.string.repost), testDriver.getString(R.string.unpost)));
    }

    private void clickItemWithText(String text) {
        final ViewElement item = findElement(With.text(text));
        assertThat(item, is(visible()));
        item.click();
    }

    public VisualPlayerElement clickStartStation() {
        clickItemWithText(testDriver.getString(R.string.stations_start_track_station));
        return new VisualPlayerElement(testDriver);
    }
}
