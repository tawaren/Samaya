package samaya.plugin.impl.location.vfs

import net.java.truevfs.access.{TArchiveDetector, TConfig, TFile}
import net.java.truevfs.comp.zipdriver.JarDriver
import samaya.plugin.impl.location.file.FileDirectory
import samaya.plugin.impl.location.vfs.VFSAddressResolver.{FileProtocol, SupportedProtocol}
import samaya.plugin.service.AddressResolver.{AbsoluteLocation, DirectoryMode, DynamicLocation, Exists, ReCreate, RelativeLocation}

import java.util.regex.Pattern
import samaya.plugin.service.{AddressResolver, Selectors}
import samaya.types.Address.Absolute
import samaya.types.{Address, Addressable, ContentAddressable, Directory, Identifier, OutputTarget}

import java.io.{File, IOException}
import scala.reflect.ClassTag

object VFSAddressResolver {
  val FileProtocol:String = "file"
  val HttpProtocol:String = "http"
  val HttpsProtocol:String = "https"
  //private val FtpProtocol:String = "ftp"
  //private val FtpsProtocol:String = "ftps"
  //Todo: Add Html & Ftp
  object SupportedProtocol {
    def unapply(protocol:String):Option[String] = protocol match {
      case FileProtocol => Some(protocol)
      case HttpProtocol => Some(protocol)
      case HttpsProtocol => Some(protocol)

      case _ => None
    }
    def default : String = FileProtocol
  }
}

class VFSAddressResolver extends AddressResolver{

  //Todo: Change to a custom archive in a custom extension
  //      Reason we can validate that the index exists
  TConfig.current().setArchiveDetector(new TArchiveDetector("sar", new JarDriver()));

  override def matches(s: Selectors.AddressSelector): Boolean = s match {
    case Selectors.Lookup(Absolute(SupportedProtocol(_), _), _) => true
    case Selectors.Delete(target) => target.isInstanceOf[VFSDirectory]
    case Selectors.Parse(AddressResolver.protocol(SupportedProtocol(_), _)) => true
    case Selectors.SerializeAddress(target, AddressResolver.AbsoluteLocation) => target.location.isInstanceOf[VFSDirectory]
    case Selectors.SerializeAddress(target, AddressResolver.DynamicLocation(_ : VFSDirectory)) => target.location.isInstanceOf[VFSDirectory]
    case Selectors.SerializeAddress(target, AddressResolver.RelativeLocation(p : VFSDirectory)) => target.location match {
      case t: VFSDirectory => t.path.startsWith(p.path)
      case _ => false
    }
    case Selectors.SerializeDirectory(_ : VFSDirectory, AddressResolver.AbsoluteLocation) => true
    case Selectors.SerializeDirectory(_ : VFSDirectory, AddressResolver.DynamicLocation(_ : VFSDirectory)) => true
    case Selectors.SerializeAddress(target : VFSDirectory, AddressResolver.RelativeLocation(p : VFSDirectory)) => target.path.startsWith(p.path)
    case Selectors.List(_ : VFSDirectory) => true
    case Selectors.Default => true
    case _ => false
  }


  override def resolveDirectory(path: Address, mode:DirectoryMode = Exists): Option[VFSDirectory] = {
    path match {
      case Address.Absolute(SupportedProtocol(protocol), fullPath)  =>
        val pathString = fullPath.map(_.fullName).reduce((left, right) => left+File.separator+right)
        val file = new TFile(pathString)
        if(mode == ReCreate && file.exists()) file.rm_r()
        if(mode != Exists && !file.exists()) file.mkdirs()
        if(!file.exists() || !file.isDirectory) {
          None
        } else {
          Some(new VFSDirectory(protocol,fullPath,file))
        }
      case _ => None
    }
  }

  private def load[T <: Addressable](parent:VFSDirectory, ident:Identifier, file:TFile, loader:AddressResolver.Loader[T]):Option[T] = {
    if(file.isFile){
      loader.load(new VFSSource(parent,ident,file))
    } else {
      loader.load(new VFSDirectory(parent.protocol, parent.path :+ ident,file))
    }
  }

