package samaya.plugin.config

import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{PlainMessage, Priority}
import samaya.plugin.config.ConfigValue.ResultWrapper

import scala.language.implicitConversions

trait ConfigValue[+T] {
  protected def getOption:Option[T]
  def value:T = getOption.get

  def default[V >: T](d: => V):ConfigValue[V] = new ResultWrapper[T,V](this)({
    case Some(v) => Some(v)
    case None => Some(d)
  })

  def map[V](f: T => V):ConfigValue[V]  = new ResultWrapper[T,V](this)(_.map(f))
  def flatMap[V](f: T => Option[V]):ConfigValue[V]  = new ResultWrapper[T,V](this)(_.flatMap(f))

}

object ConfigValue {

  implicit class BooleanConfigValue(val value:ConfigValue[Boolean]){
    def warnIfFalse(msg:String, priority: Priority):ConfigValue[Boolean] = {
      new ResultWrapper[Boolean,Boolean](value)({ v =>
        if (v.isDefined && !v.get) {
          ErrorManager.feedback(PlainMessage(msg, ErrorManager.Warning, priority))
        }
        v
      })
    }
  }

  implicit class StringConfigValue(val value:ConfigValue[String]){
    def asInt:ConfigValue[Int] = value.map(_.toInt)
    def asBool:ConfigValue[Boolean] = value.map(_.toBoolean)
    def asDouble:ConfigValue[Double] = value.map(_.toDouble)

    //Todo: asInt, asBoolean, ...
  }


  trait ConfigCell[T] extends ConfigValue[T] {
    protected var _value:Option[T] = None
    private[config] def configure(main:ParameterAndOptions, base:ParameterAndOptions, plugin:ParameterAndOptions):Unit ={
      _value = init(main,base,plugin)
    }
    protected def init(main:ParameterAndOptions, base:ParameterAndOptions, plugin:ParameterAndOptions):Option[T]
    override protected def getOption: Option[T] = _value
  }

  class ResultWrapper[F,T](value:ConfigValue[F])(f: Option[F] => Option[T]) extends ConfigValue[T] {
    override protected lazy val getOption: Option[T] = f(value.getOption)
  }
}
