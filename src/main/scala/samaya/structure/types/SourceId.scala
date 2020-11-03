package samaya.structure.types

trait SourceId{
  val origin:Region
  def deriveSourceId(num:Int):SourceId = deriveSourceId(num:Int,origin:Region)
  def deriveSourceId(num:Int,newSrc:Region):SourceId = new DerivedSourceId(this,newSrc, num)
}
