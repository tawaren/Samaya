package samaya.plugin.impl.location.file

import java.io.File
import samaya.types.{Address, Directory, Identifier}
import samaya.compilation.ErrorManager.{Always, unexpected}
import samaya.types.Address.LocationBased

class FileDirectory(val path:Seq[Identifier], val file:File) extends Directory{
  override def name: String = file.getName
  override def toString: String = path.mkString("/")
  override def isRoot: Boolean = path.isEmpty

  override lazy val location: Directory = {
    if(path.isEmpty) {
      unexpected("The root directory does not have a parent directory", Always);
    } else{
      new FileDirectory( path.dropRight(1),file.getParentFile)
    }
  }

  def normalize(ids:Seq[Identifier]):LocationBased = {
    val builder = Seq.newBuilder[Identifier]
    builder.sizeHint(ids.length)
    var negates = 0;
    for(id <- ids.reverse) id match {
      case Identifier.General("..", None) => negates+=1
      case Identifier.Specific("..", None) => negates+=1
      case Identifier.General("", None) =>
      case Identifier.Specific("", None) =>
      case _ if negates != 0 => negates-=1
      case id => builder.addOne(id)
    }
    if(negates == 0) {
      Address.Absolute(FileAddressResolver.Protocoll, builder.result().reverse)
    } else {
      Address.Relative(ids.take(negates) ++ builder.result().reverse)
    }
  }

  override def resolveAddress(address: Address): Address = address match {
    case Address.Relative(elems) => normalize(path ++ elems)
    case Address.HybridAddress(target, Address.Relative(elems)) => Address.HybridAddress(target, normalize(path ++ elems))
    case rest => rest
  }

  override def identifier: Identifier = path.last

  def canEqual(other: Any): Boolean = other.isInstanceOf[FileDirectory]
  override def equals(other: Any): Boolean = other match {
    case that: FileDirectory =>
      (that canEqual this) &&
        file == that.file
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(file)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
