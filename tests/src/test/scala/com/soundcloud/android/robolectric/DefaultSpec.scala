package com.soundcloud.android.robolectric

import com.soundcloud.android.TestApplication
import java.io.File
import com.xtremelabs.robolectric.{Robolectric, RobolectricConfig}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{OneInstancePerTest, FlatSpec}

trait DefaultSpec extends FlatSpec with RobolectricSuite with ShouldMatchers with OneInstancePerTest {
  lazy val app: TestApplication = Robolectric.application.asInstanceOf[TestApplication]

  override lazy val robolectricConfig = new RobolectricConfig(new File(".")) {
    override def getApplicationName = classOf[TestApplication].getName
  }
}