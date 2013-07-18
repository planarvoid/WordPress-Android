package com.soundcloud.android.view;

import static java.lang.Math.max;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.ScLandingPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

/**
 * Container for all activity content views plus the fly-in menu.
 *
 * Touch Handling pulled from the Android ICS ScrollView class
*/
public class RootView extends ViewGroup {
    private static final float MAXIMUM_MINOR_VELOCITY   = 150.0f;
    private static final float MAXIMUM_MAJOR_VELOCITY   = 200.0f;
    private static final float MAXIMUM_ACCELERATION     = 2000.0f;
    private static final float DROP_SHADOW_WIDTH        = 10.0f;

    private static final int INVALID_POINTER            = -1;
    private static final int VELOCITY_UNITS             = 1000;
    private static final int MSG_ANIMATE                = 1000;
    private static final int ANIMATION_DURATION         = 300;
    private static final int MENU_TARGET_WIDTH          = 230;
    private static final int BEZEL_HIT_WIDTH            = 30;
    private static final int OFFSET_RIGHT               = 60;   // action bar home icon mRight
    private static final int MAXIMUM_OVERLAY_ALPHA      = 180;  // max alpha of the menu dimmers

    private static final float PARALLAX_SPEED_RATIO = 0.5f;

    private static int mExpandedState; // YES, THIS NEEDS TO BE STATIC
    private static final int EXPANDED_LEFT              = 100000;
    private static final int COLLAPSED_FULL_CLOSED      = 100001;


    public static final String EXTRA_ROOT_VIEW_STATE    = "fim_menu_state";

    private static final String KEY_MENU_STATE          = "menuState_key";
    private static final String STATE_KEY               = "state_key";
    private static final String BLOCK_KEY               = "block_key";

    private boolean mIsBlocked, mShouldTrackGestures;
    private MainMenu mMenu;
    private ViewGroup mContent;
    private View mBlocker;

    private boolean mIsBeingDragged;
    private boolean mAnimating;
    private boolean mCloseOnResume;

    private @Nullable VelocityTracker mVelocityTracker;

    private OnMenuStateListener mOnMenuStateListener;
    private int mTouchDelta;

    private final Scroller mScroller;
    private final Paint mOverlayPaint;

    private final GradientDrawable mShadowLeftDrawable;
    private final GradientDrawable mShadowRightDrawable;

    private final int mMaximumMinorVelocity;
    private final int mMaximumMajorVelocity;
    private final int mMaximumAcceleration;
    private final int mVelocityUnits;

    private int mOffsetRight;
    private final int mDrowShadoWidth;
    private final int mBezelHitWidth;
    private int mActivePointerId;
    private float mLastMotionX, mLastMotionY;
    private int mTouchSlop;

    /**
     * Callback invoked when the menu is opened.
     */
    public static interface OnMenuStateListener {

        /**
         * Invoked when the menu becomes fully open.
         */
        public void onMenuOpenLeft();

        /**
         * Invoked when the menu becomes fully closed.
         */
        public void onMenuClosed();

