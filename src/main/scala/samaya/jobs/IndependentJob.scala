package samaya.jobs

trait IndependentJob[O] {
  def execute(): O
}

object IndependentJob {
  def apply[I,O](f: I => O)(in:I):IndependentJob[O] = () => f(in)
  def apply[O](f: () => O):IndependentJob[O] = () => f()
}