package samaya.plugin.impl.pkg.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import JsonModel._
import samaya.codegen.{ModuleSerializer, NameGenerator}
import samaya.compilation.ErrorManager.unexpected
import samaya.plugin.service.PackageEncoder.PackageExtension
import samaya.structure.{LinkablePackage, Meta, Interface, ModuleInterface, TransactionInterface, Transaction}
import samaya.plugin.service.{InterfaceEncoder, LocationResolver, PackageEncoder, Selectors}
import samaya.structure.types.Hash
import samaya.structure
import samaya.types.{Identifier, InputSource, Location, Path, Workspace}
import samaya.validation.PackageValidator

//A Package Manager for a json description of a package
class JsonPackageEncoder extends PackageEncoder {

  val Json = "json"

  override def matches(s: Selectors.PackageSelector): Boolean = {
    s match {
      case Selectors.PackageDeserializationSelector(PackageExtension(Json)) => true
      case Selectors.PackageSerializationSelector(PackageExtension(Json)) => true
      case _ => false
    }

  }

  //todo: use error handler
  //todo: Make more stuff Optional in Module Meta
  def deserializePackage(file: InputSource): Option[structure.LinkablePackage] = {

    //parse the package with Package as description
    val pkg = readFromStream[Package](file.content)
    //parse the paths
    val codeIdent = LocationResolver.parsePath(pkg.locations.code) match {
      case Some(f) => f
      case None => return None
    }

    val interfaceIdent = LocationResolver.parsePath(pkg.locations.interface) match {
      case Some(f) => f
      case None => return None
    }

    val sourceIdent = LocationResolver.parsePath(pkg.locations.source) match {
      case Some(f) => f
      case None => return None
    }

    //find the location where the byte code files for the package are/shpuld be stored
    val codeLoc = LocationResolver.resolveLocation(file.location,codeIdent) match {
      case Some(f) => f
      case None => return None
    }

    //find the location where the module interface files for the package are/shpuld be stored
    val interfaceLoc = LocationResolver.resolveLocation(file.location,interfaceIdent) match {
      case Some(f) => f
      case None => return None
    }

    //find the location where the source files for the package are stored
    val sourceLoc = LocationResolver.resolveLocation(file.location,sourceIdent) match {
      case Some(f) => f
      case None => return None
    }

    //process all the dependencies recursively
    val packages = pkg.dependencies.map(dep => {

      //parse the paths
      val depIdent = LocationResolver.parsePath(dep) match {
        case Some(f) => f
        case None => return None
      }

      //find the location of the dependency
      val dependecyFile = LocationResolver.resolveSource(file.location,depIdent,Some(Set(PackageEncoder.packageExtensionPrefix))) match {
        case Some(f) => f
        case None =>
          return None
      }

      //register the dependency (returns the Package object)
      PackageEncoder.deserializePackage(dependecyFile) match {
        case Some(r) => r
        case None => return None
      }
    })

    //process each Module entry

    val components = pkg.components.map { comp =>
      val meta = buildMeta(comp, codeLoc, sourceLoc, interfaceLoc)
      //load and register the interface of the entry
      //  this will check the signatures of the entry
      InterfaceEncoder.deserializeInterface(comp.info.language, comp.info.version, comp.info.classifier, meta.interface, meta) match {
        case Some(f) => f
        case None => return None
      }
    }



    //now that the dependencies are fine we can check this
    // transform the parse result into the shared internal structure for packages

    //  case class Package(name:String, hash: String, modules: Seq[Module], locations:Locations, dependencies:Seq[String])

    val newPkg = new LinkablePackage(
      true,
      file.location,
      Hash.fromString(pkg.hash),
      pkg.name,
      components,
      packages
    )

    //Now that everything is registered we can check the complete package
    PackageValidator.validatePackage(newPkg)

    //return package
    Some(newPkg)
  }

  private def buildMeta(link:StrongLink, codeLoc:Location, sourceLoc:Location, interfaceLoc:Location):Meta = {
    //get the byte code File
    val codeSource = LocationResolver.resolveSource(codeLoc, Path(NameGenerator.generateCodeName(link.name,  link.info.classifier)), Some(Set(ModuleSerializer.codeExtension)))
    //get the source File
    val sourceId = link.source match {
      case Some(Source(name, Some(extension))) => Identifier(name, extension)
      case Some(Source(name, None)) => Identifier(name)
      case None => unexpected("I hope we do not need this")//Identifier(m.name, m.info.classifier)
    }
    val sourceCodeSource = LocationResolver.resolveSource(sourceLoc, Path(sourceId))
    //get the interface File
    val interfaceSource = LocationResolver.resolveSource(
      interfaceLoc,
      Path(NameGenerator.generateInterfaceName(link.name,  link.info.classifier)),
      Some(Set(InterfaceEncoder.interfaceExtensionPrefix))
    )

    interfaceSource match {
      case Some(interfaceSource) => Meta(
        codeHash = link.hash.code.map(Hash.fromString),
        interfaceHash = Hash.fromString(link.hash.interface),
        sourceHash = Hash.fromString(link.hash.source),
        code = codeSource,
        interface = interfaceSource,
        sourceCode = sourceCodeSource
      )
      //todo:make an error out of this
      case None => throw new Exception("Interface not found")
    }
  }

  override def serializePackage(pkg: structure.LinkablePackage, workspace: Workspace): Boolean = {
    val out = LocationResolver.resolveSink(workspace.workspaceLocation, Identifier(pkg.name, PackageExtension(Json))).getOrElse(throw new Exception("NEED CUSTOM"))
    val repr = Serializer.toPackageRepr(pkg, workspace)
    out.write(writeToStream[Package](repr,_))
    true
  }
}
