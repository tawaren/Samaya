package samaya.plugin.service

import samaya.jobs.{DependantJob, IndependentJob, JobResultBuilder}
import samaya.plugin.{Plugin, PluginProxy}
import samaya.plugin.service.category.JobExecutorPluginCategory

import scala.reflect.ClassTag

trait JobExecutor extends Plugin{
  override type Selector = Selectors.JobExecutorSelector
  def executeStatefulDependantJobs[K,P](jobs: Map[K, DependantJob[K,P]], roots: Seq[K]): Option[Seq[P]]
  def executeIndependentJobs[O,P <: JobResultBuilder[O,P]](builder:P, jobs:Iterable[IndependentJob[O]]): Unit
}

object JobExecutor extends JobExecutor with PluginProxy{

  type PluginType = JobExecutor
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = JobExecutorPluginCategory

  override def executeStatefulDependantJobs[K,P](jobs: Map[K, DependantJob[K,P]], roots: Seq[K]): Option[Seq[P]] = {
    //Todo: Better error
    select(Selectors.DependantJobSelector).flatMap(r => r.executeStatefulDependantJobs(jobs, roots))
  }

  override def executeIndependentJobs[O, P <: JobResultBuilder[O, P]](builder: P, jobs: Iterable[IndependentJob[O]]): Unit = {
    select(Selectors.IndependentJobSelector).foreach(r => r.executeIndependentJobs(builder, jobs))
  }
}

