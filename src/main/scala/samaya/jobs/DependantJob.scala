package samaya.jobs

trait DependantJob[K,P] {
  def dependencies(): Set[K]
  def execute(deps: Seq[P]): Option[P]
  def description(): String
}
