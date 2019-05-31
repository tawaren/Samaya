package mandalac.structure

import mandalac.structure.meta.ConstructorAttribute

trait Constructor extends Indexed{
  def tag:Int
  def name:String
  def attributes:Seq[ConstructorAttribute]
  def fields:Seq[Field]
  def field(index:Int):Option[Field]

  override def index: Int = tag

}
