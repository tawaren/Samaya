package samaya.pakage

import samaya.jobs.{IndependentJob, JobResultBuilder}
import samaya.types.{InputSource, OutputTarget, RepositoryBuilder}

class CopyJob(in:InputSource, out:OutputTarget) extends IndependentJob[OutputTarget]{
  override def execute(): OutputTarget = {
    in.copyTo(out)
    out
  }
}

object CopyJob {
  class IndexContent(repoBuilder: RepositoryBuilder) extends JobResultBuilder[OutputTarget,IndexContent] {
    override def add(out: OutputTarget): Unit = repoBuilder.indexContent(out.toInputSource)
  }
}