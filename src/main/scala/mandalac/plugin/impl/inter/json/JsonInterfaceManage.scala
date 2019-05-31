package mandalac.plugin.impl.inter.json

import java.io.OutputStream

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromStream, writeToStream}
import mandalac.plugin.impl.inter.json.JsonModel.Interface
import mandalac.registries.ModuleRegistry
import mandalac.structure.{Module, ModuleMeta}
import mandalac.validation.ModuleValidator
import mandalac.compilation.ErrorHandler._
import mandalac.plugin.service.{InterfaceManager, Selectors}
import mandalac.types.InputSource

//A Interface Manager for a json description of a interface
class JsonInterfaceManage extends InterfaceManager {
  //This plugin handles everything with ".json"
  // Todo: Maybe a more precise file type ?? alla interface.json
  //       Ev even parse header and look into it???


  override def matches(s: Selectors.InterfaceSelector): Boolean =  {
    s match {
      case Selectors.InterfaceDeserializationSelector(source) => source.identifier.extension.contains("json")
      case Selectors.InterfaceSerializationSelector(format) => format == "json"
    }
  }

  //this parses a json interface, validates it and then stores it
  override def registerInterface(file: InputSource, meta:ModuleMeta): Option[Module] = {
    producesErrorValue{
      //extract the source data
      val pkg = meta.path
      val name = meta.name

      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[Interface](file.content)

      //check that the name of the extracted interface is the same as the filename
      if(name != interfaceAst.name) return None

      //convert the AST to the the internal shared representation of Modules
      val impl = new InterfaceImpl(interfaceAst,meta.codeHash,meta)

      //Run the validator on the module
      ModuleValidator.validateModule(impl)
      //The Module is valid so register it
      ModuleRegistry.recordModule(pkg, impl)
      //return the module on success
      impl
    }
  }

  override def serializeInterface(module: Module, out: OutputStream): Boolean = {
    val repr = Serializer.toInterfaceRepr(module)
    writeToStream[Interface](repr, out)
    true
  }
}
