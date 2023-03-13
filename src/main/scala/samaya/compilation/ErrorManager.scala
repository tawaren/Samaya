package samaya.compilation

import samaya.structure.types.{Region, SourceId}

import scala.util.DynamicVariable

object ErrorManager {

  sealed trait ErrorLevel
  case object Error extends ErrorLevel
  case object Warning extends ErrorLevel
  case object Fatal extends ErrorLevel
  case object Info extends ErrorLevel

  private val priorityOrdering = Ordering.by[Priority,Int](_.primary).thenComparingInt(_.secondary)

  sealed trait Priority extends Comparable[Priority]{
    def primary:Int
    def secondary:Int
    override def compareTo(o: Priority): Int = priorityOrdering.compare(this,o)
  }

  //Special is excluded
  case object Always extends Priority{override def primary: Int = -1; override def secondary: Int = 0}

  case class Builder(override val secondary: Int = 100) extends Priority{override def primary: Int = 7}
  case class Deployer(override val secondary: Int = 100) extends Priority{override def primary: Int = 6}
  case class SourceParsing(override val secondary: Int = 100) extends Priority{override def primary: Int = 5}
  case class Compiler(override val secondary: Int = 100) extends Priority{override def primary: Int = 4}
  case class InterfaceParsing(override val secondary: Int = 100) extends Priority{override def primary: Int = 3}
  case class Checking(override val secondary: Int = 100) extends Priority{override def primary: Int = 2}
  case class InterfaceGen(override val secondary: Int = 100) extends Priority{override def primary: Int = 1}
  case class CodeGen(override val secondary: Int = 100) extends Priority{override def primary: Int = 0}

  case object Unused extends Priority{override def primary: Int = -1; override def secondary: Int = 0}

  trait Message {
    def level:ErrorLevel
    def priority:Priority
  }

  private trait ErrorHandler {
    def record(err:Message): Unit
    def finish(): Unit
  }

  private val logger:DynamicVariable[ErrorHandler] = new DynamicVariable(ConsoleLogger)

  case class ExceptionError(error:Exception) extends Message {
    override def toString: String = error.toString
    override def level: ErrorLevel = Fatal
    override val priority: Priority = Always
  }

  case class PlainMessage(msg:String, level:ErrorLevel, override val priority: Priority) extends Message {
    override def toString: String = level.toString +": "+ msg
  }

  case class LocatedMessage(msg:String, origin:Region, level:ErrorLevel, override val priority: Priority) extends Message {
    override def toString: String = s"$level: ${origin.start} $msg"
  }

  object LocatedMessage {
    def apply(msg:String, sources:Seq[SourceId], level:ErrorLevel, priority: Priority):Seq[LocatedMessage] = sources.map(LocatedMessage(msg, _,  level, priority))
    def apply(msg:String, source:SourceId, level:ErrorLevel, priority: Priority):LocatedMessage = LocatedMessage(msg, source.origin,  level, priority)
  }

  private case class UnexpectedErrorException(err:String, priority: Priority) extends RuntimeException(err)

  def newPlainErrorScope[T]( b: => T): T = {
    val delayer = new DelayedPriorityLogger(logger.value)
    try {
      val res = logger.withValue(delayer)(b)
      delayer.finish()
      res
    } catch {
      case exp@UnexpectedErrorException(err,priority) =>
        delayer.record(PlainMessage(err,Fatal,priority))
        delayer.finish()
        throw exp;
      case other:Exception =>
        other.printStackTrace()
        delayer.record(ExceptionError(other))
        delayer.finish()
        throw other;
    }
  }

  //todo: Have a version in a file Module context
  def canProduceErrors(b: => Unit): Boolean = {
    val handler = new CheckedHandler(logger.value)
    val delayer = new DelayedPriorityLogger(handler)
    try {
      logger.withValue(delayer)(b)
      delayer.finish()
    } catch {
      case UnexpectedErrorException(err, priority) =>
        delayer.record(PlainMessage(err,Fatal,priority))
        delayer.finish()
      case other:Exception =>
        //Todo: Only if enabled
        other.printStackTrace()
        delayer.record(ExceptionError(other))
        delayer.finish()
    }
    handler.hasError
  }

  def producesErrorValue[T]( b: => T): Option[T] = {
    val handler = new CheckedHandler(logger.value)
    val delayer = new DelayedPriorityLogger(handler)
    try {
      val res = logger.withValue(delayer)(b)
      delayer.finish()
      if(handler.hasError){
        None
      } else {
        Some(res)
      }
    } catch {
      case UnexpectedErrorException(err,priority) =>
        delayer.record(PlainMessage(err,Fatal,priority))
        delayer.finish()
        None
      case other:Exception =>
        other.printStackTrace()
        delayer.record(ExceptionError(other))
        delayer.finish()
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

  def unexpected(err:String, priority: Priority): Nothing = {
    throw UnexpectedErrorException(err, priority)
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
    //Nothing todo
    override def finish(): Unit = ()
  }

  private class DelayedPriorityLogger(parent:ErrorHandler) extends ErrorHandler {
    private var highestPrio:Priority = Unused
    private var messages = Seq[Message]()
    override def record(err: Message): Unit = {
      if(err.priority == Always) parent.record(err)
      val res = highestPrio.compareTo(err.priority)
      if(res == 0) {
        messages = messages :+ err
      } else if(res < 0) {
        highestPrio = err.priority
        messages = Seq(err)
      }
    }
    override def finish(): Unit = {
      messages.foreach(parent.record)
      messages = Seq()
      parent.finish()
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
    override def finish(): Unit = parent.finish()
  }

}
