package com.soundcloud.android.comments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.comments.AddCommentDialogFragment.CommentAddedSubscriber;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.app.Activity;
import android.content.Intent;

public class CommentAddedSubscriberTest extends AndroidUnitTest {

    CommentAddedSubscriber commentAddedSubscriber;

    @Mock private Activity activity;
    private Urn track = Urn.forTrack(123);
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        commentAddedSubscriber = new CommentAddedSubscriber(activity, track, eventBus);
    }

    @Test
    public void onUndoSendsCollapsePlayerCommand() throws Exception {
        commentAddedSubscriber.onUndo(null);

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isCollapse()).isTrue();
    }

    @Test
    public void onUndoPublishesViewCommentTrackingEvent() throws Exception {
        commentAddedSubscriber.onUndo(null);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.KIND_PLAYER_CLOSE);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getAttributes().get("method")).isEqualTo(UIEvent.METHOD_COMMENTS_OPEN_FROM_ADD_COMMENT);
    }

    @Test
    public void collapsedEventAfterOnUndoStartsCommentsActivity() throws Exception {
        commentAddedSubscriber.onUndo(null);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(intentArgumentCaptor.capture());

        final Intent value = intentArgumentCaptor.getValue();
        assertThat(value.getComponent().getClassName()).isEqualTo(TrackCommentsActivity.class.getName());
        assertThat(value.getParcelableExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK_URN)).isEqualTo(track);

    }
}
