package samaya.compilation

import javax.lang.model.util.Elements.Origin
import samaya.structure.types.{Region, SourceId}

import scala.util.DynamicVariable

object ErrorManager {

  sealed trait ErrorLevel
  case object Error extends ErrorLevel
  case object Warning extends ErrorLevel
  case object Fatal extends ErrorLevel
  case object Info extends ErrorLevel

  trait Message {
    def level:ErrorLevel
  }

  private trait ErrorHandler {
    def record(err:Message)
  }

  private val logger:DynamicVariable[ErrorHandler] = new DynamicVariable(ConsoleLogger)
  private val formatter:DynamicVariable[ErrorFormatter] = new DynamicVariable(InterpolationFormatter)

  implicit class FormatHelper(val sc: StringContext) extends AnyVal {
    //will still compile but gives a feedback to the programmer
    def info(args: Any*): Message = formatter.value.generateMessage(sc,Info,args:_*)
    //will still compile but issue a warning
    def warn(args: Any*): Message = formatter.value.generateMessage(sc,Warning,args:_*)
    //will not compile and tell the programmer why
    def error(args: Any*): Message = formatter.value.generateMessage(sc,Error,args:_*)
    //makes continuation impossible
    def fatal(args: Any*): Message = formatter.value.generateMessage(sc,Fatal,args:_*)
  }

  case class ExceptionError(error:Exception) extends Message {
    override def toString: String = error.toString
    override def level: ErrorLevel = Fatal
  }

  case class PlainMessage(msg:String, level:ErrorLevel) extends Message {
    override def toString: String = level +": "+ msg
  }

  case class LocatedMessage(msg:String, origin:Region, level:ErrorLevel) extends Message {
    override def toString: String = s"$level: ${origin.start} $msg"
  }

  object LocatedMessage {
    def apply(msg:String, sources:Seq[SourceId], level:ErrorLevel):Seq[LocatedMessage] = sources.map(LocatedMessage(msg, _,  level))
    def apply(msg:String, source:SourceId, level:ErrorLevel):LocatedMessage = LocatedMessage(msg, source.origin,  level)
  }

  private case class UnexpectedErrorException(err:String) extends RuntimeException(err)

  //todo: Have a version in a file Module context
  def canProduceErrors(b: => Unit): Boolean = {
    val handler = new CheckedHandler(logger.value)
    try {
      logger.withValue(handler)(b)
    } catch {
      case exp@UnexpectedErrorException(err) =>
        //Todo: Only if enabled
        exp.printStackTrace()
        handler.record(PlainMessage(err,Fatal))

      case other:Exception =>
        //Todo: Only if enabled
        other.printStackTrace()
        handler.record(ExceptionError(other))
    }
    handler.hasError
  }

  def producesErrorValue[T]( b: => T): Option[T] = {
    val handler = new CheckedHandler(logger.value)
    try {
      val res = logger.withValue(handler)(b)
      if(handler.hasError){
        None
      } else {
        Some(res)
      }
    } catch {
      case exp@UnexpectedErrorException(err) =>
        exp.printStackTrace()
        handler.record(PlainMessage(err,Fatal))
        None
      case other:Exception =>
        other.printStackTrace()
        handler.record(ExceptionError(other))
        None
    }
  }

  //todo: unexpected_feedback
  //      means I expected the compiler to catch this but he did not please fix him
  //      we can have a production mode where this is not specially marked

  def feedback(err:Message): Unit = {
    logger.value.record(err)
  }

  def feedback(err:Seq[Message]): Unit = {
    err.foreach(feedback)
  }

  def unexpected(err:String): Nothing = {
    throw UnexpectedErrorException(err)
  }

  //default impls
  private object ConsoleLogger extends ErrorHandler {
    override def record(err: Message): Unit = {
      //for a linking formatter
      /*File f = new File(“./src/test/resource/testfiles/level_01/level_01_01/file_01_01_A.txt”);
      log.info(f.getAbsolutePath() + “:” + 34);  */
      import Console.{RED, BOLD, YELLOW, BLUE, RESET}
      err.level match {
        case Error => println(s"$RED$err$RESET")
        case Warning => println(s"$YELLOW$err$RESET")
        case Fatal => println(s"$RED$BOLD$err$RESET")
        case Info => println(s"$BLUE$err$RESET")
      }
    }
  }

  private object InterpolationFormatter extends ErrorFormatter {
    //todo: Make one that supports:
    //      Printing the whole function / adt / error / line & highlight the specified parts
    //      use the position infos in the Module to find the corresponding Function
    //      prints each function... only once and highlights
    //    Options: %c( ...... ) //after %c all the positions follow: c refers to context: adt, fun, error
    //                          // each context is printed once if .... refers to multiple instances
    //             %l( ...... ) //after %l all the positions follow: l refers to the line
    //                          // each line is printed once if .... refers to multiple instances
    //             %b( ...... ) //after %b all the positions follow: b refers to the block
    //                          // each block is printed once if .... refers to multiple disconnected instances (for connected the common parent is printed)
    override def generateMessage(sc: StringContext, level: ErrorLevel, args: Any*): Message = {
      PlainMessage(sc.s(args:_*),level)
    }
  }

  private class CheckedHandler(parent:ErrorHandler) extends ErrorHandler {
    var hasError = false
    override def record(err: Message): Unit = {
      err.level match {
        case Error => hasError = true
        case Fatal => hasError = true
        case _ =>
      }
      parent.record(err)
    }
  }

}
