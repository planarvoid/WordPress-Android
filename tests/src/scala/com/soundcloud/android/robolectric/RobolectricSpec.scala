package com.soundcloud.android.robolectric

import org.scalatest.matchers.ShouldMatchers
import com.xtremelabs.robolectric.bytecode.{RobolectricClassLoader, ShadowWrangler}
import android.net.Uri__FromAndroid
import com.xtremelabs.robolectric.internal.RealObject
import com.xtremelabs.robolectric.shadows.ShadowApplication
import com.xtremelabs.robolectric.{Robolectric, ApplicationResolver, RobolectricConfig}
import java.io.File
import com.xtremelabs.robolectric.res.ResourceLoader
import org.scalatest._
import com.xtremelabs.robolectric.util.DatabaseConfig.{UsingDatabaseMap, DatabaseMap}
import com.xtremelabs.robolectric.util.{H2Map, DatabaseConfig}


trait RobolectricSpec extends FlatSpec with ShouldMatchers {
  lazy val instrumentedClass = RobolectricSpec.classLoader.bootstrap(this.getClass)
  lazy val instrumentedInstance = instrumentedClass.newInstance().asInstanceOf[RobolectricSpec]
  lazy val robolectricConfig = new RobolectricConfig(new File("."))
  lazy val defaultDatabaseMap = new H2Map

  lazy val resourceLoader = {
    new ResourceLoader(robolectricConfig.getRealSdkVersion,
      Class.forName(robolectricConfig.getRClassName),
      robolectricConfig.getResourceDirectory,
      robolectricConfig.getAssetsDirectory)
  }

  override def run(testName: Option[String],
                   reporter: Reporter,
                   stopper: Stopper,
                   filter: Filter,
                   configMap: Map[String, Any],
                   distributor: Option[Distributor],
                   tracker: Tracker) {
    if (!isInstrumented) {
      instrumentedInstance.setupApplicationState()
      instrumentedInstance.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
    } else {
      super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
    }
  }

  def createApplication() = new ApplicationResolver(robolectricConfig).resolveApplication()
  def isInstrumented = getClass.getClassLoader.getClass.getName.contains(classOf[RobolectricClassLoader].getName)

  protected def resetStaticState()  { }
  protected def bindShadowClasses() { }

  protected def setupApplicationState() {
      robolectricConfig.validate()
      Robolectric.bindDefaultShadowClasses()
      bindShadowClasses()

      Robolectric.resetStaticState()
      resetStaticState()

      DatabaseConfig.setDatabaseMap(setupDatabaseMap(getClass, defaultDatabaseMap))
      Robolectric.application = ShadowApplication.bind(createApplication(), resourceLoader)
  }

  protected def setupDatabaseMap(testClass: Class[_], map: DatabaseMap):DatabaseMap = {
    if (testClass.isAnnotationPresent(classOf[UsingDatabaseMap])) {
      val usingMap = testClass.getAnnotation(classOf[UsingDatabaseMap])
      if (usingMap.value() != null) {
        Robolectric.newInstanceOf(usingMap.value())
      } else if (map == null) {
        throw new RuntimeException("UsingDatabaseMap annotation value must provide a class implementing DatabaseMap")
      } else {
        map
      }
    } else {
      map
    }
  }
}

object RobolectricSpec {
  lazy val classHandler = ShadowWrangler.getInstance
  lazy val classLoader = {
    val loader = new RobolectricClassLoader(classHandler)
    loader.delegateLoadingOf("org.scalatest.")
    loader.delegateLoadingOf("org.mockito.")
    loader.delegateLoadingOf("scala.")

    List(
      classOf[Uri__FromAndroid],
      classOf[RobolectricSpec],
      classOf[RealObject],
      classOf[ShadowWrangler],
      classOf[RobolectricConfig],
      classOf[DatabaseMap],
      classOf[android.R]
    ).foreach {
      classToDelegate => loader.delegateLoadingOf(classToDelegate.getName)
    }
    loader
  }
}