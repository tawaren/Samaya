package samaya.build.compilation

import samaya.structure.types.SourceId

case class Dependency(path: Seq[String],usages: Seq[SourceId])
