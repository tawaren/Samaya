package samaya.plugin.impl.location.vfs

import net.java.truevfs.access.{TFile, TFileOutputStream}

import java.io.{File, FileOutputStream, OutputStream}
import samaya.types.{Identifier, InputSource, OutputTarget}

import scala.util.Using


class VFSTarget(override val location:VFSDirectory, override val identifier:Identifier, val file:TFile) extends OutputTarget{
  override def toInputSource: InputSource = new VFSSource(location, identifier, file)

  override def write[T](writer: OutputStream => T): T = {
    Using(new TFileOutputStream(file))(writer).get
  }
}

