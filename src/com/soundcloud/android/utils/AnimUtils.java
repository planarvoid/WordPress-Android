
package com.soundcloud.android.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Interpolator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;

/**
 * AnimationUtils is a helper class to make it easy to apply certain animation
 * effects to layouts and components. It also demonstrates various ways of
 * loading animation sequence definitions - from XML and generated in Java code.
 *
 * @author Nazmul Idris
 * @version 1.0
 * @since Jun 24, 2008, 1:22:27 PM
 */
public class AnimUtils {

    /*
     * @see <ahref=
     * "http://code.google.com/android/samples/ApiDemos/src/com/google/android/samples/view/LayoutAnimation2.html"
     * >animations</a>
     */
    public static void setLayoutAnim_slidedownfromtop(ViewGroup panel, Context ctx) {

        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(100);
        set.addAnimation(animation);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(500);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(set, 0.25f);
        panel.setLayoutAnimation(controller);

    }

    public static void setLayoutAnim_slideupfrombottom(ViewGroup panel, Context ctx) {

        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(100);
        set.addAnimation(animation);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(500);
        set.addAnimation(animation);

        // set.setFillBefore(false);
        // set.setFillAfter(false);

        LayoutAnimationController controller = new LayoutAnimationController(set, 0.25f);
        panel.setLayoutAnimation(controller);

    }

    public static Animation runFadeInAnimationOn(Activity ctx, View target) {
        Animation animation = AnimationUtils.loadAnimation(ctx, android.R.anim.fade_in);
        target.startAnimation(animation);
        return animation;
    }

    public static Animation runFadeOutAnimationOn(Activity ctx, View target) {
        Animation animation = AnimationUtils.loadAnimation(ctx, android.R.anim.fade_out);
        target.startAnimation(animation);
        return animation;
    }

    public static Animation inFromRightAnimation() {
        return inFromRightAnimation(new DecelerateInterpolator());
    }

    // for the previous movement
    public static Animation inFromRightAnimation(android.view.animation.Interpolator i) {

        Animation inFromRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromRight.setDuration(250);
        inFromRight.setInterpolator(i);
        return inFromRight;
    }

    public static Animation outToLeftAnimation() {
        return outToLeftAnimation(new AccelerateInterpolator());
    }

    public static Animation outToLeftAnimation(android.view.animation.Interpolator i) {
        Animation outtoLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        outtoLeft.setDuration(250);
        outtoLeft.setInterpolator(i);
        return outtoLeft;
    }

    public static Animation inFromLeftAnimation() {
        return inFromLeftAnimation(new DecelerateInterpolator());
    }

    // for the next movement
    public static Animation inFromLeftAnimation(android.view.animation.Interpolator i) {
        Animation inFromLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromLeft.setDuration(250);
        inFromLeft.setInterpolator(i);
        return inFromLeft;
    }

    public static Animation outToRightAnimation() {
        return outToRightAnimation(new AccelerateInterpolator());
    }

    public static Animation outToRightAnimation(android.view.animation.Interpolator i) {
        Animation outtoRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        outtoRight.setDuration(250);
        outtoRight.setInterpolator(i);
        return outtoRight;
    }

}// end class AnimationUtils
