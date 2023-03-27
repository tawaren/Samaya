package samaya.plugin.impl.location.file

import java.io.{File, FileInputStream, FileOutputStream, InputStream}
import samaya.types.{Identifier, InputSource, OutputTarget}
import scala.util.Using


class FileSource(override val location:FileDirectory, override val identifier:Identifier, val file:File) extends InputSource{
  override def read[T](reader: InputStream => T): T = {
    Using(new FileInputStream(file))(reader).get
  }

  override def copyTo(target: OutputTarget): Unit = target match {
    case trg: FileTarget => Using(new FileInputStream(file)){ input =>
      Using(new FileOutputStream(trg.file)){output =>
        val sourceChannel = input.getChannel
        output.getChannel.transferFrom(sourceChannel, 0, sourceChannel.size())
      }
    }
    case _ => super.copyTo(target)
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[FileSource]
  override def equals(other: Any): Boolean = other match {
    case that: FileSource =>
      (that canEqual this) &&
        file == that.file
    case _ => false
  }

  override def hashCode(): Int = file.hashCode()
  override def toString: String = file.getAbsolutePath
}

