package mandalac.plugin.impl.location.file

import java.io.{File, FileOutputStream, OutputStream}
import java.util.regex.Pattern

import mandalac.plugin.service.{LocationResolver, Selectors}
import mandalac.types.Path.{Absolute, Relative}
import mandalac.types.{Identifier, InputSource, Location, Path}

object FileLocationResolver {
  val Protocoll:String = "file"
  val prefix:String = LocationResolver.getProtocolHeader(Protocoll)
}


class FileLocationResolver extends LocationResolver{

  override def matches(s: Selectors.LocationSelector): Boolean = s match {
    case Selectors.Lookup(_, Absolute(FileLocationResolver.Protocoll, _)) => true
    case Selectors.Lookup(parent, Relative(elems)) =>(
      parent != null
      && parent.isInstanceOf[FileLocation]
      && elems.init.forall(e => e.isInstanceOf[Identifier.General]))
    case Selectors.Parse(LocationResolver.protocol(FileLocationResolver.Protocoll, _)) => true
    case Selectors.Serialize(_, target) => target != null && target.isInstanceOf[FileLocation]
    case Selectors.List(parent) => parent != null && parent.isInstanceOf[FileLocation]
    case Selectors.Default => true
    case _ => false
  }

  override def resolveLocation(parent:Location, path: Path): Option[FileLocation] = {
    if(path.elements.exists{
      case Identifier.Specific(_,_) => true
      case Identifier.General(_) => false
    }) return None

    val fullPath = (parent, path) match {
      case (parent:FileLocation, Path.Relative(relPath)) => parent.path ++ relPath
      case (_, Path.Absolute(FileLocationResolver.Protocoll, absolutePath))  => absolutePath
      case _ => return None
    }

    val pathString = fullPath.map{
      case Identifier.General(name) => name
    }.reduce((left, right) => left+File.separator+right)

    val file = new File(pathString)
    if(!file.exists() || !file.isDirectory) {
      None
    } else {
      Some(new FileLocation(fullPath,file))
    }
  }

  override def resolveSource(parent:Location, path: Path): Option[InputSource] = {
    if(path.elements.isEmpty) return None

    val folder = path match {
      case Path.Relative(elements) => Path.Relative(elements.init)
      case Path.Absolute(protocol, elements) => Path.Absolute(protocol, elements.init)
    }

    resolveLocation(parent, folder) match {
      case Some(directParent) =>
        val directFolder = directParent.file
        if(!directFolder.exists()) return None
        if(!directFolder.isDirectory) return None

        path.elements.last match {
          case Identifier.General(name) =>
            val allFiles = directFolder.listFiles().filter(f => {
              if(f.isFile) {
                val p = f.getName
                val parts = p.split('.')
                if(parts.length < 2) {
                  false
                } else {
                  parts(0) == name
                }
              } else {
                false
              }
            })
            if(allFiles.length != 1) return None
            val newIdent =  Identifier.Specific(name, allFiles(0).getName.drop(name.length+1))
            Some(new FileSource(parent.asInstanceOf[FileLocation],newIdent,allFiles(0)))
          case ident@Identifier.Specific(name, extension) =>
            val target = directFolder.getAbsolutePath + File.separator + name + "." + extension
            val file =  new File(target)
            if(!file.exists()) return None
            if(!file.isFile) return None
            Some(new FileSource(parent.asInstanceOf[FileLocation],ident,file))
        }
      case None => None
    }


  }

  override def listSources(parent: Location): Set[Identifier] = {
    val file = parent.asInstanceOf[FileLocation].file
    if(!file.exists() || !file.isDirectory) {
      Set.empty
    } else {
      file.listFiles().filter(f => f.isFile && f.getName.exists(c => c == '.')).map{f =>
        val parts = f.getName.split('.')
        Identifier.Specific(parts(0), f.getName.drop(parts(0).length+1))
      }.toSet
    }
  }

  override def listLocations(parent: Location): Set[Identifier] = {
    val file = parent.asInstanceOf[FileLocation].file
    if(!file.exists() || !file.isDirectory) {
      Set.empty
    } else {
      file.listFiles().filter(f => f.isDirectory && !f.getName.exists(c => c == '.')).map{f =>
        Identifier.General(f.getName)
      }.toSet
    }
  }

  override def resolveSink(parent: Location, ident: Identifier.Specific): Option[OutputStream] = {
    val folder = parent.asInstanceOf[FileLocation].file
    val target = folder.getAbsolutePath + File.separator + ident.name + "." + ident.extension
    val file = new File(target)
    if(file.isDirectory) {
      None
    } else {
      Some(new FileOutputStream(file))
    }
  }


  private def relativePath(parent: FileLocation, target: FileLocation): String = {
    parent.path.zip(target.path)
      .filter(tp => tp._1 != tp._2)
      .map(tp => tp._1.fullName)
      .reduce((p1,p2) => p1 + "/" + p2)
  }

  private def absolutePath(target: FileLocation): String =  FileLocationResolver.prefix + target.file.getCanonicalPath

  override def serializeLocation(parent: Location, target: Location): Option[String] = {
    val t = target.asInstanceOf[FileLocation]
    parent match {
      case p: FileLocation =>
        if (t.path.startsWith(p.path)) {
          Some(relativePath(p,t))
        } else {
          Some(absolutePath(t))
        }
      case _ => Some(absolutePath(t))
    }
  }

  override def provideDefault(): Option[Location] = {
    //todo: delegate to config
    val userDir = System.getProperty("user.dir")
    if(userDir == null) return None
    val file = new File(userDir)
    if(!file.exists()) return None
    if(!file.isDirectory) return None
    val path = file.getCanonicalPath.split(Pattern.quote(File.separator)).map(Identifier.General)
    Some(new FileLocation(path, file))
  }

  override def parsePath(ident: String): Option[Path] = {
    ident match {
      case LocationResolver.protocol(FileLocationResolver.Protocoll, path) =>
        val parts = LocationResolver.pathSeparator.split(path)
        if(parts.init.exists(elem => elem.contains('.'))) return None
        val pathIds = parts.init.map(elem => Identifier.General(elem))
        val nameExt = parts.last.split('.')
        val lastId = if(nameExt.length > 1) {
          Identifier.Specific(nameExt(0),parts.last.drop(nameExt(0).length+1))
        } else {
          Identifier.General(nameExt(0))
        }
        Some(Path.Absolute(FileLocationResolver.Protocoll,pathIds :+ lastId))
      case _ => None
    }
  }
}


