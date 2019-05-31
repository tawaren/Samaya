package mandalac.plugin.impl.location.file

import java.io.File

import mandalac.types.{Identifier, Location}

class FileLocation(val path:Seq[Identifier], val file:File) extends Location{
  override def name: String = file.getName
}
