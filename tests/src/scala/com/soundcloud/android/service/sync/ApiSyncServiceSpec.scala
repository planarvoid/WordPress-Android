package com.soundcloud.android.service.sync

import com.soundcloud.android.robolectric.DefaultSpec
import android.content.Intent
import java.util.ArrayList
import com.soundcloud.android.provider.Content

class ApiSyncServiceSpec extends DefaultSpec {
  lazy val service = new ApiSyncService()
  def uris(uri: String*) = new ArrayList[String] { uri.foreach(add(_))}

  it  should "sync content" in {
    service.doHandleIntent(new Intent(ApiSyncService.SYNC_ACTION, Content.ME_TRACKS.uri))
  }
}