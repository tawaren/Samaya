package samaya.plugin.impl.location.file

import java.io.{File, FileInputStream, InputStream}

import samaya.types.{Identifier, InputSource}


class FileSource(override val location:FileLocation, override val identifier:Identifier, val file:File) extends InputSource{
  override def content: InputStream = new FileInputStream(file)

  def canEqual(other: Any): Boolean = other.isInstanceOf[FileSource]
  override def equals(other: Any): Boolean = other match {
    case that: FileSource =>
      (that canEqual this) &&
        file == that.file
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(file)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

