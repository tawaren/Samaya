package samaya.structure.types

trait SourceId{
  val src:Region
  def deriveSourceId(num:Int):SourceId = deriveSourceId(num:Int,src:Region)
  def deriveSourceId(num:Int,newSrc:Region):SourceId = new DerivedSourceId(this,newSrc, num)
}
