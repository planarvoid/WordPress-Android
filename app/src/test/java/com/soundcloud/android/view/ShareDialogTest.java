package com.soundcloud.android.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import rx.observers.TestSubscriber;

import android.app.Dialog;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;

public class ShareDialogTest extends AndroidUnitTest {

    private Context context;
    private TestSubscriber<Void> subscriber;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.setupActivity(FragmentActivity.class);
        subscriber = new TestSubscriber<>();
    }

    @Test
    public void showsDialog() throws Exception {
        ShareDialog shareDialog = ShareDialog.show(context);
        assertThat(shareDialog.getDialog().isShowing()).isTrue();
    }

    @Test
    public void backButtonDismisses() throws Exception {
        ShareDialog shareDialog = ShareDialog.show(context);
        Dialog dialog = shareDialog.getDialog();
        dialog.onBackPressed();
        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    public void backButtonEmitsAnItemToExistingSubscribersAndCompletes() throws Exception {
        ShareDialog shareDialog = ShareDialog.show(context);
        shareDialog.onCancelObservable().subscribe(subscriber);
        shareDialog.getDialog().onBackPressed();
        subscriber.assertValueCount(1);
        subscriber.assertCompleted();
    }

    @Test
    public void backButtonEmitsAnItemToFutureSubscribersAndCompletes() throws Exception {
        ShareDialog shareDialog = ShareDialog.show(context);
        shareDialog.getDialog().onBackPressed();
        shareDialog.onCancelObservable().subscribe(subscriber);
        subscriber.assertValueCount(1);
        subscriber.assertCompleted();
    }

    @Test
    public void dismissDoesNotEmitAndItemToExistingSubscribersButCompletes() throws Exception {
        ShareDialog shareDialog = ShareDialog.show(context);
        shareDialog.onCancelObservable().subscribe(subscriber);
        shareDialog.dismiss();
        subscriber.assertValueCount(0);
        subscriber.assertCompleted();
    }

    @Test
    public void dismissDoesNotEmitAndItemToFutureSubscribersButCompletes() throws Exception {
        ShareDialog shareDialog = ShareDialog.show(context);
        shareDialog.dismiss();
        shareDialog.onCancelObservable().subscribe(subscriber);
        subscriber.assertValueCount(0);
        subscriber.assertCompleted();
    }

    @Test
    public void touchOutsideDialogDoesNotDismiss() throws Exception {
        ShareDialog shareDialog = ShareDialog.show(context);
        shareDialog.getDialog().onTouchEvent(MotionEvent.obtain(1, 1, MotionEvent.ACTION_DOWN, -100, -100, 0));
        assertThat(shareDialog.getDialog().isShowing()).isTrue();
    }

    @Test
    public void touchOutsideDialogDoesNotEmitAnItemToSubscribersAndDoesNotComplete() throws Exception {
        ShareDialog shareDialog = ShareDialog.show(context);
        shareDialog.onCancelObservable().subscribe(subscriber);
        shareDialog.getDialog().onTouchEvent(MotionEvent.obtain(1, 1, MotionEvent.ACTION_DOWN, -100, -100, 0));
        subscriber.assertValueCount(0);
        subscriber.assertNotCompleted();
    }
}
