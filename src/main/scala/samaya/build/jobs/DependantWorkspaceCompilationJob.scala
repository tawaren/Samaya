package samaya.build.jobs

import samaya.compilation.ErrorManager.{Builder, Error, PlainMessage, feedback}
import samaya.jobs.DependantJob
import samaya.plugin.service.{AddressResolver, JobExecutor, PackageEncoder}
import samaya.repository.BuildRepository
import samaya.structure.LinkablePackage
import samaya.types.{Repository, Workspace}

import scala.collection.mutable

class DependantWorkspaceCompilationJob(wp:Workspace, repos:Set[Repository]) extends DependantJob[Workspace,LinkablePackage]{
  override def dependencies(): Set[Workspace] = wp.includes

  private def updateContentIndexes(pkg:LinkablePackage): Unit = {

    BuildRepository.indexContent(pkg)

    pkg.components.foreach(cmp => {
      cmp.meta.interface.foreach(BuildRepository.indexContent)
      cmp.meta.sourceCode.foreach(BuildRepository.indexContent)
      cmp.meta.code.foreach(BuildRepository.indexContent)
    })

    pkg.dependencies.foreach(dep => {
      BuildRepository.indexContent(dep)
    })

  }

  private def addOrdered(deps:Map[String,DependantBuildJob], processed:mutable.LinkedHashSet[String], current:String):Unit = {
    if(processed.contains(current)) return
    val currentDeps = deps.get(current) match {
      //Todo: I do not like it produces some errors out of order
      case Some(depJob) => depJob.dependencies
      case None => return
    }
    currentDeps.foreach(addOrdered(deps, processed, _))
    processed.add(current)
  }

  private def orderedSequence(deps:Map[String,DependantBuildJob]):Seq[String] = {
    val processed:mutable.LinkedHashSet[String] = mutable.LinkedHashSet.empty
    deps.keySet.foreach(addOrdered(deps,processed,_))
    processed.toSeq
  }

  override def execute(deps: Seq[LinkablePackage]): Option[LinkablePackage] = {
    val t0 = System.currentTimeMillis()
    val depsBuilder = Map.newBuilder[String, LinkablePackage]
    val includes = wp.includes
    val dependencies = wp.dependencies
    depsBuilder.sizeHint(includes.size + dependencies.size)
    deps.foreach(pkg => depsBuilder += pkg.name -> pkg)
    dependencies.foreach(pkg => depsBuilder += pkg.name -> pkg)
    val resolvedDeps = depsBuilder.result()
    if (resolvedDeps.size != includes.size + dependencies.size) {
      throw new Exception("NEEDS A CUSTOM ERROR: Alla ambigous dependencies")
    }

    Repository.withRepos(repos) {
      val source = wp.sourceLocation
      val code = wp.codeLocation
      val interface = wp.interfaceLocation

      val compSources = wp.sources.flatMap { src =>
        AddressResolver.resolve(source.resolveAddress(src), AddressResolver.InputLoader) match {
          case None =>
            feedback(PlainMessage(s"Could not find $src", Error, Builder()))
            None
          case moduleSources => moduleSources
        }
      }

      val componentToSourceMapping = NameExtractionJob.execute(compSources)
      val ctx = new DependantBuildJob.Context(wp.name, code, interface, resolvedDeps, componentToSourceMapping)
      val jobs = DependencyExtractionJob.execute(compSources).map {
        case (compSource, deps) => (compSource.identifier.name, ctx.createJob(compSource, deps))
      }
      val ordered = orderedSequence(jobs)
      val resPkg = JobExecutor.executeStatefulDependantJobs(jobs, ordered) match {
        case Some(pkgs) => if (pkgs.nonEmpty) {
          pkgs.reduce(_.merge(_)).toLinkablePackage(wp.packageTarget, wp.includes.map(_.name))
        } else {
          ctx.createDefaultPartialPackage().toLinkablePackage(wp.packageTarget, wp.includes.map(_.name))
        }
        case None =>
          feedback(PlainMessage(s"Workspace ${wp.name} compilation failed due to error in source code", Error, Builder()))
          return None
      }

      PackageEncoder.serializePackage(resPkg, Some(wp))
      updateContentIndexes(resPkg)
      println(s"compilation of package ${resPkg.name} finished in ${System.currentTimeMillis() - t0} ms")
      Some(resPkg)
    }
  }

  override def description(): String = s"build ${wp.identifier.fullName} in ${wp.location}"
}
