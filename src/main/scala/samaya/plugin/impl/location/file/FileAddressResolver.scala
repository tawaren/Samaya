package samaya.plugin.impl.location.file

import java.io.File
import java.util.regex.Pattern
import samaya.plugin.service.{AddressResolver, Selectors}
import samaya.types.Address.{Absolute, Relative}
import samaya.types.{Address, Addressable, ContentAddressable, Directory, Identifier, InputSource, OutputTarget}

import scala.reflect.ClassTag

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
      && elems.init.forall(e => e.isInstanceOf[Identifier.Specific]))
    case Selectors.Delete(target) => target.isInstanceOf[FileDirectory]
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

    val fullPath = (parent, path) match {
      case (parent:FileDirectory, Address.Relative(relPath)) => parent.path ++ relPath
      case (_, Address.Absolute(FileAddressResolver.Protocoll, absolutePath))  => absolutePath
      case _ => return None
    }

    val pathString = fullPath.map(_.fullName).reduce((left, right) => left+File.separator+right)

    val file = new File(pathString)
    if(create && !file.exists()) file.mkdirs()
    if(!file.exists() || !file.isDirectory) {
      None
    } else {
      Some(new FileDirectory(fullPath,file))
    }
  }

  private def load[T <: Addressable](parent:FileDirectory, ident:Identifier, file:File,loader:AddressResolver.Loader[T]):Option[T] = {
    if(file.isFile){
      loader.load(new FileSource(parent,ident,file))
    } else {
      loader.load(new FileDirectory(parent.path :+ ident,file))
    }
  }

  override def resolve[T <: Addressable](parent: Directory, path: Address, loader: AddressResolver.Loader[T]): Option[T] = {
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
        if(!directFolder.isDirectory) return None

        elements.last match {
          case Identifier.General(name) =>
            //Prefer exact match if available
            val allFiles = directFolder.list((_: File, fileName: String) => {
              fileName.split('.')(0) == name
            })

            implicit val classTag:ClassTag[T] = loader.tag
            val res = allFiles.flatMap(fileName => {
              val file = new File(directFolder, fileName)
              val newIdent =  Identifier(name, fileName.drop(name.length+1))
              load(directParent,newIdent,file, loader)
            })

            if(res.length != 1) {
              None
            } else {
              res.headOption
            }
          case ident : Identifier.Specific =>
            val target = directFolder.getAbsolutePath + File.separator + ident.fullName
            val file =  new File(target)
            if(!file.exists()) return None
            load(directParent,ident,file, loader)
        }
      case None =>  None
    }
  }

  private def deleteDirectoryRecursive(directoryToBeDeleted: File): Boolean = {
    val allContents = directoryToBeDeleted.listFiles
    if (allContents != null) {
      for (file <- allContents) {
        deleteDirectoryRecursive(file)
      }
    }
    directoryToBeDeleted.delete
  }

  override def deleteDirectory(dir: Directory): Unit = {
    dir match {
      case directory: FileDirectory => deleteDirectoryRecursive(directory.file)
      case _ =>
    }
  }

  override def listSources(parent: Directory): Set[Identifier] = {
    val file = parent.asInstanceOf[FileDirectory].file
    if(!file.exists() || !file.isDirectory) {
      Set.empty
    } else {
      file.listFiles().filter(f => f.isFile && f.getName.exists(c => c == '.')).map{f =>
        val parts = f.getName.split('.')
        Identifier(parts(0), f.getName.drop(parts(0).length+1))
      }.toSet
    }
  }

  override def listDirectories(parent: Directory): Set[Identifier] = {
    val file = parent.asInstanceOf[FileDirectory].file
    if(!file.exists() || !file.isDirectory) {
      Set.empty
    } else {
      file.listFiles().filter(f => f.isDirectory && !f.getName.exists(c => c == '.')).map{f =>
        Identifier.Specific(f.getName)
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
    val path = file.getCanonicalPath.split(Pattern.quote(File.separator)).map(Identifier.Specific.apply).toSeq
    Some(new FileDirectory(path, file))
  }


  override def parsePath(ident: String): Option[Address] = {
    ident match {
      case AddressResolver.protocol(FileAddressResolver.Protocoll, path) =>
        val parts = AddressResolver.pathSeparator.split(path)
        if(parts.init.exists(elem => elem != ".." && elem.contains('.'))) return None
        val pathIds = parts.init.map(Identifier.Specific.apply)
        val nameExt = parts.last.split('.')
        val lastId = if(nameExt.length > 1) {
          Identifier.Specific(parts.last)
        } else if(AddressResolver.pathSeparator.matches(""+ident.last)){
          Identifier.Specific(nameExt(0), None)
        } else {
          Identifier.General(nameExt(0))
        }
        Some(Address.Absolute(FileAddressResolver.Protocoll,(pathIds :+ lastId).toSeq))
      case _ => None
    }
  }
}


