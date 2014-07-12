package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PendingRecordingItemPresenterTest {
    @Mock private LayoutInflater inflater;
    @Mock private File file;
    private PendingRecordingItemPresenter presenter;

    private View itemView;
    private Recording recording;
    private List<Recording> recordings;

    @Before
    public void setUp() throws Exception {
        when(file.exists()).thenReturn(true);
        when(file.lastModified()).thenReturn(System.currentTimeMillis() - (15 * 60 * 1000));
        final Context context = Robolectric.application;
        itemView = LayoutInflater.from(context).inflate(R.layout.recording_list_item, new FrameLayout(context), false);
        recording = TestHelper.getModelFactory().createModel(Recording.class);
        recording.audio_path = file;
        recordings = Arrays.asList(recording);
        presenter = new PendingRecordingItemPresenter(context.getResources(), inflater);
    }

    @Test
    public void shouldShowPendingUploadInHeader() {
        presenter.bindItemView(0, itemView, recordings);
        expect(textView(R.id.list_item_header).getText()).toEqual("Pending Upload");
    }

    @Test
    public void shouldBindDurationToView() {
        presenter.bindItemView(0, itemView, recordings);

        expect(textView(R.id.list_item_right_info).getText()).toEqual("1:26");
    }

    @Test
    public void shouldShowPrivateIndicatorIfTrackIsPrivate() {
        recording.setIsPrivate(true);
        presenter.bindItemView(0, itemView, recordings);

        expect(textView(R.id.private_indicator).getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldHidePrivateIndicatorIfTrackIsPublic() {
        recording.setIsPrivate(false);
        presenter.bindItemView(0, itemView, recordings);

        expect(textView(R.id.private_indicator).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldShowTimeSinceRecording() {
        presenter.bindItemView(0, itemView, recordings);

        expect(textView(R.id.time_since_recorded).getText()).toEqual("15 minutes ago");
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}