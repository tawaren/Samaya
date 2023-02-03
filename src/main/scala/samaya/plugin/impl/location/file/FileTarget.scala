package samaya.plugin.impl.location.file

import java.io.{File, FileOutputStream, OutputStream}

import samaya.types.{Identifier, InputSource, OutputTarget}


class FileTarget(location:FileDirectory, identifier:Identifier, file:File) extends OutputTarget{
  override def toInputSource: InputSource = new FileSource(location, identifier, file)

  override def write[T](writer: OutputStream => T): T = {
    val out = new FileOutputStream(file)
     try {
      writer(out)
    } finally {
      out.close()
    }
  }
}

