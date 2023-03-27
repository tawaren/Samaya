package samaya.plugin.impl.location.file

import samaya.plugin.impl.location.file.FileAddressResolver.Protocoll
import samaya.plugin.impl.location.vfs.VFSDirectory
import samaya.plugin.service.AddressResolver.{AbsoluteLocation, DirectoryMode, DynamicLocation, Exists, ReCreate, RelativeLocation}

import java.io.File
import java.util.regex.Pattern
import samaya.plugin.service.{AddressResolver, Selectors}
import samaya.types.Address.{Absolute, Relative}
import samaya.types.{Address, Addressable, ContentAddressable, Directory, GeneralSource, Identifier, InputSource, OutputTarget}

import scala.reflect.ClassTag

object FileAddressResolver {
  val Protocoll:String = "file"
  val prefix:String = AddressResolver.getProtocolHeader(Protocoll)
}

class FileAddressResolver extends AddressResolver{

  override def matches(s: Selectors.AddressSelector): Boolean = s match {
    case Selectors.Lookup(Absolute(Protocoll, _), _) => true
    case Selectors.Delete(target) => target.isInstanceOf[FileDirectory]
    case Selectors.Parse(AddressResolver.protocol(FileAddressResolver.Protocoll, _)) => true
    case Selectors.SerializeAddress(target, AddressResolver.AbsoluteLocation) => target.location.isInstanceOf[FileDirectory]
    case Selectors.SerializeAddress(target, AddressResolver.DynamicLocation(_ : FileDirectory)) => target.location.isInstanceOf[FileDirectory]
    case Selectors.SerializeAddress(target, AddressResolver.RelativeLocation(p : FileDirectory)) => target.location match {
      case t: FileDirectory => t.path.startsWith(p.path)
      case _ => false
    }
    case Selectors.SerializeDirectory(_ : FileDirectory, AddressResolver.AbsoluteLocation) => true
    case Selectors.SerializeDirectory(_ : FileDirectory, AddressResolver.DynamicLocation(_ : FileDirectory)) => true
    case Selectors.SerializeAddress(target : FileDirectory, AddressResolver.RelativeLocation(p : FileDirectory)) => target.path.startsWith(p.path)
    case Selectors.List(_ : FileDirectory) => true
    case Selectors.Default => true
    case _ => false
  }



  override def resolveDirectory(path: Address, mode:DirectoryMode = Exists): Option[FileDirectory] = {
    val fullPath = path match {
      case Address.Absolute(FileAddressResolver.Protocoll, absolutePath)  => absolutePath
      case _ => return None
    }

    val pathString = fullPath.map(_.fullName).reduce((left, right) => left+File.separator+right)

    val file = new File(pathString)
    if(mode == ReCreate && file.exists()) deleteDirectoryRecursive(file)
    if(mode != Exists && !file.exists()) file.mkdirs()
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



  override def resolve[T <: Addressable](path: Address, loader: AddressResolver.Loader[T]): Option[T] = {
    val elements = path match {
      case Address.Absolute(Protocoll, elements)  => elements
      case _ => return None
    }

    if(elements.isEmpty) return None
    val folder = Address.Absolute(Protocoll, elements.init)

    resolveDirectory(folder) match {
      case Some(directParent) =>
        val directFolder = directParent.file

        if(!directFolder.exists()) return None
        if(!directFolder.isDirectory) return None

        elements.last match {
          case g@Identifier.General(name, _) =>
            val namePrefix = g.partialName
            //Prefer exact match if available
            val allFiles = directFolder.list((_: File, fileName: String) => {
              fileName.startsWith(namePrefix) && fileName.split('.')(0) == name
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

  override def list(parent: Directory, filter:Option[AddressResolver.AddressKind]): Set[Identifier] = {
    def matchesFilter(file: File):Boolean = filter match {
      case Some(AddressResolver.Directory) => file.isDirectory
      case Some(AddressResolver.Element) => file.isFile
      case None => true
    }
    val file = parent.asInstanceOf[FileDirectory].file
    if(!file.exists()) {
      Set.empty
    } else {
      file.listFiles().filter(matchesFilter).map(f => Identifier.Specific(f.getName))
    }.toSet
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

  override def serializeContentAddress(target: ContentAddressable, mode: AddressResolver.SerializerMode): Option[String] = {
    serializeDirectoryAddress(target.location,mode).map(dir => dir +"/"+target.identifier.fullName)
  }

  override def serializeDirectoryAddress(target: Directory, mode: AddressResolver.SerializerMode): Option[String] = {
    val t : FileDirectory = target.asInstanceOf[FileDirectory]
    mode match {
      case RelativeLocation(parent:FileDirectory) if t.path.startsWith(parent.path) => Some(relativePath(parent,t))
      case DynamicLocation(parent:FileDirectory) if t.path.startsWith(parent.path) => Some(relativePath(parent,t))
      case DynamicLocation(_) => Some(absolutePath(t))
      case AbsoluteLocation => Some(absolutePath(t))
      case _ => None
    }
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
        val lastId = Identifier(parts.last)
        Some(Address.Absolute(FileAddressResolver.Protocoll,(pathIds :+ lastId).toSeq))
      case _ => None
    }
  }
}


