package samaya.structure.types

//todo: can we use for try as well ??? in the catches???
//Needed as source id for Unpakcs in a switch branch
// Reason: if different Branches would use the same SourceId then they would gen the same vals
//         meaning if two unpacks use the same id for their fields they have the same val but thats not true
class DerivedSourceId(val parent:SourceId, override val origin: Region, val branch:Int) extends SourceId {

  def canEqual(other: Any): Boolean = other.isInstanceOf[DerivedSourceId]

  override def equals(other: Any): Boolean = other match {
    case that: DerivedSourceId =>
      (that canEqual this) &&
        parent == that.parent &&
        branch == that.branch
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(parent, branch)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
