package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class EmptyViewElement extends Element {

    public EmptyViewElement(Han solo, With matcher) {
        super(solo, matcher);
    }


    //FIXME: Don't return ViewElements
    public ViewElement emptyConnectionErrorMessage() {
        return emptyView().findElement(With.text(solo.getString(R.string.ak_error_no_internet)));
    }

    public String message() {
        return messageView().getText();
    }

    private ViewElement emptyView(){
        return solo.findElement(With.id(R.id.ak_emptyview_holder));
    }

    private TextElement messageView() {
        return new TextElement(emptyView().findElement(With.id(R.id.ak_emptyview_message)));
    }
}
