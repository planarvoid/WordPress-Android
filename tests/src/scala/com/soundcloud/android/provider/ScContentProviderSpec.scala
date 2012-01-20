package com.soundcloud.android.provider

import scala.collection.JavaConversions._
import android.net.Uri
import com.soundcloud.android.AndroidCloudAPI
import android.database.Cursor
import com.soundcloud.android.robolectric.{Utils, DefaultSpec}
import Utils._
import org.scalatest.OneInstancePerTest
import com.soundcloud.android.service.sync.SyncAdapterServiceTest
import com.soundcloud.android.model._
import android.content.ContentValues
import android.provider.BaseColumns
import com.xtremelabs.robolectric.Robolectric

class ScContentProviderSpec extends DefaultSpec with OneInstancePerTest {
  val mapper = AndroidCloudAPI.Mapper
  lazy val provider = new ScContentProvider() { onCreate() }

  lazy val favorites: CollectionHolder[Track] =
    mapper.readValue(getClass.getResourceAsStream("user_favorites.json"), classOf[Track.TrackHolder])

  lazy val activities: CollectionHolder[Activity] =
    mapper.readValue(classOf[SyncAdapterServiceTest].getResourceAsStream("incoming_1.json"), classOf[Activities])


  it should "insert and write back a user" in {
    val user = favorites.head.user
    val userUri = provider.insert(Content.USERS.uri, user.buildContentValues())
    userUri.toString should equal("content://com.soundcloud.android.provider.ScContentProvider/users/172720")

    val readUser = query(userUri, 1)(_.map(new User(_))).head

    readUser.id should equal(user.id)
    readUser.permalink should equal(user.permalink)
    readUser.avatar_url should equal(user.avatar_url)
    readUser.username should equal(user.username)
  }

  it should "insert an read back a track" in {
    val track = favorites.head
    // need to insert the user to get the track to show
    val userUri = provider.insert(Content.USERS.uri, track.getUser.buildContentValues())
    userUri should not be (null)
    val trackUri = provider.insert(Content.TRACKS.uri, track.buildContentValues())
    trackUri.toString should equal("content://com.soundcloud.android.provider.ScContentProvider/tracks/27583938")

    val readTrack = query(trackUri, 1)(_.map(new Track(_))).head

    readTrack.id should equal(track.id)
    readTrack.title should equal(track.title)
    readTrack.created_at should equal(track.created_at)
    readTrack.permalink should equal(track.permalink)
    readTrack.commentable should equal(track.commentable)
    readTrack.sharing should equal(track.sharing)
    readTrack.artwork_url should equal(track.artwork_url)
  }

  it should "insert and query user favorites" in {
    favorites should have size (15)
    insertTracks(Content.ME_FAVORITES.uri, favorites)

    val tracks = query(Content.ME_FAVORITES.uri, favorites.size)(_.map(new Track(_)))
    val users = query(Content.USERS.uri)(_.map(new User(_)))
    users should have size (14)
  }

  it should "insert and query activities" in {
    val inserted = bulkInsertModels(Content.ME_SOUND_STREAM.uri, activities)
    inserted should be (activities.size)

    query(Content.ME_SOUND_STREAM.uri,  activities.size) { cursor =>
      for (c:Cursor <- cursor) {
        c.getLong(DBHelper.Activities.CREATED_AT) should be >= (0L)
        c.getString(DBHelper.Activities.TAGS) should not be (null)
      }
    }
  }

  it should "not notify on empty bulk insert" in {
    provider.bulkInsert(Content.ME_ACTIVITIES.uri, new Array[ContentValues](0)) should be (0)
    provider.bulkInsert(Content.ME_ACTIVITIES.uri, null) should be (0)
    val resolver = Robolectric.shadowOf(Robolectric.application.getContentResolver)
    resolver.getNotifiedUris.size should be (0)
  }

  it should "provide the global Android search" in {
    insertTracks(Content.TRACKS.uri, favorites)
    val cursor = provider.query(Content.ANDROID_SEARCH_SUGGEST.uri, null, null, Array[String]("missing"), null)
    cursor.getCount should be(1)
    // TODO: does not work currently - needs MatrixCursor shadow
  }

  def insertTracks(uri: Uri, tracks: Iterable[Track]) = tracks.map { t =>
    val user = provider.insert(Content.USERS.uri, t.user.buildContentValues(false))
    val track = provider.insert(uri, t.buildContentValues())
    user should not be (null)
    track should not be (null)
    track
  }

  def insertModels(uri: Uri, models: Iterable[ScModel]) = models.map { m =>
    val model = provider.insert(uri, m.buildContentValues())
    model should not be (null)
    model
  }

  def bulkInsertModels(uri: Uri, models: Iterable[ScModel]) =
    provider.bulkInsert(uri, models.map(_.buildContentValues()).toArray)

  def query[T](uri: Uri, expectedCount: Int = -1)(fun: Cursor => T) = {
    val cursor = provider.query(uri, null, null, null, null)
    try {
      if (expectedCount >= 0) {
        cursor.getCount should be(expectedCount)
      }
      fun(cursor)
    } finally {
      cursor.close()
    }
  }
}