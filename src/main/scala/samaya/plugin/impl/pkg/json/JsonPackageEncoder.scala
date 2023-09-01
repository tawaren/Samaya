package samaya.plugin.impl.pkg.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import JsonModel._
import samaya.ProjectUtils.processWitContextRepo
import samaya.codegen.{ModuleSerializer, NameGenerator}
import samaya.compilation.ErrorManager.{Decoding, Error, PlainMessage, Warning, canProduceErrors, feedback}
import samaya.config.ConfigValue
import samaya.plugin.config.ConfigPluginCompanion
import samaya.plugin.impl.inter.json.JsonInterfaceEncoder.format
import samaya.plugin.service.AddressResolver.ContentExtensionLoader
import samaya.structure.{LinkablePackage, Meta}
import samaya.plugin.service.{AddressResolver, InterfaceEncoder, PackageEncoder, Selectors}
import samaya.structure.types.Hash
import samaya.structure
import samaya.types.Address.ContentBased
import samaya.types.{Address, Directory, GeneralSource, Identifier, InputSource, Workspace}

object JsonPackageEncoder extends ConfigPluginCompanion {
  val Json:String = "json"
  val format: ConfigValue[String] = arg("package.encoder.format|encoder.format|format").default(Json)
}

import samaya.plugin.impl.pkg.json.JsonPackageEncoder._

//A Package Manager for a json description of a package
class JsonPackageEncoder extends PackageEncoder {

  val ext : String = PackageEncoder.packageExtensionPrefix+"."+Json
  override def matches(s: Selectors.PackageSelector): Boolean =  s match {
    case Selectors.PackageDecoderSelector(input : InputSource) => input.identifier.extension.contains(ext)
    case Selectors.PackageDecoderSelector(dir : Directory) => AddressResolver.resolve(dir.resolveAddress(Address(dir.identifier.name,ext)), AddressResolver.InputLoader).isDefined
    case Selectors.PackageEncoderSelector if format.value == Json => true
    case _ => false
  }

