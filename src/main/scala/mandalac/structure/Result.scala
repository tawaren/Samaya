package mandalac.structure

import mandalac.structure.meta.ParamAttribute
import mandalac.structure.types.{Type}

trait Result extends Indexed {
  def name:String
  def pos:Int
  def typ:Type
  def borrows:Set[String]
  def attributes:Seq[ParamAttribute]

  override def index: Int = pos

}
