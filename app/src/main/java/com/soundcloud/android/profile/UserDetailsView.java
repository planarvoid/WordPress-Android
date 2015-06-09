package com.soundcloud.android.profile;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Locale;

public class UserDetailsView extends DefaultSupportFragmentLightCycle<UserDetailsFragment> {

    private static final String DISCOGS_PATH = "http://www.discogs.com/artist/%s";
    private static final String MYSPACE_PATH = "http://www.myspace.com/%s";

    private UserDetailsFragment fragment;

    @Inject ProfileEmptyViewHelper profileEmptyViewHelper;

    @InjectView(R.id.description) TextView descriptionText;
    @InjectView(R.id.website) TextView websiteText;
    @InjectView(R.id.discogs_name) TextView discogsText;
    @InjectView(R.id.myspace_name) TextView myspaceText;
    @InjectView(android.R.id.empty) EmptyView emptyView;

    @Inject
    public UserDetailsView() {
        // dgr
    }

    @Override
    public void onCreate(UserDetailsFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        this.fragment = fragment;
    }

    @Override
    public void onViewCreated(UserDetailsFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.inject(this, view);

        profileEmptyViewHelper.configureBuilderForUserDetails(emptyView, getUserUrn(fragment));
    }

    private Urn getUserUrn(UserDetailsFragment fragment) {
        return fragment.getActivity().getIntent().getParcelableExtra(ProfileActivity.EXTRA_USER_URN);
    }

    public void setTopPadding(int currentHeaderSize) {
        View fragmentLayout = getFragmentView();
        if (fragmentLayout != null){
            fragmentLayout.setPadding(0, currentHeaderSize, 0, 0);
        }
    }

    void showEmptyView(EmptyView.Status status){
        if (emptyView != null){
            emptyView.setStatus(status);
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    void hideEmptyView() {
        if (emptyView != null){
            emptyView.setVisibility(View.GONE);
        }
    }

    private View getFragmentView() {
        return fragment == null ? null : fragment.getView();
    }

    void showDescription(String description) {
        descriptionText.setVisibility(View.VISIBLE);
        descriptionText.setText(ScTextUtils.fromHtml(description));
        descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    void hideDescription() {
        descriptionText.setVisibility(View.GONE);
    }

    void showWebsite(final String websiteUrl, String websiteName) {
        websiteText.setText(ScTextUtils.isBlank(websiteName) ? websiteUrl : websiteName);
        websiteText.setVisibility(View.VISIBLE);
        websiteText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl)));
            }
        });
    }

    void hideWebsite() {
        websiteText.setVisibility(View.GONE);
    }

    void showDiscogs(final String discogsName) {
        discogsText.setMovementMethod(LinkMovementMethod.getInstance());
        discogsText.setVisibility(View.VISIBLE);
        discogsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US, DISCOGS_PATH, discogsName))));
            }
        });
    }

    void hideDiscogs() {
        discogsText.setVisibility(View.GONE);
    }

    void showMyspace(final String myspaceName) {
        myspaceText.setMovementMethod(LinkMovementMethod.getInstance());
        myspaceText.setVisibility(View.VISIBLE);
        myspaceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US, MYSPACE_PATH, myspaceName))));
            }
        });
    }

    void hideMyspace() {
        myspaceText.setVisibility(View.GONE);
    }
}
