package samaya.structure

import samaya.structure.types.SourceId

trait Binding extends Indexed {
  def name:String
  def index:Int
  def attributes:Seq[Attribute]
  def src:SourceId
}
