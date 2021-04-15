package samaya.compilation

import samaya.compilation.ErrorManager._

trait ErrorFormatter {

  def generateMessage(sc:StringContext, level:ErrorLevel, priority: Priority, args:Any*):Message

}
