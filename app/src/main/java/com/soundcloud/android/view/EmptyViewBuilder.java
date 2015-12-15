package com.soundcloud.android.view;

import com.soundcloud.android.R;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;

/**
 * A builder class to build or configure {@link EmptyView}s. Note that it is essential that instances of this class must be
 * retainable across configuration changes, so do NOT hold strong references to views or Context in here!
 */
public class EmptyViewBuilder {

    private int image;
    private String messageText, secondaryText;

    public EmptyView build(Context context) {
        EmptyView view = new EmptyView(context);
        return configure(view);
    }

    @NotNull
    public EmptyView configure(EmptyView view) {
        if (messageText != null) {
            view.setMessageText(messageText);
        }
        if (secondaryText != null) {
            view.setSecondaryText(secondaryText);
        }
        if (image > 0) {
            view.setImage(image);
        }
        return view;
    }

    public EmptyViewBuilder withMessageText(@Nullable String messageText) {
        this.messageText = messageText;
        return this;
    }

    public EmptyViewBuilder withSecondaryText(@Nullable String secondaryText) {
        this.secondaryText = secondaryText;
        return this;
    }

    public EmptyViewBuilder withImage(int imageResourceId) {
        image = imageResourceId;
        return this;
    }

    public void configureForSearch(EmptyView emptyView) {
        emptyView.setImage(R.drawable.empty_search);
        emptyView.setMessageText(R.string.search_empty);
        emptyView.setSecondaryText(R.string.search_empty_subtext);
    }

}
