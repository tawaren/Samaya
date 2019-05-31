package mandalac.structure

trait TypeParameterized {
  def generics:Seq[Generic]
  def generic(index:Int):Option[Generic]
}
