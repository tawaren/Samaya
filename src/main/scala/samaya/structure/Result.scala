package samaya.structure

import samaya.structure.types.{SourceId, Type}

trait Result extends Indexed {
  def name:String
  def index:Int
  def typ:Type
  def attributes:Seq[Attribute]
  def src:SourceId
}
