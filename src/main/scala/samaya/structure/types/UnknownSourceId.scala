package samaya.structure.types

object UnknownSourceId extends SourceId {
  //ensure each instance is unique
  //is overwritten to make it clear in the code, that it is intended
  override def hashCode(): Int = System.identityHashCode(this)
  override def equals(obj: Any): Boolean = false
  override val src: Region = Region(Location.Unknown, Location.Unknown)
}
