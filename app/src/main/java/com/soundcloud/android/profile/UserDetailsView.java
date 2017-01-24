package com.soundcloud.android.profile;

import static android.support.v4.content.ContextCompat.getDrawable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.users.SocialMediaLink;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.CustomFontTextView;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class UserDetailsView {

    private static final String DISCOGS_PATH = "http://www.discogs.com/artist/%s";
    private static final String MYSPACE_PATH = "http://www.myspace.com/%s";

    @BindView(R.id.followers_count) TextView followersCount;
    @BindView(R.id.followings_count) TextView followingsCount;
    @BindView(R.id.bio_section) View bioSection;
    @BindView(R.id.bio_text) TextView bioText;
    @BindView(R.id.links_header) LinearLayout linksHeader;
    @BindView(R.id.links_container) LinearLayout linksContainer;

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

    public void hideLinks() {
        linksHeader.setVisibility(View.GONE);
        linksContainer.setVisibility(View.GONE);
    }

    public void showLinks(List<SocialMediaLink> socialMediaLinks) {
        linksContainer.removeAllViews();
        final Context context = linksHeader.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        for (SocialMediaLink socialMediaLink : socialMediaLinks) {
            final View link = layoutInflater.inflate(R.layout.user_info_social_media_link, null);
            final CustomFontTextView linkText = (CustomFontTextView) link.findViewById(R.id.social_link);
            linkText.setMovementMethod(LinkMovementMethod.getInstance());
            linkText.setText(textFor(socialMediaLink, context));
            linkText.setCompoundDrawablesWithIntrinsicBounds(drawableFor(socialMediaLink, context), null, null, null);
            linkText.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewUri(Uri.parse(socialMediaLink.url()));
                }
            });
            linksContainer.addView(link);
        }
        linksHeader.setVisibility(View.VISIBLE);
        linksContainer.setVisibility(View.VISIBLE);
    }

    private String textFor(SocialMediaLink socialMediaLink, Context context) {
        if (socialMediaLink.title().isPresent()) {
            return socialMediaLink.title().get();
        } else {
            final Resources resources = context.getResources();
            final int resourceId = resources.getIdentifier(socialMediaLink.network(), "string", context.getPackageName());
            return (resourceId != 0) ? resources.getString(resourceId) : socialMediaLink.url();
        }
    }

    private Drawable drawableFor(SocialMediaLink socialMediaLink, Context context) {
        final Resources resources = context.getResources();
        final int resourceId = resources.getIdentifier(String.format("favicon_%s", socialMediaLink.network()), "drawable", context.getPackageName());
        final int fallbackId = resources.getIdentifier("favicon_website", "drawable", context.getPackageName());
        return (resourceId != 0) ? getDrawable(context, resourceId) : getDrawable(context, fallbackId);
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
