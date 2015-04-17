package com.soundcloud.android.profile;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiObjectContentRequest;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.api.TestApiResponses;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class UpdateAgeCommandTest {

    @Test
    public void shouldSendUpdateRequestToApiMobile() throws Exception {
        ApiClient apiClient = mock(ApiClient.class, RETURNS_SMART_NULLS);
        ApiResponse response = TestApiResponses.ok();

        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(response);

        BirthdayInfo info = BirthdayInfo.buildFrom(10, 1972);
        UpdateAgeCommand command = new UpdateAgeCommand(apiClient).with(info);
        assertThat(command.call(), is(true));

        ArgumentCaptor<ApiRequest> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClient).fetchResponse(captor.capture());

        Map<String, Integer> content = (Map) ((ApiObjectContentRequest) captor.getValue()).getContent();
        expect(content.get("month")).toEqual(info.month);
        expect(content.get("year")).toEqual(info.year);
    }
}
