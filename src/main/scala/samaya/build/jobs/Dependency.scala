package samaya.build.jobs

import samaya.structure.types.SourceId

case class Dependency(path: Seq[String],usages: Seq[SourceId])
