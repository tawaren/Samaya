package samaya.jobs

trait JobResultBuilder[C,P <: JobResultBuilder[C,P]] {
  this: P =>
  def add(c: C): Unit
}

object JobResultBuilder {
  class ForwardingResultBuilder[T](f: T => Unit) extends JobResultBuilder[T,ForwardingResultBuilder[T]]{
    override def add(c: T): Unit = f(c)
  }
  def apply[T](f: T => Unit) : ForwardingResultBuilder[T] = new ForwardingResultBuilder[T](f)
  def apply(): ForwardingResultBuilder[Unit] = new ForwardingResultBuilder[Unit]((a :Unit) => a)

}