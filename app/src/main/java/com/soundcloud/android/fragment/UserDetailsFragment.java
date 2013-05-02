package com.soundcloud.android.fragment;

import static android.text.TextUtils.isEmpty;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyListView;

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

public class UserDetailsFragment extends Fragment {

    private long mUserId;
    private FrameLayout mInfoView;
    private TextView mLocation, mWebsite, mDiscogsName, mMyspaceName, mDescription;
    private EmptyListView mEmptyInfoView;
    private boolean mDisplayedInfo, mInfoError, mAllowEmpty;

    public static UserDetailsFragment newInstance(long userId) {
        UserDetailsFragment fragment = new UserDetailsFragment();
        Bundle args = new Bundle();
        args.putLong("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mUserId = getArguments().getLong("userId");
        mInfoView = (FrameLayout) inflater.inflate(R.layout.user_browser_details_view, null);
        mLocation = (TextView) mInfoView.findViewById(R.id.location);
        mWebsite = (TextView) mInfoView.findViewById(R.id.website);
        mDiscogsName = (TextView) mInfoView.findViewById(R.id.discogs_name);
        mMyspaceName = (TextView) mInfoView.findViewById(R.id.myspace_name);
        mDescription = (TextView) mInfoView.findViewById(R.id.description);
        User user = SoundCloudApplication.MODEL_MANAGER.getUser(mUserId);
        if (user != null){
            setUser(user);
        }
        return mInfoView;
    }

    public void onSuccess(User user) {
        mAllowEmpty = true;
        if (getActivity() != null){
            mInfoError = false;
            setUser(user);
        }
    }

    public void onError() {
        mAllowEmpty = true;
        if (getActivity() != null) {
            mInfoError = true;
            if (!mDisplayedInfo) {
                configureEmptyView();
            }
        }
    }

    private void setUser(User user) {
        mDisplayedInfo = setupWebsite(user)
                | setupDiscogs(user)
                | setupMyspace(user)
                | setupLocation(user)
                | setupDescription(user);

        configureEmptyView();
    }

    private void configureEmptyView() {
        if (mDisplayedInfo && mEmptyInfoView != null && mEmptyInfoView.getParent() == mInfoView) {
            mInfoView.removeView(mEmptyInfoView);
        } else if (!mDisplayedInfo) {
            if (mEmptyInfoView == null) {
                mEmptyInfoView = new EmptyListView(getActivity());
            }
            if (!mAllowEmpty) {
                mEmptyInfoView.setStatus(EmptyListView.Status.WAITING);
            } else {
                if (mInfoError) {
                    mEmptyInfoView.setStatus(EmptyListView.Status.ERROR);

                } else {
                    mEmptyInfoView.setStatus(EmptyListView.Status.OK);
                    if (mUserId == SoundCloudApplication.getUserId()) {
                        if (getActivity() != null) {
                            mEmptyInfoView.setMessageText(R.string.info_empty_you_message)
                                    .setActionText(getString(R.string.info_empty_you_action))
                                    .setButtonActions(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/settings")), null);
                        }
                    } else {
                        mEmptyInfoView.setMessageText(R.string.info_empty_other_message)
                                .setActionText(null);
                    }
                    if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                        // won't fit in most landscape views
                        mEmptyInfoView.setImage(-1);
                        mEmptyInfoView.setActionText(null);
                    }
                }


            }

            if (mEmptyInfoView.getParent() != mInfoView) {
                if (mEmptyInfoView.getParent() instanceof ViewGroup){
                    ((ViewGroup) mEmptyInfoView.getParent()).removeView(mEmptyInfoView);
                }
                mInfoView.addView(mEmptyInfoView,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }

    }



    private boolean setupDiscogs(final User user) {
        if (!isEmpty(user.discogs_name)) {

            mDiscogsName.setMovementMethod(LinkMovementMethod.getInstance());
            mDiscogsName.setVisibility(View.VISIBLE);
            mDiscogsName.setFocusable(true);
            mDiscogsName.setClickable(true);
            mDiscogsName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://www.discogs.com/artist/" + user.discogs_name));
                    startActivity(viewIntent);
                }
            });
            return true;
        } else {
            mDiscogsName.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupDescription(User user) {
        if (!isEmpty(user.description)) {
            mDescription.setVisibility(View.VISIBLE);
            mDescription.setText(ScTextUtils.fromHtml(user.description));
            mDescription.setMovementMethod(LinkMovementMethod.getInstance());
            return true;
        } else {
            mDescription.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupLocation(User user) {
        final String location = user.getLocation();
        if (!isEmpty(location)) {
            mLocation.setText(getString(R.string.from) + " " + location);
            mLocation.setVisibility(View.VISIBLE);
            return true;
        } else {
            mLocation.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupWebsite(final User user) {
        if (!isEmpty(user.website)) {
            mWebsite.setText(user.getWebSiteTitle());
            mWebsite.setVisibility(View.VISIBLE);
            mWebsite.setFocusable(true);
            mWebsite.setClickable(true);
            mWebsite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.website));
                    startActivity(viewIntent);
                }
            });
            return true;
        } else {
            mWebsite.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean setupMyspace(final User user) {
        if (!isEmpty(user.myspace_name)) {
            mMyspaceName.setMovementMethod(LinkMovementMethod.getInstance());
            mMyspaceName.setVisibility(View.VISIBLE);
            mMyspaceName.setFocusable(true);
            mMyspaceName.setClickable(true);
            mMyspaceName.setOnClickListener(new View.OnClickListener() {
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
            mMyspaceName.setVisibility(View.GONE);
            return false;
        }
    }
}
