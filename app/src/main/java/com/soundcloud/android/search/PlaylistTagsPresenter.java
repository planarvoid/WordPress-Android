package com.soundcloud.android.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.FlowLayout;
import com.soundcloud.rx.eventbus.EventBus;

import java.util.List;

import javax.inject.Inject;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class PlaylistTagsPresenter {

    private final Resources resources;
    private final EventBus eventBus;

    @Nullable private Listener listener;

    public interface Listener {
        void onTagSelected(Context context, String tag);
    }

    @Inject
    public PlaylistTagsPresenter(Resources resources, EventBus eventBus) {
        this.resources = resources;
        this.eventBus = eventBus;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    private final View.OnClickListener recentTagClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.recentTagSearch((String) v.getTag()));
            if (listener != null) {
                listener.onTagSelected(v.getContext(), (String) v.getTag());
            }
        }
    };

    private final View.OnClickListener popularTagClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.popularTagSearch((String) v.getTag()));
            if (listener != null) {
                listener.onTagSelected(v.getContext(), (String) v.getTag());
            }
        }
    };

    public void displayPopularTags(View view, List<String> tags) {
        view.findViewById(R.id.popular_tags_container).setVisibility(VISIBLE);
        displayTags(view, tags, R.id.all_tags, popularTagClickListener);
    }

    public void hidePopularTags(View view) {
        view.findViewById(R.id.popular_tags_container).setVisibility(GONE);
    }

    public void displayRecentTags(View view, List<String> tags) {
        view.findViewById(R.id.recent_tags_container).setVisibility(VISIBLE);
        displayTags(view, tags, R.id.recent_tags, recentTagClickListener);
    }

    @SuppressLint("SetTextI18n")
    private void displayTags(View layout, List<String> tags,
                             int layoutId, View.OnClickListener tagClickListener) {
        ViewGroup tagFlowLayout = (ViewGroup) layout.findViewById(layoutId);
        tagFlowLayout.removeAllViews();

        int padding = ViewUtils.dpToPx(resources, 5);
        FlowLayout.LayoutParams flowLP = new FlowLayout.LayoutParams(padding, padding);

        for (final String tag : tags) {
            if (!TextUtils.isEmpty(tag)) {
                TextView tagView = (TextView) View.inflate(layout.getContext(), R.layout.btn_tag, null);
                tagView.setText("#" + tag);
                tagView.setTag(tag);
                tagView.setOnClickListener(tagClickListener);
                tagFlowLayout.addView(tagView, flowLP);
            }
        }
    }
}
