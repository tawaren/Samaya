package samaya.plugin.impl.location.vfs

import net.java.truevfs.access.{TFile, TFileInputStream, TFileOutputStream}

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import samaya.types.{Identifier, InputSource, OutputTarget}

import scala.util.Using


class VFSSource(override val location:VFSDirectory, override val identifier:Identifier, val file:TFile) extends InputSource{
  override def read[T](reader: InputStream => T): T = {
    Using(new TFileInputStream(file))(reader).get
  }


  override def copyTo(target: OutputTarget): Unit = target match {
    case trg: VFSTarget => {
      try {
        val in = new BufferedInputStream(new TFileInputStream(file))
        val out = new BufferedOutputStream(new TFileOutputStream(trg.file))
        try TFile.cp(in, out)
        finally {
          if (in != null) in.close()
          if (out != null) out.close()
        }
      }
    }
    case _ => super.copyTo(target)
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[VFSSource]
  override def equals(other: Any): Boolean = other match {
    case that: VFSSource =>
      (that canEqual this) &&
        file == that.file
    case _ => false
  }

  override def hashCode(): Int = file.hashCode()
  override def toString: String = file.getAbsolutePath
}

