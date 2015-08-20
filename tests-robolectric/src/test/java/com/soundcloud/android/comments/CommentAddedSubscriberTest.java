package com.soundcloud.android.comments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.comments.AddCommentDialogFragment.CommentAddedSubscriber;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.app.Activity;
import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class CommentAddedSubscriberTest {

    CommentAddedSubscriber commentAddedSubscriber;

    @Mock private Activity activity;
    private PropertySet track = TestPropertySets.expectedTrackForPlayer();
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        commentAddedSubscriber = new CommentAddedSubscriber(activity, track, eventBus);
    }

    @Test
    public void onUndoSendsCollapsePlayerCommand() throws Exception {
        commentAddedSubscriber.onUndo(null);

        expect(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isCollapse()).toBeTrue();
    }

    @Test
    public void onUndoPublishesViewCommentTrackingEvent() throws Exception {
        commentAddedSubscriber.onUndo(null);

        expect(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).toEqual(UIEvent.KIND_PLAYER_CLOSE);
        expect(eventBus.lastEventOn(EventQueue.TRACKING).getAttributes().get("method")).toEqual(UIEvent.METHOD_COMMENTS_OPEN_FROM_ADD_COMMENT);
    }

    @Test
    public void collapsedEventAfterOnUndoStartsCommentsActivity() throws Exception {
        commentAddedSubscriber.onUndo(null);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(intentArgumentCaptor.capture());

        final Intent value = intentArgumentCaptor.getValue();
        expect(value.getComponent().getClassName()).toEqual(TrackCommentsActivity.class.getName());
        expect(value.getParcelableExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK)).toBe(track);

    }
}