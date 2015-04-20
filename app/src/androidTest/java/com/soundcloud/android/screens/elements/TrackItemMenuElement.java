package com.soundcloud.android.screens.elements;

import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.AddToPlaylistScreen;

public class TrackItemMenuElement extends PopupMenuElement {

    public TrackItemMenuElement(Han solo) {
        super(solo);
    }

    public void toggleLike() {
        likeItem().click();
    }

    public AddToPlaylistScreen clickAddToPlaylist() {
        clickItemWithText(1, "Add");
        return new AddToPlaylistScreen(testDriver);
    }

    public void clickRemoveFromPlaylist() {
        clickItemWithText(1, "Remove");
    }

    public boolean isLiked() {
        return getTextView(likeItem()).getText().equals("Unlike");
    }

    private TextElement getTextView(ViewElement viewElement) {
        return new TextElement(viewElement.findElement(With.className("android.widget.TextView")));
    }

    private ViewElement likeItem() {
        return menuItems().get(0);
    }

    private void clickItemWithText(int position, String text) {
        final ViewElement item = menuItems().get(position);
        assertThat(item.findElement(With.textContaining(text)), is(visible()));
        item.click();
    }
}
