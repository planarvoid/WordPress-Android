package com.handmark.pulltorefresh.library.internal;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.CloudUtils;

public class LoadingLayout extends FrameLayout {

	static final int DEFAULT_ROTATION_ANIMATION_DURATION = 150;

	private final ImageView headerImage;
	private final ProgressBar headerProgress;
	private final TextView headerText;
    private final TextView lastUpdatedText;

	private String pullLabel;
	private String refreshingLabel;
	private String releaseLabel;

	private final Animation rotateAnimation, resetRotateAnimation;
    private long lastUpdated;

    public LoadingLayout(Context context, final int mode, String releaseLabel, String pullLabel, String refreshingLabel) {
		super(context);
		ViewGroup header = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header, this);
		headerText = (TextView) header.findViewById(R.id.pull_to_refresh_text);
		headerImage = (ImageView) header.findViewById(R.id.pull_to_refresh_image);
		headerProgress = (ProgressBar) header.findViewById(R.id.pull_to_refresh_progress);
        lastUpdatedText = (TextView) header.findViewById(R.id.pull_to_refresh_updated_at);

		final Interpolator interpolator = new LinearInterpolator();
		rotateAnimation = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
		        0.5f);
		rotateAnimation.setInterpolator(interpolator);
		rotateAnimation.setDuration(DEFAULT_ROTATION_ANIMATION_DURATION);
		rotateAnimation.setFillAfter(true);

		resetRotateAnimation = new RotateAnimation(-180, 0, Animation.RELATIVE_TO_SELF, 0.5f,
		        Animation.RELATIVE_TO_SELF, 0.5f);
		resetRotateAnimation.setInterpolator(interpolator);
		resetRotateAnimation.setDuration(DEFAULT_ROTATION_ANIMATION_DURATION);
		resetRotateAnimation.setFillAfter(true);

		this.releaseLabel = releaseLabel;
		this.pullLabel = pullLabel;
		this.refreshingLabel = refreshingLabel;

		switch (mode) {
			case PullToRefreshBase.MODE_PULL_UP_TO_REFRESH:
				headerImage.setImageResource(R.drawable.pulltorefresh_up_arrow);
				break;
			case PullToRefreshBase.MODE_PULL_DOWN_TO_REFRESH:
			default:
				headerImage.setImageResource(R.drawable.pulltorefresh_down_arrow);
				break;
		}
	}

	public void reset() {
		headerText.setText(pullLabel);
		headerImage.setVisibility(View.VISIBLE);
		headerProgress.setVisibility(View.GONE);
        configureLastUpdated();
	}

    public void releaseToRefresh() {
		headerText.setText(releaseLabel);
		headerImage.clearAnimation();
		headerImage.startAnimation(rotateAnimation);
	}

	public void setPullLabel(String pullLabel) {
		this.pullLabel = pullLabel;
	}

	public void refreshing() {
		headerText.setText(refreshingLabel);
		headerImage.clearAnimation();
		headerImage.setVisibility(View.INVISIBLE);
		headerProgress.setVisibility(View.VISIBLE);
	}

	public void setRefreshingLabel(String refreshingLabel) {
		this.refreshingLabel = refreshingLabel;
	}

	public void setReleaseLabel(String releaseLabel) {
		this.releaseLabel = releaseLabel;
	}

	public void pullToRefresh() {
		headerText.setText(pullLabel);
		headerImage.clearAnimation();
		headerImage.startAnimation(resetRotateAnimation);
	}

	public void setTextColor(int color) {
		headerText.setTextColor(color);
	}

    public void configureLastUpdated(){
        if (lastUpdated > 0){
            lastUpdatedText.setText(getResources().getString(R.string.pull_to_refresh_last_updated,
                    CloudUtils.getElapsedTimeString(getResources(), lastUpdated, true)));
            lastUpdatedText.setVisibility(View.VISIBLE);
        } else {
            lastUpdatedText.setVisibility(View.INVISIBLE);
        }
    }

    public void setLastUpdated(long lastUpdated) {
        if (this.lastUpdated != lastUpdated){
            this.lastUpdated = lastUpdated;
            configureLastUpdated();
        }
    }
}
