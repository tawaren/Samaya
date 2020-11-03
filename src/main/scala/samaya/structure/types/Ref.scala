package samaya.structure.types

//Marker trait: Represents either an Id or a Val
trait Ref {
  def src:SourceId
  def name:String
  def id:Id = Id(name,src)
}
