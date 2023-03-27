package samaya.build.jobs

import samaya.compilation.ErrorManager.producesErrorValue
import samaya.jobs.{IndependentJob, JobResultBuilder}
import samaya.plugin.service.{JobExecutor, TaskExecutor}
import samaya.structure.LinkablePackage
import samaya.types.Workspace

import scala.collection.mutable

case class WorkspaceCompilationJob(iwp:Workspace) extends IndependentJob[Option[LinkablePackage]]{
 override def execute(): Option[LinkablePackage] = producesErrorValue{
   TaskExecutor[Workspace,LinkablePackage]("build",iwp)
 }.flatten
}

object WorkspaceCompilationJob {
 class State(var failure:Boolean = false, val builder: mutable.Builder[(String,LinkablePackage),Map[String,LinkablePackage]] = Map.newBuilder) extends JobResultBuilder[Option[LinkablePackage],State]{
  override def add(c: Option[LinkablePackage]): Unit = c match {
   case Some(pkg) => builder.addOne(pkg.name -> pkg)
   case None => failure = true
  }
 }

 def execute(wps:Set[Workspace], curBuilder: mutable.Builder[(String,LinkablePackage),Map[String,LinkablePackage]] = Map.newBuilder):Boolean = {
  val state = new State(builder = curBuilder)
  JobExecutor.executeIndependentJobs(state,wps.map(input => WorkspaceCompilationJob(input)))
  state.failure
 }
}
