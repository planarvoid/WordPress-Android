package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistsScreen;

public class PlaylistItemOverflowMenu extends PopupMenuElement {

    public PlaylistItemOverflowMenu(Han testDriver) {
        super(testDriver);
    }

    public PlaylistsScreen clickMakeAvailableOffline() {
        menuItems().get(0).click();
        return new PlaylistsScreen(testDriver);
    }

    public void toggleLike() {
        likeItem().click();
    }

    public boolean isLiked() {
        return getTextView(likeItem()).getText().equals("Unlike");
    }

    private TextElement getTextView(ViewElement viewElement) {
        return new TextElement(viewElement.findElement(With.className("android.widget.TextView")));
    }

    private ViewElement likeItem() {
        if(menuItems().size() == 2) {
            return menuItems().get(1);
        }
        return menuItems().get(0);
    }
}
