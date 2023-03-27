package samaya.build


import samaya.ProjectUtils
import samaya.build.BuildTask.{TaskName, target}
import samaya.build.jobs.{DependantBuildJob, DependencyExtractionJob, NameExtractionJob, WorkspaceCompilationJob}
import samaya.compilation.ErrorManager
import samaya.plugin.service.{AddressResolver, ContentRepositoryEncoder, JobExecutor, PackageEncoder, Selectors, TaskExecutor}
import samaya.structure.LinkablePackage
import samaya.types.{Repository, Workspace}
import samaya.compilation.ErrorManager.{Builder, Error, PlainMessage, feedback}
import samaya.plugin.config.{ConfigPluginCompanion, ConfigValue}
import samaya.plugin.service.TaskExecutor.IsClass
import samaya.repository.BuildRepository

import scala.reflect.ClassTag

class BuildTask extends TaskExecutor {

  private val IsString = IsClass[String]
  private val IsPackage = IsClass[LinkablePackage]
  private val IsWorkspace = IsClass[Workspace]

  override def matches(s: Selectors.TaskExecutorSelector): Boolean = s match {
    case Selectors.SelectByName(TaskName) => true
    case Selectors.SelectApplyTask(TaskName, IsString(), IsPackage()) => true
    case Selectors.SelectApplyTask(TaskName, IsWorkspace(), IsPackage()) => true
    case _ => false
  }

  override def execute(_name: String): Unit = build(target.value)

  override def apply[S: ClassTag, T: ClassTag](_name: String, src: S): Option[T] = src match {
    case IsString(target) => IsPackage[T](build(target))
    case IsWorkspace(wp) => IsPackage[T](compileWorkspace(wp))
  }

  def build(target:String):Option[LinkablePackage] = {
    val t0 = System.currentTimeMillis()
    val wp = ProjectUtils.createWorkspace(target)
    //todo: Have some default Repositories loaded from a file
    val res = compileWorkspace(wp)
    ContentRepositoryEncoder.storeRepository(wp.packageTarget, BuildRepository)
    println(s"compilation of workspace ${wp.name} finished in ${System.currentTimeMillis()-t0} ms" )
    res
  }

  //Todo: if we do these to does pack them into helper & share
  //todo: the getOrElse(Set.empty) need an infer by convention algorithm
  //       a plugin that has a strategy to find missing stuff?
  def compileWorkspace(wp:Workspace):Option[LinkablePackage] = {
    val depsBuilder = Map.newBuilder[String, LinkablePackage]
    val includes =  wp.includes
    val repos = wp.repositories
    val dependencies =  wp.dependencies

    depsBuilder.sizeHint(includes.size + dependencies.size)
    //Todo: go down the structure & make the tasks + collect the repos
    //      if a workspace is reachable over multiple paths then repos are joined
    //     then start them as dependat tasks and apply the repos
    Repository.withRepos(repos) {
      val depsError = WorkspaceCompilationJob.execute(includes, depsBuilder)
      if (!depsError) {
        dependencies.foreach(pkg => depsBuilder += pkg.name -> pkg)
        val resolvedDeps = depsBuilder.result()

        if (resolvedDeps.size != includes.size + dependencies.size) {
          throw new Exception("NEEDS A CUSTOM ERROR: Alla ambigous dependencies")
        }

        val t0 = System.currentTimeMillis()

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

        val resPkg = JobExecutor.executeStatefulDependantJobs(jobs, jobs.keys.toSet) match {
          case Some(pkgs) => if(pkgs.nonEmpty){
            pkgs.reduce(_.merge(_)).toLinkablePackage(wp.packageTarget,wp.includes.map(_.name))
          } else {
            ctx.createDefaultPartialPackage().toLinkablePackage(wp.packageTarget,wp.includes.map(_.name))
          }
          case None =>
            //Todo Error Message
            return None
        }

        PackageEncoder.serializePackage(resPkg, Some(wp))
        updateContentIndexes(resPkg)
        println(s"compilation of package ${resPkg.name} finished in ${System.currentTimeMillis() - t0} ms")
        Some(resPkg)
      } else {
        feedback(PlainMessage(s"Did not compile ${wp.name} in ${wp.location} because of errors in dependencies", ErrorManager.Info, Builder()))
        None
      }
    }
  }

  def updateContentIndexes(pkg:LinkablePackage): Unit = {

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
}

object BuildTask extends ConfigPluginCompanion {

  val TaskName: String = "build"

  val target: ConfigValue[String] =  param(1).default("/")

}
