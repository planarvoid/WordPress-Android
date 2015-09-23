package com.soundcloud.android.profile;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyView;
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

    @Inject ProfileEmptyViewHelper profileEmptyViewHelper;
    
    @Bind(R.id.description) TextView descriptionText;
    @Bind(R.id.website) TextView websiteText;
    @Bind(R.id.discogs_name) TextView discogsText;
    @Bind(R.id.myspace_name) TextView myspaceText;
    @Bind(android.R.id.empty) EmptyView emptyView;

    private UserDetailsListener listener;
    private Urn userUrn;

    @Inject
    public UserDetailsView() {
        // dgr
    }

    public void setListener(UserDetailsListener listener) {
        this.listener = listener;
    }

    public void setUrn(Urn userUrn){
        this.userUrn = userUrn;
    }

    public void setView(View view){
        ButterKnife.bind(this, view);
    }

    public void clearViews(){
        ButterKnife.unbind(this);
    }

    void showEmptyView(EmptyView.Status status){
        if (emptyView != null){
            profileEmptyViewHelper.configureBuilderForUserDetails(emptyView, this.userUrn);
            emptyView.setStatus(status);
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    void hideEmptyView() {
        if (emptyView != null){
            emptyView.setVisibility(View.GONE);
        }
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
        websiteText.setText(Strings.isBlank(websiteName) ? websiteUrl : websiteName);
        websiteText.setVisibility(View.VISIBLE);
        websiteText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onViewUri(Uri.parse(websiteUrl));
                }
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
                if (listener != null) {
                    listener.onViewUri(Uri.parse(String.format(Locale.US, DISCOGS_PATH, discogsName)));
                }
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
                if (listener != null) {
                    listener.onViewUri(Uri.parse(String.format(Locale.US, MYSPACE_PATH, myspaceName)));
                }
            }
        });
    }

    void hideMyspace() {
        myspaceText.setVisibility(View.GONE);
    }

    interface UserDetailsListener {
        void onViewUri(Uri uri);
    }
}
