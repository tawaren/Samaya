package samaya.plugin.impl.location.file

import java.io.{File, FileOutputStream, OutputStream}
import java.util.regex.Pattern
import samaya.plugin.service.{AddressResolver, PackageEncoder, Selectors}
import samaya.types.Address.{Absolute, Relative}
import samaya.types.{Address, Identifier, InputSource, Directory, OutputTarget}
import samaya.compilation.ErrorManager._
import samaya.structure.ContentAddressable
import samaya.types.Identifier.Specific;

object FileAddressResolver {
  val Protocoll:String = "file"
  val prefix:String = AddressResolver.getProtocolHeader(Protocoll)
}

class FileAddressResolver extends AddressResolver{

  override def matches(s: Selectors.AddressSelector): Boolean = s match {
    case Selectors.Lookup(_, Absolute(FileAddressResolver.Protocoll, _), _) => true
    case Selectors.Lookup(parent, Relative(elems), _) =>(
      parent != null
      && parent.isInstanceOf[FileDirectory]
      && elems.init.forall(e => e.isInstanceOf[Identifier.General]))
    case Selectors.Parse(AddressResolver.protocol(FileAddressResolver.Protocoll, _)) => true
    case Selectors.SerializeAddress(_, target, AddressResolver.Location) => target != null && target.location.isInstanceOf[FileDirectory]
    case Selectors.SerializeDirectory(_, target) => target != null && target.isInstanceOf[FileDirectory]
    case Selectors.List(parent) => parent != null && parent.isInstanceOf[FileDirectory]
    case Selectors.Default => true
    case _ => false
  }

  override def resolveDirectory(parent:Directory, path: Address, create:Boolean): Option[FileDirectory] = {
    val elements = path match {
      case loc: Address.LocationBased => loc.elements
      case _ => return None
    }

    if(elements.isEmpty) {
      return parent match {
        case location: FileDirectory => Some(location)
        case _ => None
      }
    }

    if(elements.exists{
      case Identifier.Specific(_,_) => true
      case Identifier.General(_) => false
    }) return None

    val fullPath = (parent, path) match {
      case (parent:FileDirectory, Address.Relative(relPath)) => parent.path ++ relPath
      case (_, Address.Absolute(FileAddressResolver.Protocoll, absolutePath))  => absolutePath
      case _ => return None
    }

    val pathString = fullPath.map{
      case Identifier.General(name) => name
        //Please Compiler
      case Identifier.Specific(_, _) => unexpected("should not happen", Always)
    }.reduce((left, right) => left+File.separator+right)

    val file = new File(pathString)
    if(create && !file.exists()) file.mkdirs()
    if(!file.exists() || !file.isDirectory) {
      None
    } else {
      Some(new FileDirectory(fullPath,file))
    }
  }

  override def resolve[T <: ContentAddressable](parent:Directory, path: Address, loader:AddressResolver.Loader[T], extensionFilter:Option[Set[String]] = None): Option[T] = {
    val elements = path match {
      case loc: Address.LocationBased => loc.elements
      case _ => return None
    }

    if(elements.isEmpty) return None
    val folder = path match {
      case Address.Relative(elements) => Address.Relative(elements.init)
      case Address.Absolute(protocol, elements) => Address.Absolute(protocol, elements.init)
      case _ => return None
    }

    resolveDirectory(parent, folder) match {
      case Some(directParent) =>
        val directFolder = directParent.file

        if(!directFolder.exists()) return None
        if(!directFolder.isDirectory)  return None

        elements.last match {
          case Identifier.General(name) =>
            val allFiles = directFolder.listFiles().filter(f => {
              if(f.isFile) {
                val p = f.getName
                val parts = p.split('.')
                if(parts.length < 2) {
                  false
                } else {
                  if(parts(0) != name) {
                    false
                  } else if(extensionFilter.nonEmpty){
                    parts.length >= 2 && extensionFilter.get.contains(parts(1))
                  } else {
                    true
                  }
                }
              } else {
                false
              }
            })
            if(allFiles.length != 1) return None
            val newIdent =  Identifier.Specific(name, allFiles(0).getName.drop(name.length+1))
            loader.load(new FileSource(directParent,newIdent,allFiles(0)))
          case ident@Identifier.Specific(name, extension) =>
            val target = directFolder.getAbsolutePath + File.separator + name + "." + extension
            val file =  new File(target)
            if(!file.exists()) return None
            if(!file.isFile) return None
            loader.load(new FileSource(directParent,ident,file))
        }
      case None => None
    }
  }

