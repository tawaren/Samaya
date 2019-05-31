package mandalac.structure

import mandalac.structure.meta.RiskAttribute

trait Risk extends Indexed{
  def name:String
  def index:Int
  def attributes:Seq[RiskAttribute]
}
