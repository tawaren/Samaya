package mandalac.plugin.service

import java.io.OutputStream

import mandalac.plugin.service.category.InterfaceEncodingPluginCategory
import mandalac.plugin.{Plugin, PluginProxy}
import mandalac.structure.{Module, ModuleEssentials, ModuleMeta}
import mandalac.types.InputSource

//a plugin description for managing (parsing and validating) interface descriptions
trait InterfaceManager extends Plugin{

  override type Selector = Selectors.InterfaceSelector

  //parses file and validate it and then returns the corresponding module
  // beside returning it it is registered in the Module Registry if it is valid
  // as it is just the interface the code of the function body as well as private functions are not present
  //todo: make a simple deserialize and do the rest in a common part
  def registerInterface(file:InputSource, meta:ModuleMeta):Option[Module]
  //writes output
  def serializeInterface(module: ModuleEssentials, out:OutputStream): Boolean
}

object InterfaceManager extends InterfaceManager with PluginProxy{

  type PluginType = InterfaceManager
  override def category: PluginCategory[PluginType] = InterfaceEncodingPluginCategory

  override def registerInterface(source: InputSource, meta:ModuleMeta): Option[Module] = {
    select(Selectors.InterfaceDeserializationSelector(source)).flatMap(r => r.registerInterface(source, meta))
  }

  override def serializeInterface(module: ModuleEssentials, out:OutputStream): Boolean  = {
    //todo: get default from config
    serializeInterface(module,"json",out)
  }

  def serializeInterface(module: ModuleEssentials, format:String, out: OutputStream): Boolean = {
    select(Selectors.InterfaceSerializationSelector(format)).exists(r => r.serializeInterface(module, out))

  }

}