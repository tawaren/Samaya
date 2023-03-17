package samaya.plugin.impl.location.file

import java.io.File
import samaya.types.{Directory, Identifier}
import samaya.compilation.ErrorManager.{Always, unexpected}

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
