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

class ScContentProviderSpec extends DefaultSpec with OneInstancePerTest {
  val mapper = AndroidCloudAPI.Mapper
  lazy val provider = new ScContentProvider() { onCreate() }

  lazy val favorites: CollectionHolder[Track] =
    mapper.readValue(getClass.getResourceAsStream("user_favorites.json"), classOf[Track.TrackHolder])

  lazy val activities: CollectionHolder[Activity] =
    mapper.readValue(classOf[SyncAdapterServiceTest].getResourceAsStream("incoming_1.json"), classOf[Activities])

  def insertTracks(tracks: Iterable[Track], uri: Uri) {
    for (t <- tracks) {
      val user  = provider.insert(Content.USERS.uri, t.user.buildContentValues(false))
      val track = provider.insert(uri, t.buildContentValues())
      user should not be (null)
      track should not be (null)
    }
  }

  def insertModels(models: Iterable[ScModel], uri: Uri) = {
    models.map { m =>
      val model = provider.insert(uri, m.buildContentValues())
      model should not be (null)
      model
    }
  }

  def query[T](uri: Uri)(fun: Cursor => T) = {
    val cursor = provider.query(uri, null, null, null, null)
    try {
      fun(cursor)
    } finally {
      cursor.close()
    }
  }

  it should "insert and write back a user" in {
    val user = favorites.head.user
    val userUri = provider.insert(Content.USERS.uri, user.buildContentValues())
    userUri.toString should equal("content://com.soundcloud.android.provider.ScContentProvider/users/172720")

    val readUser = query(userUri) { c =>
      c.getCount should equal(1)
      c.map(new User(_))
    }.head

    readUser.id should equal(user.id)
    readUser.permalink should equal(user.permalink)
    readUser.avatar_url should equal(user.avatar_url)
    readUser.username should equal(user.username)
  }

  it should "insert an read back a track" in {
    val track = favorites.head
    // need to insert the user to get the track to show
    val userUri = provider.insert(Content.USERS.uri, track.getUser.buildContentValues())
    userUri should not be(null)
    val trackUri = provider.insert(Content.TRACKS.uri, track.buildContentValues())
    trackUri.toString should equal("content://com.soundcloud.android.provider.ScContentProvider/tracks/27583938")

    val readTrack = query(trackUri) { c =>
      c.getCount should equal(1)
      c.map(new Track(_))
    }.head

    readTrack.id should equal(track.id)
    readTrack.title should equal(track.title)
    readTrack.created_at should equal(track.created_at)
    readTrack.permalink should equal(track.permalink)
    readTrack.commentable should equal(track.commentable)
    readTrack.sharing should equal(track.sharing)
    readTrack.artwork_url should equal(track.artwork_url)
  }

  it should "insert and query user favorites" in {
    favorites.size should equal(15)

    insertTracks(favorites, Content.ME_FAVORITES.uri)

    val tracks = query(Content.ME_FAVORITES.uri) { c =>
      c.getCount should equal(favorites.size)
      c.map(new Track(_))
    }

    tracks.size should equal(favorites.size)

    val users = query(Content.USERS.uri)(_.map(new User(_)))
    users.size should equal(14)
  }

  it should "insert and query activities" in {
    val models = insertModels(activities, Content.ME_SOUND_STREAM.uri)
  }
}