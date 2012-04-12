import sbt._
import Keys._
import AndroidKeys._

import collection.JavaConverters._

object CopyLibs {
    val copyLibs = TaskKey[Seq[File]]("copy-libs")
    lazy val settings = inConfig(Android)(Seq(
        copyLibs      <<= (update, preinstalledModules, streams) map { (update, preinstalled, s) =>
          val deps = update.toSeq.foldLeft(Map.empty[String,Seq[(ModuleID, File)]]) { case (map, (conf, module, art, file)) =>
            map + ((conf, map.getOrElse(conf, Nil) :+ (module, file)))
          }
          val compile = deps.getOrElse("compile", Seq.empty).toSet
          val test    = deps.getOrElse("test", Seq.empty).toSet
          val int     = deps.getOrElse("int", Seq.empty).toSet

          val justCompile = compile.filter { case (module, file) =>
            !preinstalled.exists(m => m.organization == module.organization && m.name == module.name)
          }
          val justTest = test -- justCompile

          val compileLibs = for { (_, file) <- justCompile } yield (file, new File("lib", file.getName))
          val testLibs    = for { (_, file) <- justTest } yield (file, new File("tests/lib", file.getName))
          val intLibs     = for { (_, file) <- int } yield (file, new File("tests-integration/lib", file.getName))

          (testLibs ++ compileLibs ++ intLibs).foldLeft(Seq.empty[File]) { case (files, (file, dest)) =>
            s.log.info("copying "+dest)
            IO.copyFile(file, dest)
            files :+ dest
          }
        }
    ))
}
