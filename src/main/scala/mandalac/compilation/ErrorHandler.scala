package mandalac.compilation

import scala.util.DynamicVariable

object ErrorHandler {


  sealed trait ErrorLevel
  case object Error extends ErrorLevel
  case object Warning extends ErrorLevel
  case object Unexpected extends ErrorLevel
  case object Info extends ErrorLevel

  trait Message {
    def level:ErrorLevel;
  }

  case class SimpleMessage(msg:String, level:ErrorLevel) extends Message {
    override def toString: String = msg
  }

  case class ExceptionError(error:Exception) extends Message {
    override def toString: String = error.toString
    override def level: ErrorLevel = Unexpected
  }

  private case class UnexpectedErrorException(err:String) extends RuntimeException

  private trait ErrorHandler {
    def record(err:Message)
  }

  private object ConsoleLogger extends ErrorHandler {
    override def record(err: Message): Unit = println(err)
  }

  private val logger:DynamicVariable[ErrorHandler] = new DynamicVariable(ConsoleLogger)

  private class CheckedHandler(parent:ErrorHandler) extends ErrorHandler {
    var hasError = false
    override def record(err: Message): Unit = {
      err.level match {
        case Error => hasError = true
        case Unexpected => hasError = true
        case _ =>
      }
      parent.record(err)
    }
  }

  def canProduceErrors(b: => Unit): Boolean = {
    val handler = new CheckedHandler(logger.value)
    try {
      logger.withValue(handler)(b)
    } catch {
      case UnexpectedErrorException(err) => handler.record(SimpleMessage(err,Unexpected))
      case other:Exception => handler.record(ExceptionError(other))
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
      case UnexpectedErrorException(err) =>
        handler.record(SimpleMessage(err,Unexpected))
        None
      case other:Exception =>
        handler.record(ExceptionError(other))
        None
    }
  }

  def feedback(err:Message): Unit = {
    logger.value.record(err)
  }

  def unexpected(err:String): Nothing = {
    throw UnexpectedErrorException(err)
  }
}
