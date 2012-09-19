import sbt._
import Keys._
import AndroidKeys._

import collection.JavaConverters._
import java.util.zip.ZipFile

object AmazonHelper {

    def isAmazon(f: File) = new ZipFile(f).entries.asScala.exists(_.getName.contains("com.amazon.content.id"))
    def copy(f: File) = {
        val amazon = new File(f.getParent, f.getName.replace(".apk", "-amazon.apk"))
        IO.copyFile(f, amazon)
        amazon
    }
    val prepareAmazon = TaskKey[File]("prepare-amazon")
    lazy val settings = inConfig(Android)(Seq(
        prepareAmazon      <<= (packageAlignedPath, streams) map { (path, s) =>
          if (AmazonHelper.isAmazon(path)) {
            val amazon = AmazonHelper.copy(path)
            s.log.success("Ready for Amazon appstore:\n"+amazon)
            amazon
          } else sys.error(path.getName+" is not an Amazon processed APK!")
        } dependsOn (AndroidMarketPublish.signReleaseTask, AndroidMarketPublish.zipAlignTask)
    ))
}
