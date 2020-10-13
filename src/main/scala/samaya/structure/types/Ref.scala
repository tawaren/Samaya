package samaya.structure.types

//Marker trait: Represents either an Id or a Val
trait Ref {
  def id:Id
  def src:Option[Region] = id.src
  def name:String = id.name
}
