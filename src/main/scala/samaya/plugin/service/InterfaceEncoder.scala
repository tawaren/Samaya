package samaya.plugin.service

import java.io.OutputStream
import samaya.plugin.service.category.InterfaceEncodingPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.types.Hash
import samaya.structure.{Component, Interface, Meta}
import samaya.types.InputSource

import scala.reflect.ClassTag

//a plugin description for managing (parsing and validating) interface descriptions
trait InterfaceEncoder extends Plugin{

  val interfaceExtensionPrefix = "abi"

  object InterfaceExtension {
    def apply(format: String): String = interfaceExtensionPrefix + "." + format
    def unapply(ext: String): Option[String] = if(!ext.startsWith(interfaceExtensionPrefix)) {
      None
    } else {
      Some(ext.drop(interfaceExtensionPrefix.length + 1))
    }
    def unapply(source: InputSource): Option[String] = source.identifier.extension.flatMap(unapply)
  }

  override type Selector = Selectors.InterfaceSelector
  def deserializeInterface(language:String, version:String, classifier:Set[String], file:InputSource, meta:Meta): Option[Interface[Component]]
  def serializeInterface(inter:Component, codeHash:Option[Hash], hasError:Boolean, out:OutputStream): Boolean
  def defaultFormat():Option[String]
}

object InterfaceEncoder extends InterfaceEncoder with PluginProxy{

  type PluginType = InterfaceEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = InterfaceEncodingPluginCategory

  //Todo: check if we can read source from meta.interface or if they can be different
  override def deserializeInterface(language:String, version:String, classifier:Set[String], source: InputSource, meta:Meta): Option[Interface[Component]] = {
    select(Selectors.InterfaceDecoderSelector(language, version, classifier, source)).flatMap(r => r.deserializeInterface(language,version,classifier,source,meta))
  }

  override def serializeInterface(comp: Component, codeHash:Option[Hash], hasError:Boolean, out:OutputStream): Boolean  = {
    defaultFormat().exists(f => serializeInterface(comp, codeHash, InterfaceExtension(f), hasError, out))
  }

  def serializeInterface(comp: Component, codeHash:Option[Hash], format:String, hasError:Boolean, out: OutputStream): Boolean = {
    select(Selectors.InterfaceEncoderSelector(comp.language, comp.version, comp.classifier, format)).exists(r => r.serializeInterface(comp, codeHash, hasError, out))
  }

  override def defaultFormat(): Option[String] = {
    select(Selectors.InterfaceFormatSelector).flatMap(r => r.defaultFormat())
  }
}
