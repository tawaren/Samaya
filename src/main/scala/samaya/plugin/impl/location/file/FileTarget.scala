package samaya.plugin.impl.location.file

import samaya.structure.types.Hash

import java.io.{File, FileOutputStream, OutputStream}
import samaya.types.{Identifier, InputSource, OutputTarget}

import scala.util.Using


class FileTarget(override val location:FileDirectory, override val identifier:Identifier, val file:File) extends OutputTarget{
  override def toInputSource: InputSource = new FileSource(location, identifier, file)

  override def write[T](writer: OutputStream => T): T = {
    Using(new FileOutputStream(file))(writer).get
  }

}