  override def listSources(parent: Directory): Set[Identifier] = {
    val file = parent.asInstanceOf[FileDirectory].file
    if(!file.exists() || !file.isDirectory) {
      Set.empty
    } else {
      file.listFiles().filter(f => f.isFile && f.getName.exists(c => c == '.')).map{f =>
        val parts = f.getName.split('.')
        Identifier.Specific(parts(0), f.getName.drop(parts(0).length+1))
      }.toSet
    }
  }

  override def listDirectories(parent: Directory): Set[Identifier] = {
    val file = parent.asInstanceOf[FileDirectory].file
    if(!file.exists() || !file.isDirectory) {
      Set.empty
    } else {
      file.listFiles().filter(f => f.isDirectory && !f.getName.exists(c => c == '.')).map{f =>
        Identifier.General(f.getName)
      }.toSet
    }
  }

  override def resolveSink(parent: Directory, ident: Identifier.Specific): Option[OutputTarget] = {
    val folder = parent.asInstanceOf[FileDirectory]
    val target = folder.file.getAbsolutePath + File.separator + ident.name + ident.extension.map(ext => "."+ext).getOrElse("")
    val file = new File(target)
    if(file.isDirectory) {
      None
    } else {
      Some(new FileTarget(folder,ident,file))
    }
  }


  private def relativePath(parent: FileDirectory, target: FileDirectory): String = {
    if(target.path.size == parent.path.size) {
      ""
    } else {
      target.path.drop(parent.path.size).map(p => p.fullName).reduceLeft((p1,p2) => p1 + "/" + p2)
    }
  }

  private def absolutePath(target: FileDirectory): String =  FileAddressResolver.prefix + target.file.getCanonicalPath

  override def serializeAddress(parent:Option[Directory], target:ContentAddressable): Option[String] = {
    serializeDirectory(parent, target.location).map(dir => dir +"/"+target.identifier.name)
  }

  override def serializeDirectory(parent:Option[Directory], target:Directory): Option[String] = {
    val t = target.asInstanceOf[FileDirectory]
    Some(parent match {
      case Some(p: FileDirectory) =>
        if (t.path.startsWith(p.path)) {
          relativePath(p,t)
        } else {
          absolutePath(t)
        }
      case _ => absolutePath(t)
    })
  }

  override def provideDefault(): Option[Directory] = {
    //todo: delegate to config
    val userDir = System.getProperty("user.dir")
    if(userDir == null) return None
    val file = new File(userDir)
    if(!file.exists()) return None
    if(!file.isDirectory) return None
    val path = file.getCanonicalPath.split(Pattern.quote(File.separator)).map(Identifier.General)
    Some(new FileDirectory(path, file))
  }

  override def parsePath(ident: String): Option[Address] = {
    ident match {
      case AddressResolver.protocol(FileAddressResolver.Protocoll, path) =>
        val parts = AddressResolver.pathSeparator.split(path)
        if(parts.init.exists(elem => elem != ".." && elem.contains('.'))) return None
        val pathIds = parts.init.map(elem => Identifier.General(elem))
        val nameExt = parts.last.split('.')
        val lastId = if(nameExt.length > 1) {
          Identifier.Specific(nameExt(0),parts.last.drop(nameExt(0).length+1))
        } else {
          Identifier.General(nameExt(0))
        }
        Some(Address.Absolute(FileAddressResolver.Protocoll,pathIds :+ lastId))
      case _ => None
    }
  }
}


