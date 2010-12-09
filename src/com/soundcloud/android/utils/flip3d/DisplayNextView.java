package com.soundcloud.android.utils.flip3d;


import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;

public final class DisplayNextView implements Animation.AnimationListener {
private boolean mCurrentView;
View view1;
View view2;

public DisplayNextView(boolean currentView, View view1, View view2) {
	 Log.i("DEBUG","Display Next Views " + view1 + " " + view2);
mCurrentView = currentView;
this.view1 = view1;
this.view2 = view2;
}

public void onAnimationStart(Animation animation) {
}

public void onAnimationEnd(Animation animation) {
	view1.post(new SwapViews(mCurrentView, view1, view2));
}

public void onAnimationRepeat(Animation animation) {
}
}