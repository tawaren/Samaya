package samaya.structure

import samaya.structure.types.SourceId

trait ModuleEntry extends Indexed {
  def name:String
  def position:Int
  //Todo: remove optional and implement source tracking in json interfaces
  def src:SourceId
}
