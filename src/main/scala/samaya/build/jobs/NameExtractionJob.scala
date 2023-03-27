package samaya.build.jobs

import samaya.jobs.{IndependentJob, JobResultBuilder}
import samaya.plugin.service.{JobExecutor, LanguageCompiler}
import samaya.types.InputSource

import scala.collection.mutable

case class NameExtractionJob(moduleSource:InputSource) extends IndependentJob[Set[(String,String)]]{
 override def execute(): Set[(String, String)] = {
  LanguageCompiler.extractComponentNames(moduleSource).map(dep => (dep, moduleSource.identifier.name))
 }
}

object NameExtractionJob {
 class State(val builder:mutable.Builder[(String,String),Map[String,String]] = Map.newBuilder) extends JobResultBuilder[Set[(String, String)],State]{
  override def add(c: Set[(String, String)]): Unit = builder.addAll(c)
 }

 def execute(compSources:Set[InputSource]):Map[String,String] = {
  val state = new State()
  JobExecutor.executeIndependentJobs(state,compSources.map(input => NameExtractionJob(input)))
  state.builder.result()
 }
}