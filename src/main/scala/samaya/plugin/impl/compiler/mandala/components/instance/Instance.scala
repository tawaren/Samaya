package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.structure.types.{CompLink, Type}
import samaya.structure.{Component, Interface, Meta}

trait Instance extends Component {
  def classTarget:CompLink
  def applies:Seq[Type]
  def toInterface(meta: Meta): Interface[Instance]
}

object Instance {
  def deriveTopName(moduleName: String, implName: String):String = moduleName+"$"+implName

  sealed trait EntryRef
  case class RemoteEntryRef(module:CompLink, offset:Int) extends EntryRef
  //Note: This one is only here for the special case where we building a module with nested instances
  //      It can be used for the entries in the localInstances map
  //      But it should never appear anywhere else
  case class LocalEntryRef(offset:Int) extends EntryRef

}