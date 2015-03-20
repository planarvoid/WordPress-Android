package com.soundcloud.android.screens.elements;

import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.AddToPlaylistScreen;

public class TrackItemMenuElement extends PopupMenuElement {

    public TrackItemMenuElement(Han solo) {
        super(solo);
    }

    public void toggleLike() {
        menuItems().get(0).click();
    }

    public AddToPlaylistScreen clickAddToPlaylist() {
        clickItemWithText(1, "Add");
        return new AddToPlaylistScreen(testDriver);
    }

    public void clickRemoveFromPlaylist() {
        clickItemWithText(1, "Remove");
    }

    private void clickItemWithText(int position, String text) {
        final ViewElement item = menuItems().get(position);
        assertThat(item.findElement(With.textContaining(text)), is(visible()));
        item.click();
    }
}
