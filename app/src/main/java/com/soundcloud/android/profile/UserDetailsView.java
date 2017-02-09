package com.soundcloud.android.profile;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.users.SocialMediaLinkItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.CustomFontTextView;
import com.soundcloud.android.view.EmptyView;

import android.content.Context;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class UserDetailsView {

    @BindView(R.id.followers_count) TextView followersCount;
    @BindView(R.id.followings_count) TextView followingsCount;
    @BindView(R.id.bio_section) View bioSection;
    @BindView(R.id.bio_text) TextView bioText;
    @BindView(R.id.links_header) LinearLayout linksHeader;
    @BindView(R.id.links_container) LinearLayout linksContainer;
    @BindView(android.R.id.empty) EmptyView emptyView;

    private UserDetailsListener listener;
    private Unbinder unbinder;

    @Inject
    public UserDetailsView() {
        // dgr
    }

    public void setListener(UserDetailsListener listener) {
        this.listener = listener;
    }

    public void setView(View view) {
        unbinder = ButterKnife.bind(this, view);
    }

    public void clearViews() {
        unbinder.unbind();
    }

    void showEmptyView(EmptyView.Status status) {
        if (emptyView != null) {
            emptyView.setStatus(status);
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    void hideEmptyView() {
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
    }

    public void hideLinks() {
        linksHeader.setVisibility(View.GONE);
        linksContainer.setVisibility(View.GONE);
    }

    public void showLinks(List<SocialMediaLinkItem> socialMediaLinks) {
        linksContainer.removeAllViews();
        final Context context = linksHeader.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        for (SocialMediaLinkItem socialMediaLink : socialMediaLinks) {
            final View link = layoutInflater.inflate(R.layout.user_info_social_media_link, null);
            final CustomFontTextView linkText = (CustomFontTextView) link.findViewById(R.id.social_link);
            linkText.setMovementMethod(LinkMovementMethod.getInstance());
            linkText.setText(socialMediaLink.displayName());
            linkText.setCompoundDrawablesWithIntrinsicBounds(socialMediaLink.icon(context), null, null, null);
            linkText.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewUri(socialMediaLink.uri());
                }
            });
            linksContainer.addView(link);
        }
        linksHeader.setVisibility(View.VISIBLE);
        linksContainer.setVisibility(View.VISIBLE);
    }

    void showBio(String contents) {
        bioSection.setVisibility(View.VISIBLE);
        bioText.setText(ScTextUtils.fromHtml(contents));
        bioText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    void hideBio() {
        bioSection.setVisibility(View.GONE);
    }

    public void setFollowersCount(String count) {
        followersCount.setText(count);
    }

    public void setFollowingsCount(String count) {
        followingsCount.setText(count);
    }

    interface UserDetailsListener {
        void onViewUri(Uri uri);

        void onViewFollowersClicked();

        void onViewFollowingClicked();
    }

    @OnClick(R.id.view_followers)
    void onViewFollowersClicked(View item) {
        listener.onViewFollowersClicked();
    }

    @OnClick(R.id.view_following)
    void onViewFollowingClicked(View item) {
        listener.onViewFollowingClicked();
    }
}
