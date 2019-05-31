package mandalac.plugin.impl.pkg.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import JsonModel._
import mandalac.structure.{Module, ModuleMeta}
import mandalac.registries.PackageRegistry
import mandalac.plugin.service.{InterfaceManager, LocationResolver, PackageManager, Selectors}
import mandalac.structure.types.Hash
import mandalac.types
import mandalac.types.{Identifier, InputSource, Location, Path, Workspace}
import mandalac.validation.PackageValidator

//A Package Manager for a json description of a package
class JsonPackageManager extends PackageManager {
  //This plugin handles everything with ".json"
  // Todo: Maybe a more precise file type ?? alla pckg.json
  //       Ev even parse header and look into it???
  override def matches(s: Selectors.PackageSelector): Boolean = {
    s match {
      case Selectors.PackageDeserializationSelector(source) => source.identifier.extension.contains("json")
      case Selectors.PackageSerializationSelector(format) => format == "json"
    }

  }

  //todo: use error handler
  def registerPackage(parent:Seq[Identifier], file: InputSource): Option[types.Package] = {

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

    //calculate the new path of this package
    val packageNamePath = parent :+ Identifier(pkg.name)

    //process all the dependencies recursively
    val packages = pkg.dependencies.map(dep => {

      //parse the paths
      val depIdent = LocationResolver.parsePath(dep) match {
        case Some(f) => f
        case None => return None
      }

      //find the location of the dependency
      val dependecyFile = LocationResolver.resolveSource(file.location,depIdent) match {
        case Some(f) => f
        case None => return None
      }

      //register the dependency (returns the Package object)
      PackageManager.registerPackage(packageNamePath,dependecyFile) match {
        case Some(r) => r
        case None => return None
      }
    })

    //process each Module entry
    val metas = pkg.modules.flatMap(m => {
      //get the byte code File
      val codeSource = LocationResolver.resolveSource(codeLoc, Path(m.name))
      //get the source File
      val sourceCodeSource = LocationResolver.resolveSource(sourceLoc,Path(m.name))
      //get the interface File
      LocationResolver.resolveSource(interfaceLoc, Path(m.name)) match {
        case Some(interfaceSource) => Some(ModuleMeta(
          path = parent,
          name = m.name,
          codeHash = Hash.fromString(m.hash.code),
          interfaceHash = Hash.fromString(m.hash.interface),
          sourceHash = Hash.fromString(m.hash.source),
          code = codeSource,
          interface = interfaceSource,
          sourceCode = sourceCodeSource
        ))
        //todo:make an error out of this
        case None => None
      }
    })

    var modules = Seq.newBuilder[Module]
    modules.sizeHint(metas.size)
    //go over each entry and check it
    for(meta <- metas){
      //load and register the interface of the entry
      //  this will check the signatures of the entry

      InterfaceManager.registerInterface(meta.interface, meta) match {
        case Some(f) => modules += f
        case None => return None
      }

      //Note: Code and Source will be validated by validatePackage
      //  interface is here as it has to be registered
    }

    //now that the dependencies are fine we can check this
    // transform the parse result into the shared internal structure for packages
    val newPkg = new PackageImpl(pkg,parent, modules.result(), packages)



    //Now that everithing is registered we can check the complete package
    PackageValidator.validatePackage(newPkg)
    // if everything was successfull we register it
    PackageRegistry.recordPackage(packageNamePath,newPkg)
    //return package
    Some(newPkg)
  }

  override def serializePackage(parent:Location, pkg: types.Package, workspace: Workspace): Boolean = {
    val out = LocationResolver.resolveSink(parent, Identifier(workspace.name, "json")).getOrElse(throw new Exception("NEED CUSTOM"))
    val repr = Serializer.toPackageRepr(parent, pkg, workspace)
    writeToStream[Package](repr, out)
    true
  }
}
