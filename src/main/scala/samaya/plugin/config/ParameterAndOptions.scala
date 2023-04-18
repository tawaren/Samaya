package samaya.plugin.config

import io.circe.{Json, JsonObject}

import java.util.Properties
import scala.collection.mutable

//Tool that aggregates command line arguments
// usable for merging multiple for plugins
class ParameterAndOptions(val parameters:Seq[String], val options:Map[String, Seq[String]]) {
  def format(
              valueSep:String ="=",
              sequenceSep:String=";",
              optionSep:String=" ",
              toKey: String => String = key => if(key.length==1) "-"+key else "--"+key
            ):String = {

    def quote(v:String, quote:String = "\""):String = {
      if(v.contains(" ")){
        quote+v+quote
      } else {
        v
      }
    }

    val params = parameters.map(quote(_)).mkString(optionSep)
    val opts = options.map{
      case (k,Seq()) => toKey(k)
      case (k,Seq(v)) => toKey(k)+valueSep+quote(v)
      case (k,vs) => toKey(k)+valueSep+"\""+vs.map(quote(_, "'")).mkString(sequenceSep)+"\""
    }
    params+optionSep+opts.mkString(optionSep)
  }

  override def toString:String = format()

  def dropParams(num:Int):ParameterAndOptions = new ParameterAndOptions(parameters.drop(num), options)
  def addParams(pms:mutable.Seq[String]):ParameterAndOptions = new ParameterAndOptions(parameters ++ pms, options)

}

object ParameterAndOptions {

  def joinQuote(args:Array[String], quote:String, join:String):Array[String] = {
    val arrBuilder = Array.newBuilder[String]
    var i:Int = 0
    while (i < args.length) {
      val cur = args(i)
      i+=1
      if(cur.startsWith(quote)) {
        var _cur = cur.drop(quote.length)
        while (!_cur.endsWith(quote)) {
          if (i >= args.length) throw new Exception("Unterminated quotation in command line arguments")
          _cur = _cur + join + args(i)
          i += 1
        }
        arrBuilder.addOne(_cur.dropRight(quote.length))
      } else {
        arrBuilder.addOne(cur)
      }
    }
    arrBuilder.result()
  }

  val separators = "[,;:]"
  def parseList(value:String):Seq[String] = {
    val seq = Seq.newBuilder[String]
    val quoteSplit = value.split("'")
    var ii:Int = 0
    while (ii < quoteSplit.length) {
      //todo: makes problem for invalid cornercases like "a,b,v'1,2'"
      seq.addAll(quoteSplit(ii).split(separators).map(_.trim).filter(_.nonEmpty))
      ii+=1
      if(ii < quoteSplit.length){
        seq.addOne(quoteSplit(ii))
      }
      ii+=1
    }
    seq.result()
  }

  def apply(_args:Array[String]):ParameterAndOptions = {
    val parameters = Seq.newBuilder[String]
    val options = mutable.Map.empty[String,Seq[String]]

    def addOption(key:String,value:Seq[String]):Unit = {
      options.put(key.trim,options.get(key) match {
        case Some(oldValue) => oldValue ++ value
        case None => value
      })
    }

    val args:Array[String] = joinQuote(_args.flatMap( e => e.split("=", 2) match {
      case arr if arr.length <= 1 => arr
      case arr if arr(0).contains('"') => Array(arr.mkString("="))
      case arr => arr
    }),"\"", " ")

    var key:String = null
    def processOption(arg:String): Unit ={
      if(key != null) {
        addOption(key,Seq("true"))
        key = null
      }
      val splitted = arg.split("=", 2)
      if(splitted.length == 2){
        addOption(splitted(0),parseList(splitted(1)))
      } else {
        key = splitted(0)
      }
    }

    var i:Int = 0
    while(i < args.length) {
      val arg = args(i)
      i+=1
      if(arg.startsWith("--")){
        processOption(arg.drop(2))
      } else if(arg.length == 2 && arg.startsWith("-")){
        processOption(arg.drop(1))
      } else {
        //is a value
        if(key != null) {
          addOption(key,parseList(arg))
        } else {
          parameters.addOne(arg)
        }
      }
    }
    if(key != null){
      addOption(key,Seq("true"))
    }
    new ParameterAndOptions(parameters.result(),options.toMap)
  }

  def apply(props:Properties):ParameterAndOptions = {
    val options = Map.newBuilder[String, Seq[String]]
    props.forEach{
      case (key :String, value : String) => options.addOne((key, parseList(value)))
      case _ =>
    }
    new ParameterAndOptions(Seq.empty, options.result())
  }

  def apply(args:Map[String,Json]):ParameterAndOptions = {
    val options = Map.newBuilder[String, Seq[String]]
    args.foreach{
      case (key :String, value) => value.asArray match {
        case Some(listValue) => options.addOne((key, listValue.map{ jVal =>
          jVal.asString match {
            //Note: We need to treat Strings specially, otherwise they get quoted
            case Some(str) => str
            case None => jVal.toString()
          }
        }))
        case None => options.addOne((key, value.asString match {
          //Note: We need to treat Strings specially, otherwise they get quoted
          case Some(str) => Seq(str)
          case None => Seq((value.toString()))
        }))
      }
    }
    new ParameterAndOptions(Seq.empty, options.result())
  }
}
