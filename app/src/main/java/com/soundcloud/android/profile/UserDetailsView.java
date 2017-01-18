package com.soundcloud.android.profile;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;

import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Locale;

class UserDetailsView {

    private static final String DISCOGS_PATH = "http://www.discogs.com/artist/%s";
    private static final String MYSPACE_PATH = "http://www.myspace.com/%s";

    @BindView(R.id.bio_section) View bioSection;
    @BindView(R.id.links_section) View linksSection;
    @BindView(R.id.bio_text) TextView bioText;
    @BindView(R.id.website) TextView websiteText;
    @BindView(R.id.website_divider) View websiteDivider;
    @BindView(R.id.discogs_name) TextView discogsText;
    @BindView(R.id.discogs_divider) View discogsDivider;
    @BindView(R.id.myspace_name) TextView myspaceText;
    @BindView(R.id.myspace_divider) View myspaceDivider;
    @BindView(R.id.followers_count) TextView followersCount;
    @BindView(R.id.followings_count) TextView followingsCount;

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

    public void showLinksSection() {
        linksSection.setVisibility(View.VISIBLE);
    }

    public void hideLinksSection() {
        linksSection.setVisibility(View.GONE);
    }

    void showBio(String contents) {
        bioSection.setVisibility(View.VISIBLE);
        bioText.setText(ScTextUtils.fromHtml(contents));
        bioText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    void hideBio() {
        bioSection.setVisibility(View.GONE);
    }

    void showWebsite(final String websiteUrl, String websiteName) {
        websiteDivider.setVisibility(View.VISIBLE);
        websiteText.setVisibility(View.VISIBLE);
        websiteText.setText(Strings.isBlank(websiteName) ? websiteUrl : websiteName);
        websiteText.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewUri(Uri.parse(websiteUrl));
            }
        });
    }

    void hideWebsite() {
        websiteDivider.setVisibility(View.GONE);
        websiteText.setVisibility(View.GONE);
    }

    void showDiscogs(final String discogsName) {
        discogsDivider.setVisibility(View.VISIBLE);
        discogsText.setVisibility(View.VISIBLE);
        discogsText.setMovementMethod(LinkMovementMethod.getInstance());
        discogsText.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewUri(Uri.parse(String.format(Locale.US, DISCOGS_PATH, discogsName)));
            }
        });
    }

    void hideDiscogs() {
        discogsDivider.setVisibility(View.GONE);
        discogsText.setVisibility(View.GONE);
    }

    void showMyspace(final String myspaceName) {
        myspaceDivider.setVisibility(View.VISIBLE);
        myspaceText.setVisibility(View.VISIBLE);
        myspaceText.setMovementMethod(LinkMovementMethod.getInstance());
        myspaceText.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewUri(Uri.parse(String.format(Locale.US, MYSPACE_PATH, myspaceName)));
            }
        });
    }

    void hideMyspace() {
        myspaceDivider.setVisibility(View.GONE);
        myspaceText.setVisibility(View.GONE);
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