  //todo: use error handler
  //todo: Make more stuff Optional in Module Meta
  def deserializePackage(source: GeneralSource): Option[structure.LinkablePackage] = {
    val file = source match {
      case source: InputSource => source
      case dir : Directory => AddressResolver.resolve(dir.resolveAddress(Address(dir.identifier.name, ext)), AddressResolver.InputLoader) match {
        case Some(source) => source
        case None => return None
      }
      case _ => return None
    }
    //Needed to lookup an accompanioning Repository
    processWitContextRepo(file.location) {
      //parse the package with Package as description
      val pkg = file.read(readFromStream[Package](_))
      //base path
      val pkgFolder = pkg.path match {
        case Some(path) =>
          AddressResolver.parsePath(path) match {
            case Some(parsedPath) => AddressResolver.resolveDirectory(file.location.resolveAddress(parsedPath)) match {
              case Some(base) => base
              case None => return None
            }
            case None =>
              feedback(PlainMessage(s"Package path ${pkg.path} has wrong format", Error, Decoding()))
              return None
          }
        case None => file.location
      }

      //find the location where the byte code files for the package are/shpuld be stored
      val codeLoc = pkg.locations
        .flatMap(l => AddressResolver.parsePath(l.code))
        .flatMap(ident => AddressResolver.resolveDirectory(pkgFolder.resolveAddress(ident)))
      val interfaceLoc = pkg.locations
        .flatMap(l => AddressResolver.parsePath(l.interface))
        .flatMap(ident => AddressResolver.resolveDirectory(pkgFolder.resolveAddress(ident)))
      val sourceLoc = pkg.locations
        .flatMap(l => AddressResolver.parsePath(l.source))
        .flatMap(ident => AddressResolver.resolveDirectory(pkgFolder.resolveAddress(ident)))

      //process all the dependencies recursively

      val packages = pkg.dependencies.map(dep => {

        //parse the paths
        val depIdent = AddressResolver.parsePath(dep) match {
          case Some(f) => f
          case None =>
            feedback(PlainMessage(s"Package path ${dep} has wrong format", Error, Decoding()))
            return None
        }

        //resolve and register the dependency (returns the Package object)
        AddressResolver.resolve(pkgFolder.resolveAddress(depIdent), PackageEncoder.Loader) match {
          case Some(r) => r
          case None => feedback(PlainMessage(s"Could not load package dependency ${depIdent} - Maybe a repository is missing or the package is invalid", Warning, Decoding()))
            return None
        }
      })

      //process each Module entry

      val components = pkg.components.map { comp =>
        val meta = buildMeta(comp, codeLoc, sourceLoc, interfaceLoc)

        val interfaceMeta = meta.interface match {
          case Some(_) => meta
          case None => AddressResolver.resolve(ContentBased(meta.interfaceHash), AddressResolver.InputLoader) match {
            case Some(interSrc) => meta.copy(interface = Some(interSrc))
            case None =>
              feedback(PlainMessage(s"Could not locate component interface with hash ${meta.interfaceHash} - Maybe a repository is missing or the package is invalid", Error, Decoding()))
              return None
          }
        }
        //load and register the interface of the entry
        //  this will check the signatures of the entry
        InterfaceEncoder.deserializeInterface(comp.info.language, comp.info.version, comp.info.classifier, interfaceMeta.interface.get, interfaceMeta) match {
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
        packages,
        pkg.includes
      )

      //return package
      Some(newPkg)
    }
  }

  private def buildMeta(link:StrongLink, codeLoc:Option[Directory], sourceLoc:Option[Directory], interfaceLoc:Option[Directory]):Meta = {
    //get the byte code File - we need to add the extension in case debug files are present
    val codeSource = codeLoc match {
      //todo: at least check here that the hash is right - unless checked in validation
      case Some(codeLoc) => AddressResolver.resolve(codeLoc.resolveAddress(Address(NameGenerator.generateCodeName(link.name,  link.info.classifier))), ContentExtensionLoader(AddressResolver.InputLoader,  ModuleSerializer.codeExtension))
      //Todo: Make this the default and the other the fallback
      case None => link.hash.code.flatMap(code => AddressResolver.resolve(ContentBased(Hash.fromString(code)), ContentExtensionLoader(AddressResolver.InputLoader,  ModuleSerializer.codeExtension)))
    }

    //get the source File
    val sourceCodeSource = sourceLoc match {
      //todo: at least check here that the hash is right - unless checked in validation
      case Some(sourceLoc) => link.source match {
        case Some(path) => AddressResolver.resolve(sourceLoc.resolveAddress(Address(path)), AddressResolver.InputLoader)
        case None => AddressResolver.resolve(ContentBased(Hash.fromString(link.hash.source)), AddressResolver.InputLoader)
      }
      //Todo: Make this the default and the other the fallback
      case None => AddressResolver.resolve(ContentBased(Hash.fromString(link.hash.source)), AddressResolver.InputLoader)
    }

    //get the interface File
    val interfaceSource = interfaceLoc match {
      //todo: at least check here that the hash is right - unless checked in validation
      case Some(interfaceLoc) => AddressResolver.resolve(interfaceLoc.resolveAddress(Address(NameGenerator.generateInterfaceName(link.name,  link.info.classifier))), ContentExtensionLoader(AddressResolver.InputLoader, InterfaceEncoder.interfaceExtensionPrefix))
      //Todo: Make this the default and the other the fallback
      case None => AddressResolver.resolve(ContentBased(Hash.fromString(link.hash.interface)), ContentExtensionLoader(AddressResolver.InputLoader, InterfaceEncoder.interfaceExtensionPrefix))
    }

    Meta(
      codeHash = link.hash.code.map(Hash.fromString),
      interfaceHash = Hash.fromString(link.hash.interface),
      sourceHash = Hash.fromString(link.hash.source),
      code = codeSource,
      interface = interfaceSource,
      sourceCode = sourceCodeSource
    )
  }

  override def serializePackage(pkg: structure.LinkablePackage, workspace: Option[Workspace]): Option[InputSource] = {
    val out = AddressResolver.resolveSink(pkg.location, Identifier.Specific(pkg.name, ext)).getOrElse(throw new Exception("NEED CUSTOM"))
    val repr = Serializer.toPackageRepr(pkg, workspace)
    out.write(writeToStream[Package](repr,_))
    Some(out.toInputSource)
  }
}
