package samaya.types

import java.io.OutputStream


trait OutputTarget extends GeneralSink {
  //should only be called after the sink is filled
  def toInputSource:InputSource
  def write[T](writer:OutputStream => T):T
}
