package samaya.plugin.impl.executor.virtual

import jdk.incubator.concurrent.StructuredTaskScope
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager._
import samaya.jobs.{DependantJob, IndependentJob, JobResultBuilder}
import samaya.plugin.service.{JobExecutor, Selectors}

import java.util.concurrent._
import scala.util.Using

class VirtualThreadJobExecutor extends JobExecutor {

  val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor


  override def matches(s: Selectors.JobExecutorSelector): Boolean = true

  class ParallelJobRunner[K,P](jobs:Map[K, DependantJob[K,P]]) {
    val tasks :CompletableFuture[Map[K, Future[Option[P]]]] = new CompletableFuture()
    case class JobTask(key:K, job:DependantJob[K,P]) extends Callable[Option[P]] {
      override def call(): Option[P] = {
        val depsBuilder = Seq.newBuilder[P]
        for (targ <- job.dependencies()) {
          tasks.get().get(targ) match {
            case Some(dep) => dep.get() match {
              case Some(nState) => depsBuilder.addOne(nState)
              case None =>
                feedback(PlainMessage(s"Can not ${job.description()} because of errors in dependencies", ErrorManager.Info, Builder()))
                return None
            }
            case None => unexpected("No task for dependency " + targ + " available", Always)
          }
        }
        val deps = depsBuilder.result()
        job.execute(deps)
      }
    }

    def executeAllJobs(root:Seq[K]): Option[Seq[P]] = {
      Using(new StructuredTaskScope.ShutdownOnFailure) { scope =>
        val jobTasks = jobs.map(kv => (kv._1, JobTask(kv._1,kv._2)))
        tasks.complete(jobTasks.map{
          case (key, job) => (key, scope.fork(() => job.call()))
        })
        val resBuilder = Seq.newBuilder[P]
        for(name <- root){
          tasks.get().get(name) match {
            case Some(task) => task.get() match {
              case Some(res) => resBuilder.addOne(res)
              case None =>
                scope.join
                scope.throwIfFailed()
                return None
            }
            case None => unexpected("No task for dependency " + name + " available", Always)
          }
        }
        scope.join
        scope.throwIfFailed()
        Some(resBuilder.result())
      }.get
    }
  }

  override def executeStatefulDependantJobs[K,P](jobs: Map[K, DependantJob[K,P]], root: Seq[K]): Option[Seq[P]] = new ParallelJobRunner[K,P](jobs).executeAllJobs(root)

  override def executeIndependentJobs[O, P <: JobResultBuilder[O, P]](builder: P, jobs: Iterable[IndependentJob[O]]): Unit = {
    //Make configurable
    if(jobs.size < 2){
      jobs.foreach(job => builder.add(job.execute()))
    } else {
      Using(new StructuredTaskScope.ShutdownOnFailure) { scope =>
        val runningJobs = jobs.map(job => scope.fork[O](() => job.execute()))
        for (job <- runningJobs) {
          builder.add(job.get())
        }
        scope.join
        scope.throwIfFailed()
      }
    }
  }
}
