package samaya.build.jobs

import samaya.jobs.{IndependentJob, JobResultBuilder}
import samaya.plugin.service.{JobExecutor, LanguageCompiler}
import samaya.types.InputSource

import scala.collection.mutable

case class DependencyExtractionJob(compSource:InputSource) extends IndependentJob[(InputSource, Set[Dependency])]{
 override def execute(): (InputSource, Set[Dependency]) = {
  (compSource, LanguageCompiler.extractDependencies(compSource))
 }
}

object DependencyExtractionJob {
 class State(val builder:mutable.Builder[(InputSource,Set[Dependency]),Map[InputSource,Set[Dependency]]] = Map.newBuilder) extends JobResultBuilder[(InputSource,Set[Dependency]),State]{
  override def add(c: (InputSource,Set[Dependency])): Unit = builder.addOne(c)
 }

 def execute(compSources:Set[InputSource]):Map[InputSource,Set[Dependency]] = {
  val state = new State()
  JobExecutor.executeIndependentJobs(state,compSources.map(input => DependencyExtractionJob(input)))
  state.builder.result()
 }
}

