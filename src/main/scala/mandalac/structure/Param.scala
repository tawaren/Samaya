package mandalac.structure

import mandalac.structure.meta.ParamAttribute
import mandalac.structure.types.Type

trait Param extends Indexed {
  def name:String
  def pos:Int
  def typ:Type
  def consumes:Boolean
  def attributes:Seq[ParamAttribute]

  override def index: Int = pos

}
