package samaya.structure.types

class InputSourceId(override val src:Region) extends SourceId {
  //ensure each instance is unique
  //is overwritten to make it clear in the code, that it is intended
  override def hashCode(): Int = System.identityHashCode(this)
  override def equals(obj: Any): Boolean = obj match {
    case value: InputSourceId => this.eq(value)
    case _ => false
  }
}
