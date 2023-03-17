package samaya.plugin.impl.pkg.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import JsonModel._
import samaya.codegen.{ModuleSerializer, NameGenerator}
import samaya.compilation.ErrorManager.{Always, unexpected}
import samaya.plugin.service.PackageEncoder.PackageExtension
import samaya.structure.{Interface, LinkablePackage, Meta, ModuleInterface, Transaction, TransactionInterface}
import samaya.plugin.service.{InterfaceEncoder, AddressResolver, PackageEncoder, Selectors}
import samaya.structure.types.Hash
import samaya.structure
import samaya.types.{Identifier, InputSource, Directory, Address, Workspace}
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
    //base path
    val pkgFolder = pkg.path match {
      case Some(path) =>
        AddressResolver.parsePath(path) match {
          case Some(parsedPath) => AddressResolver.resolveDirectory(file.location,parsedPath) match {
            case Some(base) => base
            case None => return None
          }
          case None => return None
        }
      case None => file.location
    }

    //parse the paths
    val codeIdent = AddressResolver.parsePath(pkg.locations.code) match {
      case Some(f) => f
      case None => return None
    }

    val interfaceIdent = AddressResolver.parsePath(pkg.locations.interface) match {
      case Some(f) => f
      case None => return None
    }

    val sourceIdent = AddressResolver.parsePath(pkg.locations.source) match {
      case Some(f) => f
      case None => return None
    }

    //find the location where the byte code files for the package are/shpuld be stored
    val codeLoc = AddressResolver.resolveDirectory(pkgFolder,codeIdent) match {
      case Some(f) => f
      case None => return None
    }

    //find the location where the module interface files for the package are/shpuld be stored
    val interfaceLoc = AddressResolver.resolveDirectory(pkgFolder,interfaceIdent) match {
      case Some(f) => f
      case None => return None
    }

    //find the location where the source files for the package are stored
    val sourceLoc = AddressResolver.resolveDirectory(pkgFolder,sourceIdent) match {
      case Some(f) => f
      case None => return None
    }

    //process all the dependencies recursively
    val packages = pkg.dependencies.map(dep => {

      //parse the paths
      val depIdent = AddressResolver.parsePath(dep) match {
        case Some(f) => f
        case None => return None
      }

      //resolve and register the dependency (returns the Package object)
      AddressResolver.resolve(pkgFolder, depIdent, PackageEncoder.Loader) match {
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
      pkgFolder,
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

  private def buildMeta(link:StrongLink, codeLoc:Directory, sourceLoc:Directory, interfaceLoc:Directory):Meta = {
    //get the byte code File
    val codeSource = AddressResolver.resolve(codeLoc, Address(NameGenerator.generateCodeName(link.name,  link.info.classifier)), AddressResolver.InputLoader)
    //get the source File
    val sourceId = link.source match {
      case Some(Source(name, Some(extension))) => Identifier(name, extension)
      case Some(Source(name, None)) => Identifier(name)
      case None => unexpected("I hope we do not need this", Always)//Identifier(m.name, m.info.classifier)
    }
    val sourceCodeSource = AddressResolver.resolve(sourceLoc, Address(sourceId), AddressResolver.InputLoader)
    //get the interface File
    val interfaceSource = AddressResolver.resolve(interfaceLoc, Address(NameGenerator.generateInterfaceName(link.name,  link.info.classifier)), AddressResolver.InputLoader)

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

  override def serializePackage(pkg: structure.LinkablePackage, workspace: Workspace): Option[InputSource] = {
    val out = AddressResolver.resolveSink(workspace.location, Identifier(pkg.name, PackageExtension(Json))).getOrElse(throw new Exception("NEED CUSTOM"))
    val repr = Serializer.toPackageRepr(pkg, workspace)
    out.write(writeToStream[Package](repr,_))
    Some(out.toInputSource)
  }
}
