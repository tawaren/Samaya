package samaya.structure

import samaya.structure.types.{SourceId, Type}

trait Param extends Indexed {
  def name:String
  def index:Int
  def typ:Type
  def consumes:Boolean
  def attributes:Seq[Attribute]
  def src:SourceId
}
