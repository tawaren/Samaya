package mandalac.structure

import mandalac.structure.meta.FieldAttribute
import mandalac.structure.types.Type

trait Field extends Indexed {
  def name:String
  def pos:Int
  def typ:Type
  def attributes:List[FieldAttribute]

  override def index: Int = pos

}
