package com.soundcloud.android.profile;

import static android.text.TextUtils.isEmpty;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class UserInfoFragment extends Fragment {

    private long userId;
    private EmptyViewBuilder emptyViewFactory;
    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;
    private boolean displayedInfo;

    public static UserInfoFragment newInstance(long userId) {
        UserInfoFragment fragment = new UserInfoFragment();
        Bundle args = new Bundle();
        args.putLong("userId", userId);
        fragment.setArguments(args);
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        emptyViewFactory = new EmptyViewBuilder();
    }

    @Override @SuppressLint("InflateParams")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        userId = getArguments().getLong("userId");
        ViewGroup fragmentLayout = (ViewGroup) inflater.inflate(R.layout.legacy_user_info_view, null);
        PublicApiUser user = SoundCloudApplication.sModelManager.getUser(userId);
        if (user != null) {
            updateViews(fragmentLayout, user);
        }
        return fragmentLayout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureEmptyView((ViewGroup) view);
    }

    public void onSuccess(PublicApiUser user) {
        emptyViewStatus = EmptyView.Status.OK;
        ViewGroup fragmentLayout = (ViewGroup) getView();
        if (fragmentLayout != null) {
            updateViews(fragmentLayout, user);
            configureEmptyView(fragmentLayout);
        }
    }

    public void onError() {
        emptyViewStatus = EmptyView.Status.ERROR;
        ViewGroup fragmentLayout = (ViewGroup) getView();
        if (fragmentLayout != null) {
            configureEmptyView(fragmentLayout);
        }
    }

    private void updateViews(ViewGroup fragmentLayout, PublicApiUser user) {
        displayedInfo = setupWebsite(fragmentLayout, user)
                | setupDiscogs(fragmentLayout, user)
                | setupMyspace(fragmentLayout, user)
                | setupDescription(fragmentLayout, user);
    }

    private void configureEmptyView(ViewGroup fragmentLayout) {
        EmptyView emptyView = (EmptyView) fragmentLayout.findViewById(android.R.id.empty);
        if (emptyView != null) {
            fragmentLayout.removeView(emptyView);
        }

        if (!displayedInfo) {
            if (emptyViewStatus == EmptyView.Status.OK) {
                if (userId == SoundCloudApplication.fromContext(getActivity()).getAccountOperations().getLoggedInUserId()) {
                    emptyViewFactory.withMessageText(getString(R.string.info_empty_you_message))
                            .withSecondaryText(getString(R.string.info_empty_you_secondary))
                            .withImage(R.drawable.empty_profile);
                } else {
                    emptyViewFactory.withMessageText(getString(R.string.info_empty_other_message))
                            .withImage(R.drawable.empty_info);
                }
            }
            emptyView = emptyViewFactory.build(getActivity());
            emptyView.setId(android.R.id.empty);
            emptyView.setStatus(emptyViewStatus);

            fragmentLayout.addView(emptyView,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private boolean setupDiscogs(ViewGroup fragmentLayout, final PublicApiUser user) {
        TextView discogsName = (TextView) fragmentLayout.findViewById(R.id.discogs_name);
        if (!isEmpty(user.discogs_name)) {

            discogsName.setMovementMethod(LinkMovementMethod.getInstance());
            discogsName.setVisibility(View.VISIBLE);
            discogsName.setFocusable(true);
            discogsName.setClickable(true);
            discogsName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://www.discogs.com/artist/" + user.discogs_name));
                    startActivity(viewIntent);
                }
            });
            return true;
        } else {
            discogsName.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupDescription(ViewGroup fragmentLayout, PublicApiUser user) {
        TextView description = (TextView) fragmentLayout.findViewById(R.id.description);
        if (!isEmpty(user.description)) {
            description.setVisibility(View.VISIBLE);
            description.setText(ScTextUtils.fromHtml(user.description));
            description.setMovementMethod(LinkMovementMethod.getInstance());
            return true;
        } else {
            description.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupWebsite(ViewGroup fragmentLayout, final PublicApiUser user) {
        TextView website = (TextView) fragmentLayout.findViewById(R.id.website);
        if (!isEmpty(user.website)) {
            website.setText(user.getWebSiteTitle());
            website.setVisibility(View.VISIBLE);
            website.setFocusable(true);
            website.setClickable(true);
            website.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.website));
                    startActivity(viewIntent);
                }
            });
            return true;
        } else {
            website.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupMyspace(ViewGroup fragmentLayout, final PublicApiUser user) {
        TextView myspaceName = (TextView) fragmentLayout.findViewById(R.id.myspace_name);
        if (!isEmpty(user.myspace_name)) {
            myspaceName.setMovementMethod(LinkMovementMethod.getInstance());
            myspaceName.setVisibility(View.VISIBLE);
            myspaceName.setFocusable(true);
            myspaceName.setClickable(true);
            myspaceName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent =
                            new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://www.myspace.com/" + user.myspace_name));
                    startActivity(viewIntent);
                }
            });
            return true;
        } else {
            myspaceName.setVisibility(View.GONE);
            return false;
        }
    }
}
