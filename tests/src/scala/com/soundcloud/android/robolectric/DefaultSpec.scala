package com.soundcloud.android.robolectric

import com.soundcloud.android.TestApplication
import java.io.File
import com.xtremelabs.robolectric.{Robolectric, RobolectricConfig}
import com.xtremelabs.robolectric.util.DatabaseConfig.DatabaseMap
import com.xtremelabs.robolectric.util.DatabaseConfig
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{OneInstancePerTest, FlatSpec}

trait DefaultSpec extends FlatSpec with RobolectricSuite with ShouldMatchers with OneInstancePerTest {
  lazy val app: TestApplication = Robolectric.application.asInstanceOf[TestApplication]

  lazy val sqliteMap: DatabaseConfig.DatabaseMap = new SQLiteMap

  override lazy val robolectricConfig = new RobolectricConfig(new File(".")) {
    override def getApplicationName = classOf[TestApplication].getName
  }

  override protected def setupDatabaseMap(testClass: Class[_], map: DatabaseMap) = sqliteMap

  override protected def bindShadowClasses() {
    Robolectric.bindShadowClass(classOf[DefaultTestRunner.ShadowLog])
    Robolectric.bindShadowClass(classOf[ShadowHandlerThread])
  }
}