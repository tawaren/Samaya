package mandalac.plugin.impl.location.file

import java.io.{File, FileInputStream, InputStream}

import mandalac.types.{Identifier, InputSource}


class FileSource(override val location:FileLocation, override val identifier:Identifier, file:File) extends InputSource{
  override def content: InputStream = new FileInputStream(file)
}

