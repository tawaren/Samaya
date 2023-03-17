package samaya.build.compilation

import samaya.build.compilation.Compilation.Job
import samaya.build.compilation.pkgbuilder.ParallelPackageBuilder
import samaya.build.{ComponentBuilder, PartialPackage}
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{Builder, Error, LocatedMessage, PlainMessage, canProduceErrors, feedback}
import samaya.structure.LinkablePackage
import samaya.types.Directory

import java.util.concurrent.RecursiveTask
import scala.collection.concurrent.TrieMap

class ParallelCompilation(
                           name:String,
                           code:Directory,
                           interface:Directory,
                           resolvedDeps:Map[String, LinkablePackage],
                           nameFileMapping:Map[String,String],
                           var targets:Map[String, Job],
                        ) extends Compilation{

  private val compilationFailed: TrieMap[String,Unit] = TrieMap.empty

  private val defaultPackage: ParallelPackageBuilder = ParallelPackageBuilder(
    name = name,
    dependencies = resolvedDeps.values.toSeq
  )

  private val tasks : Map[String, CompileTask] = targets.map(kv => (kv._1, CompileTask(kv._1,kv._2)))

  //Todo: Switch to VirtualThreads when antlr4 supports Java 19
  //      This should give a big speed up as we do not loose out on IO waits
  def compileAll(): PartialPackage[_] = {
    for(t <- tasks.values) {
      t.fork()
    }
    var partialPkg = defaultPackage
    for(t <- tasks.values) {
      partialPkg = partialPkg.merge(t.join())
    }
    partialPkg
  }

  private case class CompileTask(key:String, job:Job) extends RecursiveTask[ParallelPackageBuilder] {
    override def compute(): ParallelPackageBuilder = {
      var partialPkg = defaultPackage
      val depsError = canProduceErrors{
        for (Dependency(path, sources) <- job.dependencies) {
          //it is in the same package
          if (path.head.nonEmpty && path.head.charAt(0).isUpper) {
            nameFileMapping.get(path.head) match {
              case None => feedback(LocatedMessage(s"Component ${path.mkString(".")} is missing in workspace", sources ,Error,Builder()))
              case Some(targ) => tasks.get(targ) match {
                case Some(dep) => partialPkg = partialPkg.merge(dep.join())
                case None => if(compilationFailed.contains(targ)) {
                  feedback(LocatedMessage(s"Component ${path.mkString(".")} was not declared in dependencies", sources ,Error,Builder()))
                }
              }
            }
          } else {
            val pkgName = path.head
            resolvedDeps.get(pkgName) match {
              case Some(pkg) =>
                val compPath = path.tail.takeWhile(ep => ep.nonEmpty && ep.charAt(0).isLower)
                val compName = path.tail.drop(compPath.length)
                if(compName.nonEmpty) {
                  if (pkg.componentByPathAndName(compPath, compName.head).isEmpty) {
                    feedback(LocatedMessage(s"Workspace does not contain dependency to ${path.mkString(".")}", sources ,Error, Builder()))
                  }
                }
              case None => feedback(LocatedMessage(s"Workspace or Package $pkgName is missing", sources ,Error, Builder()))
            }
          }
        }
      }
      if(!depsError) {
        partialPkg = partialPkg.merge(ComponentBuilder.build(job.source, code, interface, partialPkg))
      } else {
        feedback(PlainMessage(s"Did not compile ${job.source.identifier} in ${job.source.location} because of errors in dependencies", ErrorManager.Info, Builder()))
        compilationFailed.put(key,())
      }
      partialPkg
    }
  }
}