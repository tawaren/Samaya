package samaya.plugin.service

import samaya.plugin.service.category.WorkspaceEncodingPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.LinkablePackage
import samaya.toolbox.helpers.ConcurrentStrongComputationCache
import samaya.types.{Address, GeneralSource, Identifier, Workspace}

import scala.reflect.ClassTag
import scala.util.DynamicVariable


//a plugin description for managing (parsing and validating) interface descriptions
trait WorkspaceEncoder extends Plugin {

  override type Selector = Selectors.WorkspaceSelector

  //parses file and validate it and then returns the corresponding module
  // beside returning it it is registered in the Module Registry if it is valid
  // as it is just the interface the code of the function body as well as private functions are not present
  def decodeWorkspace(source: GeneralSource):Option[Workspace]
}

object WorkspaceEncoder extends WorkspaceEncoder with PluginProxy{

  val workspaceExtensionPrefix = "wsp"

  val computationCache = new ConcurrentStrongComputationCache[GeneralSource,Option[Workspace]]()

  //Todo: Shall we make more global and also extend on packages??
  private val _contextPath = new DynamicVariable[Seq[Identifier]](Seq.empty)
  def contextPath():Address = Address.Relative(_contextPath.value)

  type PluginType = WorkspaceEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = WorkspaceEncodingPluginCategory

  override def decodeWorkspace(source: GeneralSource): Option[Workspace] = {
    computationCache.getOrElseUpdate(source){
      val name = if(source.identifier.name.isEmpty){
        source.identifier.fullName
      } else {
        source.identifier.name
      }
      _contextPath.withValue(_contextPath.value :+ Identifier(name)){
        select(Selectors.WorkspaceDecoderSelector(source)).flatMap(r => r.decodeWorkspace(source))
      }
    }
  }

  def isWorkspace(source:GeneralSource): Boolean = {
    matches(Selectors.WorkspaceDecoderSelector(source))
  }
}
