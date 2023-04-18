package samaya.build


import samaya.ProjectUtils
import samaya.ProjectUtils.processWithArgRepos
import samaya.build.BuildTask.{TaskName, repos, target, validation}
import samaya.build.jobs.DependantWorkspaceCompilationJob
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{PlainMessage, canProduceErrors, feedback}
import samaya.plugin.service.{ContentRepositoryEncoder, JobExecutor, Selectors, TaskExecutor}
import samaya.structure.LinkablePackage
import samaya.types.{Repository, Workspace}
import samaya.jobs.DependantJob
import samaya.plugin.config.{ConfigPluginCompanion, ConfigValue}
import samaya.plugin.service.TaskExecutor.IsClass
import samaya.repository.BuildRepository
import samaya.validation.WorkspaceValidator

import scala.collection.mutable
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
    case IsWorkspace(wp) => IsPackage[T](build(wp))
  }

  def build(wp:Workspace):Option[LinkablePackage] = {
    processWithArgRepos(repos.value) {
      if (validation.value) {
        val hasError = canProduceErrors(WorkspaceValidator.validateWorkspace(wp, recursive = true))
        if (hasError) {
          feedback(PlainMessage(s"Workspace ${wp.name} is invalid", ErrorManager.Error, ErrorManager.Deployer()))
          return None
        } else {
          feedback(PlainMessage(s"Validation of Workspace ${wp.name} succeeded", ErrorManager.Info, ErrorManager.Deployer()))
        }
      }

      val res = compileWorkspaces(wp)
      ContentRepositoryEncoder.storeRepository(wp.packageTarget, BuildRepository)
      res
    }
  }

  def build(target:String):Option[LinkablePackage] = {
    val t0 = System.currentTimeMillis()
    val wp = ProjectUtils.createWorkspace(target)
    val res = build(wp)
    println(s"compilation of workspace ${wp.name} finished in ${System.currentTimeMillis()-t0} ms" )
    res
  }

  def collectWorkspaces(wp:Workspace, activeRepos: Set[Repository], workspaceCollector: mutable.Map[Workspace,Set[Repository]]):Unit = {
    val updatedRepos = activeRepos ++ wp.repositories
    workspaceCollector.updateWith(wp)(_.map(_++updatedRepos).orElse(Some(updatedRepos)))
    //we go recursive even if we already have it, to ensure we collect the repos of all paths
    wp.includes.foreach(collectWorkspaces(_, updatedRepos, workspaceCollector))
  }

  def compileWorkspaces(wp:Workspace):Option[LinkablePackage] = {
    //Relies on the fact that WorkspaceEncoder de-duplicates based on source
    // & by default WorkspaceImpl uses object identity
    val workspaceCollector: mutable.Map[Workspace,Set[Repository]] = mutable.Map.empty
    collectWorkspaces(wp,Set.empty,workspaceCollector)
    //transform them into jobs
    val jobs : Map[Workspace,DependantJob[Workspace,LinkablePackage]] = workspaceCollector.map{
      case (wp,repos) => (wp, new DependantWorkspaceCompilationJob(wp,repos))
    }.toMap

    JobExecutor.executeStatefulDependantJobs(jobs, Seq(wp)) match {
      case Some(Seq(lp)) => Some(lp)
      case None => None
    }
  }
}

object BuildTask extends ConfigPluginCompanion {

  val TaskName: String = "build"

  val target: ConfigValue[String] =  param(1).default("/")

  private val validation : ConfigValue[Boolean] = opt("build.validation|validation").default(true)
    .warnIfFalse("Validation of Mandala modules and instances is disabled",ErrorManager.Builder())

  private val repos : ConfigValue[Seq[String]] = collectArg("build.repos|build.repositories|repositories|repos").default(Seq.empty)

}
