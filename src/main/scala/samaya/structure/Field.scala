package samaya.structure

import samaya.structure.types.{SourceId, Type}

trait Field extends Indexed {
  def name:String
  def pos:Int
  def typ:Type
  def attributes:Seq[Attribute]

  override def index: Int = pos
  def src:SourceId

}
