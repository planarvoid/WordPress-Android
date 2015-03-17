package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import java.util.List;

public class PlaylistItemOverflowMenu {

    private final Han testDriver;

    public PlaylistItemOverflowMenu(Han testDriver) {
        this.testDriver = testDriver;
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

    private ViewElement container() {
        return testDriver.findElement(With.className("android.widget.PopupWindow$PopupViewContainer"));
    }

    private ViewElement likeItem() {
        if(menuItems().size() == 2) {
            return menuItems().get(1);
        }
        return menuItems().get(0);
    }


    private List<ViewElement> menuItems() {
        return container().findElements(With.classSimpleName("ListMenuItemView"));
    }
}