        /**
         * Invoked when the user clicks the blocked view
         */
        public void onBlockerClick();

    }
    /**
     * A slide in navigation menu.
     *
     * Based on the Android SlidingDrawer component, with additional functionality
     * and ideas credited to http://android.cyrilmottier.com/?p=658
     */
    public RootView(Context context, Drawable contentBg, int selectedMenuId) {
        super(context, null, 0);

        View.inflate(context, R.layout.root_view, this);

        setId(R.id.root_view_id);

        mBlocker = findViewById(R.id.blocker);
        mBlocker.setVisibility(View.GONE);
        mBlocker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnMenuStateListener != null) mOnMenuStateListener.onBlockerClick();
            }
        });
        mIsBlocked = false;
        mShouldTrackGestures = context instanceof ScLandingPage;

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mMenu = (MainMenu) findViewById(R.id.root_menu);
        mMenu.setSelectedMenuId(selectedMenuId);

        mContent = (ViewGroup) findViewById(R.id.content_frame);
        mContent.setBackgroundDrawable(contentBg);

        mScroller = new Scroller(context,new DecelerateInterpolator());

        final float density = getResources().getDisplayMetrics().density;

        mMaximumMinorVelocity = (int) (MAXIMUM_MINOR_VELOCITY * density + 0.5f);
        mMaximumMajorVelocity = (int) (MAXIMUM_MAJOR_VELOCITY * density + 0.5f);
        mMaximumAcceleration  = (int) (MAXIMUM_ACCELERATION * density + 0.5f);
        mVelocityUnits        = (int) (VELOCITY_UNITS * density + 0.5f);

        mOffsetRight   = (int) (OFFSET_RIGHT * density + 0.5f);
        mBezelHitWidth = (int) (BEZEL_HIT_WIDTH * density + 0.5f);

        mOverlayPaint = new Paint();
        mOverlayPaint.setColor(Color.BLACK);

        mDrowShadoWidth = (int) (DROP_SHADOW_WIDTH * density);
        mShadowLeftDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0x00000000,0x8F000000});
        mShadowRightDrawable = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,new int[]{0x00000000,0x8F000000});

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        if (mExpandedState == 0) mExpandedState = COLLAPSED_FULL_CLOSED;
        mActivePointerId = INVALID_POINTER;
        setAlwaysDrawnWithCacheEnabled(false);
    }

    public void setCloseOnResume(boolean closeOnResume){
        mCloseOnResume = closeOnResume;
    }

    public void block(){
        if (!mIsBlocked){
            mIsBlocked = true;
            mBlocker.setVisibility(View.VISIBLE);
            mBlocker.clearAnimation();
            mBlocker.setEnabled(true);
            mBlocker.setClickable(true);
            Animation animation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
            animation.setDuration(200);
            mBlocker.startAnimation(animation);
        }
    }

    public void unBlock(boolean instant){
        if (mIsBlocked) {
            mIsBlocked = false;
            mBlocker.clearAnimation();
            mBlocker.setClickable(false);
            mBlocker.setEnabled(false);
            if (instant){
                mBlocker.setVisibility(View.GONE);
            } else {
                Animation animation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
                animation.setDuration(200);
                mBlocker.startAnimation(animation);
                mBlocker.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                        if (mBlocker.getAnimation() == animation) {
                            mBlocker.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }
    }

    public int getContentHolderId() {
        if (mContent != null) {
            return mContent.getId();
        }
        return -1;
    }

    public void setContent(View content){
        if (mContent.getChildCount() > 0) mContent.removeAllViews();

        content.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mContent.addView(content);
    }

    public void setSelectedMenuId(int i) {
        mMenu.setSelectedMenuId(i);
    }

    public void configureMenu(int menuResourceId, final MainMenu.OnMenuItemClickListener listener) {
        mMenu.setMenuItems(menuResourceId);
        mMenu.setOnItemClickListener(listener);
    }

    public Bundle getMenuBundle() {
        Bundle bundle = new Bundle();
        SparseArray<Parcelable> container = new SparseArray<Parcelable>();
        mMenu.saveHierarchyState(container);
        bundle.putSparseParcelableArray(RootView.KEY_MENU_STATE, container);
        bundle.putInt(RootView.STATE_KEY,mExpandedState);
        bundle.putBoolean(RootView.BLOCK_KEY, mIsBlocked);

        return bundle;
    }

    public void restoreStateFromExtra(Bundle state) {
        mMenu.restoreHierarchyState(state.getSparseParcelableArray(KEY_MENU_STATE));
        mExpandedState = state.getInt(STATE_KEY);

        if (mExpandedState != COLLAPSED_FULL_CLOSED) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    animateClose();
                }
            }, 100); // post on delay to avoid animation jank at start of activity
        }

        mIsBlocked = state.getBoolean(RootView.BLOCK_KEY);
        mBlocker.setVisibility(mIsBlocked ? View.VISIBLE : View.GONE);
        mBlocker.setEnabled(mIsBlocked);
    }


    /**
     * global expansion state may have changed in another activity. make sure we are showing the correct state
     */
    public void onResume() {
        if (mCloseOnResume) {
            mCloseOnResume = false;
            mExpandedState = COLLAPSED_FULL_CLOSED;
        }
        mMenu.onResume();
        setExpandedState();
    }

    public void onDestroy() {
        mMenu.onDestroy();
    }

    private void setExpandedState() {
        if (!mAnimating && !mIsBeingDragged) {
            switch (mExpandedState) {
                case COLLAPSED_FULL_CLOSED:
                    setClosed();
                    break;

                case EXPANDED_LEFT:
                    openLeft();
                    break;
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.expanded = mExpandedState;
        ss.blocked = mIsBlocked;
        ss.closeOnResume = mCloseOnResume;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null) return;

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mExpandedState = ss.expanded;
        mCloseOnResume = ss.closeOnResume;
        mIsBlocked = ss.blocked;
        mBlocker.setVisibility(mIsBlocked ? View.VISIBLE : View.GONE);
        mBlocker.setEnabled(mIsBlocked);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("FlyInMenu cannot have UNSPECIFIED dimensions");
        }

        if (mContent != null){
            measureChild(mContent, widthMeasureSpec, heightMeasureSpec);
        }

        if (mMenu != null){
            mMenu.measure(MeasureSpec.makeMeasureSpec(widthSpecSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSpecSize, MeasureSpec.EXACTLY));
        }

        mShadowLeftDrawable.setBounds(0, 0, mDrowShadoWidth, getHeight());
        mShadowRightDrawable.setBounds(0, 0, mDrowShadoWidth, getHeight());

        setMeasuredDimension(widthSpecSize, heightSpecSize);

        final float density = getResources().getDisplayMetrics().density;
        mOffsetRight = (int) max(widthSpecSize - MENU_TARGET_WIDTH * density + 0.5f,
                                 OFFSET_RIGHT * density + 0.5f);
        mMenu.setOffsetRight(mOffsetRight);
    }



    public boolean isExpanded(){
        return mExpandedState != COLLAPSED_FULL_CLOSED;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final long drawingTime = getDrawingTime();

        final int width = getWidth();
        float openRatio = (float) mContent.getLeft() / (width - mOffsetRight) ;

        float closedRatio = 1 - openRatio;

        if (mIsBeingDragged || mAnimating || isExpanded()) {


            final int alpha = (int) (MAXIMUM_OVERLAY_ALPHA * (closedRatio));
            if (alpha > 0) mOverlayPaint.setColor(Color.argb(alpha, 0, 0, 0));

            if (mContent.getLeft() > 0){

                // menu
                final int left = mMenu.getLeft();
                final int offset = Math.min(-left, (int) (-closedRatio * (width - mOffsetRight) * PARALLAX_SPEED_RATIO) - left);
                mMenu.offsetLeftAndRight(offset);

                final Bitmap menuCache = mMenu.getDrawingCache();
                if (menuCache != null) {
                    canvas.drawBitmap(menuCache, mMenu.getLeft(), 0, null);
                } else {
                    canvas.save();
                    drawChild(canvas, mMenu, drawingTime);
                    canvas.restore();
                }

                // menuOverlay
                if (alpha > 0) canvas.drawRect(0, 0, mMenu.getRight(), getHeight(), mOverlayPaint);

                // gradient
                canvas.save();
                canvas.translate(mContent.getLeft() - mDrowShadoWidth, 0);
                mShadowLeftDrawable.draw(canvas);
                canvas.restore();
            }

            // content
            final Bitmap contentCache = mContent.getDrawingCache();
            if (contentCache != null) {
                canvas.drawBitmap(contentCache, mContent.getLeft(), 0, null);
            } else {
                canvas.save();
                drawChild(canvas, mContent, drawingTime);
                canvas.restore();
            }

        } else {
            drawChild(canvas, mContent, drawingTime);
        }

        if (mBlocker.getVisibility() == View.VISIBLE) drawChild(canvas, mBlocker,drawingTime);

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mIsBeingDragged) {
            return;
        }

        if (changed){
            setExpandedState();
        }

        final int width = getWidth();
        final int height = getHeight();
        mMenu.layout(0, 0, width, height);
        mContent.layout(mContent.getLeft(), 0, mContent.getLeft() + width, height);
        mBlocker.layout(0, 0, width, height);
    }


    private boolean checkShouldTrack(int x){
        if (x >= mContent.getLeft() && x <= mContent.getRight()) {
            switch (mExpandedState) {
                case COLLAPSED_FULL_CLOSED:
                    return x < mContent.getLeft() + mBezelHitWidth;

                case EXPANDED_LEFT:
                    return x < getRight();
            }
        }
        return false;
    }

    /*
        touch handling from ICS ScrollView class
    */

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
        * This method JUST determines whether we want to intercept the motion.
        * If we return true, onMotionEvent will be called and we do the actual
        * scrolling there.
        */

        /*
        * Shortcut the most recurring case: the user is in the dragging
        * state and he is moving his finger.  We want to intercept this
        * motion.
        */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                /*
                * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                * whether the user has moved far enough from his original down touch.
                */

                /*
                * Locally do absolute value. mLastMotionX is set to the x value
                * of the down event.
                */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                VelocityTracker tracker = initVelocityTrackerIfNotExists();
                tracker.addMovement(ev);

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex >= 0 && pointerIndex < ev.getPointerCount()) {
                    final float x = ev.getX(pointerIndex);
                    final int xDiff = (int) Math.abs(x - mLastMotionX);
                    if (xDiff > mTouchSlop) {
                        return startDrag(ev, (int) x, (int) ev.getY(pointerIndex));
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                if (!mShouldTrackGestures || mIsBlocked || !checkShouldTrack(x)) {
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }

                if (isExpanded()){
                    return startDrag(ev, x, (int) ev.getY());
                }

                /*
                * Remember location of down touch.
                * ACTION_DOWN always refers to pointer index 0.
                */
                mLastMotionX = x;
                mLastMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);

                VelocityTracker tracker = initOrResetVelocityTracker();
                tracker.addMovement(ev);
                /*
                * If being flinged and user touches the screen, initiate drag;
                * otherwise don't.  mScroller.isFinished should be false when
                * being flinged.
                */
                mIsBeingDragged = !mScroller.isFinished();
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /* Release the drag */
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                break;
        }

        /*
        * The only time we want to intercept motion events is if we are in the
        * drag mode.
        */
        return mIsBeingDragged;
    }

    private boolean startDrag(MotionEvent ev, int eventX, int eventY) {

        mIsBeingDragged = true;
        mLastMotionX = eventX;
        mLastMotionY = eventY;

        // Must be called before prepareTracking()
        prepareContent();

        final int left = mContent.getLeft();
        mTouchDelta = eventX - left;
        prepareTracking(left);

        return true;
    }

    private @NotNull VelocityTracker initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
        return mVelocityTracker;
    }

    private @NotNull VelocityTracker initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        return mVelocityTracker;
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                final int x = (int) event.getX(0);

                /*
                 This event should only happen if there are no touch targets beneath the touch.
                 Otherwise, the drag logic should happen above, in onInterceptTouchEvent
                */
                mIsBeingDragged = getChildCount() != 0 && checkShouldTrack(x);
                if (!mIsBeingDragged) {
                    return false;
                }

                /*
                * If being flinged and user touches, stop the fling. isFinished
                * will be false if being flinged.
                */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mActivePointerId = event.getPointerId(0);
                startDrag(event, x, (int) event.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsBeingDragged) {
                    VelocityTracker tracker = initVelocityTrackerIfNotExists();
                    tracker.addMovement(event);
                    moveContent((int) (event.getX()) - mTouchDelta);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mExpandedState != COLLAPSED_FULL_CLOSED
                        && Math.sqrt(Math.pow(mLastMotionX - event.getX(),2) + Math.pow(mLastMotionY - event.getY(),2)) < mTouchSlop){

                    animateClose();
                } else {
                    VelocityTracker tracker = initVelocityTrackerIfNotExists();
                    tracker.computeCurrentVelocity(mVelocityUnits);
                    performFling(mContent.getLeft(), tracker.getXVelocity(), -1);
                }

                mActivePointerId = INVALID_POINTER;
                mIsBeingDragged = false;
                recycleVelocityTracker();
            }
            break;
        }

        return true;
    }



    private void animateClose(int position) {
        prepareTracking(position);
        performFling(position, -mMaximumAcceleration, 1);
    }

    private void animateMenuOpen(int position) {
        prepareTracking(position);
        performFling(position, mMaximumAcceleration, 0);
    }

    private void performFling(int position, float velocity, int forceState) {
        final int motion;
        if (mExpandedState == EXPANDED_LEFT) {

            if (forceState == -1 && (velocity > mMaximumMajorVelocity ||
                    (position > (getWidth() - mOffsetRight)/2 && velocity > -mMaximumMajorVelocity))) {

                // We are expanded, but they didn't move sufficiently to cause
                // us to retract.  Animate back to the expanded position.
                motion = getWidth() - position - mOffsetRight;

            } else {
                // We are expanded and are now going to animate away.
                motion = -position;
            }
        } else {
            final boolean shouldOpenLeft = velocity > mMaximumMajorVelocity ||
                    (position > (getWidth() - mOffsetRight) / 2 &&
                            velocity > -mMaximumMajorVelocity);

            if (forceState == EXPANDED_LEFT || shouldOpenLeft){
                motion = getWidth() - position - mOffsetRight;

            } else  {
                // We are collapsed, but they didn't move sufficiently to cause
                // us to open either way.  Animate back to the collapsed position.
                motion = -position;
            }
        }

        mScroller.startScroll(position, 0, motion, 0, ANIMATION_DURATION);
        mAnimating = true;
        ViewCompat.postInvalidateOnAnimation(this);
        stopTracking();
    }



    private void prepareTracking(int position) {
        mAnimating = false;
        mIsBeingDragged = true;
        mVelocityTracker = VelocityTracker.obtain();
        moveContent(isExpanded() ? position : 0);
    }

    private void moveContent(int position) {
        final int expandedLeftContentPos = getMeasuredWidth() - mOffsetRight;
        if (position == EXPANDED_LEFT) {
            final int togo = expandedLeftContentPos - mContent.getLeft();
            mContent.offsetLeftAndRight(togo); //todo proper value
        } else if (position == COLLAPSED_FULL_CLOSED) {
            mContent.offsetLeftAndRight(0 - mContent.getLeft());

        } else {
            final int left = mContent.getLeft();

            if (position > expandedLeftContentPos){
                position = (int) (expandedLeftContentPos + (position - expandedLeftContentPos) * .5);
            } else if (position < 0){
                position = position / 4;
            }

            int deltaX = position - left;
            mContent.offsetLeftAndRight(deltaX);
        }
        invalidate();
    }

    @TargetApi(11)
    private void prepareContent() {
        if (mAnimating) {
            return;
        }

        // Something changed in the content, we need to honor the layout request
        // before creating the cached bitmap
        final View content = mContent;

        // Try only once... we should really loop but it's not a big deal
        // if the draw was cancelled, it will only be temporary anyway
        content.getViewTreeObserver().dispatchOnPreDraw();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && !content.isHardwareAccelerated()) {
            content.buildDrawingCache();
        }

        content.setVisibility(View.GONE);
    }

    private void stopTracking() {
        mIsBeingDragged = false;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mContent.offsetLeftAndRight(mScroller.getCurrX() - mContent.getLeft());
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            if (mAnimating && mScroller.isFinished()) {
                if (mContent.getLeft() >= (getWidth() - mOffsetRight) / 2) {
                    openLeft();
                } else {
                    setClosed();
                }
                mAnimating = false;
            }
            super.computeScroll();
        }
    }

    private boolean isMenuOpen() {
        return (mExpandedState == EXPANDED_LEFT);
    }

    /**
     * Toggles the menu open and close with an animation.
     */
    public void animateToggleMenu() {
        if (!isMenuOpen()) {
            animateMenuOpen();
        } else {
            animateClose();
        }
    }

    /**
     * Closes the menu immediately.
     */
    public void close() {
        setClosed();
        invalidate();
        requestLayout();
    }

    public void onBack() {
        animateClose();
    }


    /**
     * Closes the menus with an animation.
     */
    public void animateClose() {
        prepareContent();
        animateClose(mContent.getLeft());
    }

    /**
     * Opens the menu with an animation.
     */
    public void animateMenuOpen() {
        prepareContent();
        animateMenuOpen(mContent.getLeft());
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }


    private void setClosed() {
        moveContent(COLLAPSED_FULL_CLOSED);
        mContent.destroyDrawingCache();

        mContent.setVisibility(View.VISIBLE);
        mMenu.setVisibility(View.GONE);

        mExpandedState = COLLAPSED_FULL_CLOSED;
        if (mOnMenuStateListener != null) mOnMenuStateListener.onMenuClosed();

    }

    private void openLeft() {
        moveContent(EXPANDED_LEFT);
        mMenu.setVisibility(View.VISIBLE);
        mContent.setVisibility(View.GONE);
        invalidate();

        mExpandedState = EXPANDED_LEFT;
        if (mOnMenuStateListener != null) mOnMenuStateListener.onMenuOpenLeft();
    }

    /**
     * Sets the listener that receives a notification when the menu state changes.
     *
     * @param onMenuStateListener The listener to be notified when the menu state changes.
     */
    public void setOnMenuStateListener(OnMenuStateListener onMenuStateListener) {
        mOnMenuStateListener = onMenuStateListener;
    }

    /**
     * Returns the handle of the menu.
     *
     * @return The View representing the handle of the menu, identified by
     *         the "handle" id in XML.
     */
    public MainMenu getMenu() {
        return mMenu;
    }

    /**
     * Returns the content of the menu.
     *
     * @return The View representing the content of the menu, identified by
     *         the "content" id in XML.
     */
    public View getContent() {
        return mContent;
    }

    /**
     * Indicates whether the menu is currently fully opened.
     *
     * @return True if the menu is opened, false otherwise.
     */
    public boolean isOpened() {
        return mExpandedState != COLLAPSED_FULL_CLOSED;
    }

    /**
     * Indicates whether the menu is scrolling or flinging.
     *
     * @return True if the menu is scroller or flinging, false otherwise.
     */
    public boolean isMoving() {
        return mIsBeingDragged || mAnimating;
    }

    static class SavedState extends BaseSavedState {
        int expanded;
        boolean blocked;
        boolean closeOnResume;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.expanded = in.readInt();
            this.blocked = in.readInt() == 1;
            this.closeOnResume = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(expanded);
            out.writeInt(blocked ? 1 : 0);
            out.writeInt(closeOnResume ? 1 : 0);
        }

        //required field that makes Parcelables from a Parcel
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}


