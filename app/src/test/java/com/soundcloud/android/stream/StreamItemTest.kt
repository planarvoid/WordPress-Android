package com.soundcloud.android.stream

import com.soundcloud.android.ads.AdFixtures
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.PlaylistFixtures
import com.soundcloud.android.testsupport.TrackFixtures
import com.soundcloud.java.optional.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class StreamItemTest {
    @Test
    fun `two track items have the same identity and different equality`() {
        val trackItem = TrackFixtures.trackItem()
        val trackStreamItem1 = TrackStreamItem(trackItem, true, Date(), Optional.absent())
        val trackStreamItem2 = TrackStreamItem(trackItem, false, Date(), Optional.absent())

        assertThat(trackStreamItem1.identityEquals(trackStreamItem2)).isTrue()
        assertThat(trackStreamItem1).isNotEqualTo(trackStreamItem2)
    }

    @Test
    fun `track item and other item have have different identity`() {
        val trackItem = TrackFixtures.trackItem()
        val trackStreamItem = TrackStreamItem(trackItem, true, Date(), Optional.absent())
        val upsellItem = StreamItem.Upsell

        assertThat(trackStreamItem.identityEquals(upsellItem)).isFalse()
    }

    @Test
    fun `two playlist items have the same identity and different equality`() {
        val playlistItem = PlaylistFixtures.playlistItem()
        val playlistStreamItem1 = PlaylistStreamItem(playlistItem, true, Date(), Optional.absent())
        val playlistStreamItem2 = PlaylistStreamItem(playlistItem, false, Date(), Optional.absent())

        assertThat(playlistStreamItem1.identityEquals(playlistStreamItem2)).isTrue()
        assertThat(playlistStreamItem1).isNotEqualTo(playlistStreamItem2)
    }

    @Test
    fun `playlist item and other item have have different identity`() {
        val playlistItem = PlaylistFixtures.playlistItem()
        val playlistStreamItem = PlaylistStreamItem(playlistItem, true, Date(), Optional.absent())
        val upsellItem = StreamItem.Upsell

        assertThat(playlistStreamItem.identityEquals(upsellItem)).isFalse()
    }

    @Test
    fun `two video ad items have the same identity and equality`() {
        val videoAd = AdFixtures.getVideoAd(Urn.forAd("ad", "123"), Urn.forTrack(1))
        val videoStreamItem1 = StreamItem.Video(videoAd)
        val videoStreamItem2 = StreamItem.Video(videoAd)

        assertThat(videoStreamItem1.identityEquals(videoStreamItem2)).isTrue()
        assertThat(videoStreamItem1).isEqualTo(videoStreamItem2)
    }

    @Test
    fun `video item and other item have have different identity`() {
        val videoAd = AdFixtures.getVideoAd(Urn.forAd("ad", "123"), Urn.forTrack(1))
        val videoStreamItem = StreamItem.Video(videoAd)
        val upsellItem = StreamItem.Upsell

        assertThat(videoStreamItem.identityEquals(upsellItem)).isFalse()
    }

    @Test
    fun `two app install ad items have the same identity and equality`() {
        val appInstallAd = AdFixtures.getApiAppInstallAd()
        val appInstallStreamItem1 = StreamItem.AppInstall(appInstallAd)
        val appInstallStreamItem2 = StreamItem.AppInstall(appInstallAd)

        assertThat(appInstallStreamItem1.identityEquals(appInstallStreamItem2)).isTrue()
        assertThat(appInstallStreamItem1).isEqualTo(appInstallStreamItem2)
    }

    @Test
    fun `app install item and other item have have different identity`() {
        val appInstallAd = AdFixtures.getApiAppInstallAd()
        val appInstallStreamItem = StreamItem.AppInstall(appInstallAd)
        val upsellItem = StreamItem.Upsell

        assertThat(appInstallStreamItem.identityEquals(upsellItem)).isFalse()
    }

    @Test
    fun `two facebook listener items have the same identity and different equality`() {
        val facebookStreamItem1 = StreamItem.FacebookListenerInvites(Optional.absent())
        val facebookStreamItem2 = StreamItem.FacebookListenerInvites(Optional.of(emptyList()))

        assertThat(facebookStreamItem1.identityEquals(facebookStreamItem2)).isTrue()
        assertThat(facebookStreamItem1).isNotEqualTo(facebookStreamItem2)
    }

    @Test
    fun `facebook listener item and other item have have different identity`() {
        val facebookStreamItem = StreamItem.FacebookListenerInvites(Optional.absent())
        val upsellItem = StreamItem.Upsell

        assertThat(facebookStreamItem.identityEquals(upsellItem)).isFalse()
    }

    @Test
    fun `two facebook creator items have the same identity and different equality`() {
        val facebookStreamItem1 = StreamItem.FacebookCreatorInvites(Urn.forTrack(1), "123")
        val facebookStreamItem2 = StreamItem.FacebookCreatorInvites(Urn.forTrack(2), "234")

        assertThat(facebookStreamItem1.identityEquals(facebookStreamItem2)).isTrue()
        assertThat(facebookStreamItem1).isNotEqualTo(facebookStreamItem2)
    }

    @Test
    fun `facebook creator item and other item have have different identity`() {
        val facebookStreamItem = StreamItem.FacebookCreatorInvites(Urn.forTrack(1), "123")
        val upsellItem = StreamItem.Upsell

        assertThat(facebookStreamItem.identityEquals(upsellItem)).isFalse()
    }
}
