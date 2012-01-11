package com.soundcloud.android.service.sync

import com.soundcloud.android.provider.Content
import com.soundcloud.android.robolectric.{Utils, DefaultSpec}
import Utils._

class ApiSyncerSpec extends DefaultSpec {
  lazy val syncer = new ApiSyncer(app)

  it should "sync content (/me/tracks), empty" in {
    respond("/me/tracks/ids?linked_partitioning=1", Response(200, """{ "collection": [] }"""))
    syncer.syncContent(Content.ME_TRACKS)
  }


  it should "sync content (/me/tracks), with ids" in {
    respond("/me/tracks/ids?linked_partitioning=1", Response(200, """{ "collection": [ 1, 2, 3 ] }"""))
    syncer.syncContent(Content.ME_TRACKS)
  }
}