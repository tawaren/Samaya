package samaya.build.jobs

import samaya.build.{ComponentBuilder, PartialPackage}
import samaya.compilation.ErrorManager.{Builder, Error, LocatedMessage, feedback, producesErrorValue}
import samaya.jobs.DependantJob
import samaya.structure.{Component, Interface, LinkablePackage}
import samaya.types.{Directory, InputSource}

//Todo: Make a trait and a implementation
class DependantBuildJob(source:InputSource, deps:Set[Dependency], ctx:DependantBuildJob.Context) extends DependantJob[PartialPackage]{
  //We delay validation so errors appear in the right context
  lazy val dependencies: Set[String] = {
    deps.flatMap{
      case Dependency(path, sources) =>
        //it is in the same package
        if (path.head.nonEmpty && path.head.charAt(0).isUpper) {
          ctx.componentToSourceMapping.get(path.head) match {
            case None =>
              feedback(LocatedMessage(s"Component ${path.mkString(".")} is missing in workspace", sources ,Error,Builder()))
              None
            case Some(targ) => Some(targ)
          }
        } else {
          val pkgName = path.head
          ctx.resolvedDeps.get(pkgName) match {
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
          None
        }
    }
  }

  def execute(deps:Seq[PartialPackage]):Option[PartialPackage] = producesErrorValue {
    val pkg = if(deps.isEmpty){
      ctx.createDefaultPartialPackage()
    } else {
      deps.reduce(_.merge(_))
    }
    ComponentBuilder.build(source, ctx.code, ctx.interface, pkg)
  }

  def description():String = s"compile ${source.identifier} in ${source.location}"
}

object DependantBuildJob {
  class Context(
                 val name:String,
                 val code:Directory,
                 val interface:Directory,
                 val resolvedDeps:Map[String,LinkablePackage],
                 val componentToSourceMapping:Map[String,String]){
    private val deps = resolvedDeps.values.toSeq

    def createJob(source:InputSource, deps:Set[Dependency]):DependantBuildJob = new DependantBuildJob(source,deps, this)

    def createDefaultPartialPackage():PartialPackage = PartialPackage(
      name = name,
      dependencies = deps,
    )
  }
}

