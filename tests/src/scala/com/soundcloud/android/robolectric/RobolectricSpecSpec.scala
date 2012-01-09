package com.soundcloud.android.robolectric

import android.content.Intent
import android.net.Uri
import com.xtremelabs.robolectric.Robolectric
import com.xtremelabs.robolectric.util.DatabaseConfig.{DatabaseMap, UsingDatabaseMap}
import java.lang.String
import com.xtremelabs.robolectric.util.DatabaseConfig

@UsingDatabaseMap(classOf[MyMap])
class RobolectricSpecSpec extends RobolectricSpec {

  it should "initialize robolectric" in {
    val i = new Intent("foo", Uri.parse("http:/testing.com"))
    i.getAction should equal("foo")
    i.getData.toString should equal("http:/testing.com")
  }

  it should "initialize the app" in {
    Robolectric.application should not be null
  }

  it should "set the db map" in {
    DatabaseConfig.getDatabaseMap.isInstanceOf[MyMap] should be (true)
  }
}

class MyMap extends DatabaseMap {
  def getDriverClassName = ""
  def getConnectionString = ""
  def getScrubSQL(sql: String) = ""
  def getSelectLastInsertIdentity = ""
  def getResultSetType = 0
}