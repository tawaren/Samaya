package samaya.compilation

import samaya.compilation.ErrorManager._

trait ErrorFormatter {

  def generateMessage(sc:StringContext, level:ErrorLevel, args:Any*):Message

}
