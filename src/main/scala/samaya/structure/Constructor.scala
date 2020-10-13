package samaya.structure

import samaya.structure.types.SourceId

trait Constructor extends Indexed{
  def tag:Int
  def name:String
  def attributes:Seq[Attribute]
  def fields:Seq[Field]
  def field(index:Int):Option[Field] = fields.find(r => r.pos == index)

  override def index: Int = tag
  def src:SourceId
}