  override def resolve[T <: Addressable](path: Address, loader: AddressResolver.Loader[T]): Option[T] = {
    path match {
      case Address.Absolute(SupportedProtocol(protocol), elements) =>

        if(elements.isEmpty) return None
        val folder = Address.Absolute(protocol, elements.init)

        resolveDirectory(folder) match {
          case Some(directParent) =>
            val directFolder = directParent.file

            if(!directFolder.exists()) return None
            if(!directFolder.isDirectory) return None

            elements.last match {
              case ident : Identifier.Specific =>
                val target = directFolder.getAbsolutePath + File.separator + ident.fullName
                val file =  new TFile(target)
                if(!file.exists()) return None
                load(directParent,ident,file, loader)
              case g@Identifier.General(name, _) =>
                val namePrefix = g.partialName
                //Prefer exact match if available
                val allFiles = directFolder.list((_: File, fileName: String) => {
                  fileName.startsWith(namePrefix) && fileName.split('.')(0) == name
                })

                implicit val classTag:ClassTag[T] = loader.tag
                val res = allFiles.flatMap(fileName => {
                  val file = new TFile(directFolder, fileName)
                  val newIdent =  Identifier(name, fileName.drop(name.length+1))
                  load(directParent,newIdent,file, loader)
                })

                if(res.length != 1) {
                  None
                } else {
                  res.headOption
                }
            }
          case None => None
        }
      case _ => None
    }
  }

  override def deleteDirectory(dir: Directory): Unit = {
    dir match {
      case directory: VFSDirectory => try {
        directory.file.rm_r()
      } catch {
        case e: IOException => { }
      }
      case _ =>
    }
  }

  override def list(parent: Directory, filter:Option[AddressResolver.AddressKind]): Set[Identifier] = {
    def matchesFilter(file: File):Boolean = filter match {
      case Some(AddressResolver.Directory) => file.isDirectory
      case Some(AddressResolver.Element) => file.isFile
      case None => true
    }
    val file = parent.asInstanceOf[VFSDirectory].file
    if(!file.exists()) {
      Set.empty
    } else {
      file.listFiles().filter(matchesFilter).map(f => Identifier.Specific(f.getName))
    }.toSet
  }

  override def resolveSink(parent: Directory, ident: Identifier.Specific): Option[OutputTarget] = {
    val folder = parent.asInstanceOf[VFSDirectory]
    val target = folder.file.getAbsolutePath + File.separator + ident.name + ident.extension.map(ext => "."+ext).getOrElse("")
    val file = new TFile(target)
    if(file.isDirectory) {
      None
    } else {
      Some(new VFSTarget(folder,ident,file))
    }
  }


  private def relativePath(parent: VFSDirectory, target: VFSDirectory): String = {
    if(target.path.size == parent.path.size) {
      ""
    } else {
      target.path.drop(parent.path.size).map(p => p.fullName).reduceLeft((p1,p2) => p1 + "/" + p2)
    }
  }

  private def absolutePath(target: VFSDirectory): String =  {
    AddressResolver.getProtocolHeader(target.protocol) + target.file.getCanonicalPath
  }


  override def serializeContentAddress(target: ContentAddressable, mode: AddressResolver.SerializerMode): Option[String] = {
    target.location match {
      case dir: VFSDirectory if dir.protocol == FileProtocol => serializeDirectoryAddress(target.location,mode).map(dir => dir +File.separator+target.identifier.fullName)
      case dir: VFSDirectory => serializeDirectoryAddress(target.location,mode).map(dir => dir +"/"+target.identifier.fullName)
      case _ => return None
    }
  }


  override def serializeDirectoryAddress(target: Directory, mode: AddressResolver.SerializerMode): Option[String] = {
    val t : VFSDirectory = target.asInstanceOf[VFSDirectory]
    mode match {
      case RelativeLocation(parent:VFSDirectory) if t.path.startsWith(parent.path) => Some(relativePath(parent,t))
      case DynamicLocation(parent:VFSDirectory) if t.path.startsWith(parent.path) => Some(relativePath(parent,t))
      case DynamicLocation(_) => Some(absolutePath(t))
      case AbsoluteLocation => Some(absolutePath(t))
      case _ => None
    }
  }

  override def provideDefault(): Option[Directory] = {
    //todo: delegate to config
    val userDir = System.getProperty("user.dir")
    if(userDir == null) return None
    val file = new TFile(userDir)
    if(!file.exists()) return None
    if(!file.isDirectory) return None
    val path = file.getCanonicalPath.split(Pattern.quote(File.separator)).map(Identifier.Specific.apply).toSeq
    Some(new VFSDirectory(SupportedProtocol.default, path, file))
  }


  override def parsePath(ident: String): Option[Address] = {
    ident match {
      case AddressResolver.protocol(SupportedProtocol(protocol), path) =>
        val parts = AddressResolver.pathSeparator.split(path)
        val pathIds = parts.init.map(Identifier.Specific.apply)
        val lastId = Identifier(parts.last)
        Some(Address.Absolute(protocol,(pathIds :+ lastId).toSeq))
      case _ => None
    }
  }
}


