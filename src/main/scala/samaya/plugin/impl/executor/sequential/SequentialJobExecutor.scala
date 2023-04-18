package samaya.plugin.impl.executor.sequential

import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{Builder, PlainMessage, feedback}
import samaya.jobs.{DependantJob, IndependentJob, JobResultBuilder}
import samaya.plugin.service.{JobExecutor, Selectors}

class SequentialJobExecutor extends JobExecutor {

  override def matches(s: Selectors.JobExecutorSelector): Boolean = true

  override def executeIndependentJobs[O, P <: JobResultBuilder[O, P]](builder: P, jobs: Iterable[IndependentJob[O]]): Unit = {
    for(job <- jobs) {
      builder.add(job.execute())
    }
  }


  override def executeStatefulDependantJobs[K,P](jobs: Map[K, DependantJob[K,P]], roots: Seq[K]): Option[Seq[P]] = {
    var openJobs:Map[K, DependantJob[K,P]] = jobs
    var results:Map[K, P] = Map.empty

    def execute(nextKey: K): Boolean = {
      val nextValue = openJobs(nextKey)
      openJobs = openJobs - nextKey

      val depsAggregator = Seq.newBuilder[P]
      for(targ <- nextValue.dependencies()){
        if(openJobs.contains(targ)) {
          if(!execute(targ)){
            feedback(PlainMessage(s"Did not ${nextValue.description()} because of errors in dependencies", ErrorManager.Info, Builder()))
            return false
          }
        }
        results.get(targ) match {
          case Some(dep) => depsAggregator.addOne(dep)
          case None =>
            feedback(PlainMessage(s"Did not ${nextValue.description()} because of errors in dependencies", ErrorManager.Info, Builder()))
            return false
        }

      }

      nextValue.execute(depsAggregator.result()) match {
        case Some(dep) =>
          results = results + (nextKey -> dep)
          true
        case None => false
      }
    }
    val resultBuilder = Seq.newBuilder[P]
    for(k <- roots) {
      if(openJobs.contains(k)){
        if(!execute(k)){
          return None
        }
      }
      results.get(k).foreach(resultBuilder.addOne)
    }
    Some(resultBuilder.result())
  }
}
