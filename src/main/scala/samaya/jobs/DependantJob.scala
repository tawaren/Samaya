package samaya.jobs

trait DependantJob[P] {
  def dependencies(): Set[String]
  def execute(deps: Seq[P]): Option[P]
  def description(): String
}
