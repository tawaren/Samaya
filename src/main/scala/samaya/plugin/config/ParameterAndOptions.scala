package samaya.plugin.config

import io.circe.{Json, JsonObject}

import java.util.Properties
import scala.collection.mutable

//Tool that aggregates command line arguments
// usable for merging multiple for plugins
class ParameterAndOptions(val parameters:Seq[String], val options:Map[String, Seq[String]]) {
  def join(other:ParameterAndOptions)(f: (Option[String], (Seq[String], Seq[String])) => Seq[String]):ParameterAndOptions = {
    val merged = options.toSeq ++ other.options.toSeq
    val grouped = merged.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
    val cleaned = grouped.map(kv => (kv._1, kv._2.reduce((a,b) => f(Some(kv._1),(a,b)))))
    new ParameterAndOptions(f(None,(parameters,other.parameters)), cleaned)
  }

  def append(other:ParameterAndOptions):ParameterAndOptions = join(other){ case (_,(a,b)) => a ++ b }
  def supersede(other:ParameterAndOptions):ParameterAndOptions = join(other) { case (_,(_,b)) => b }
  def ignore(other:ParameterAndOptions):ParameterAndOptions = other.supersede(this)

  def joinSeparate(other:ParameterAndOptions)(params:(Seq[String], Seq[String]) => Seq[String])(options:(Seq[String], Seq[String]) => Seq[String]):ParameterAndOptions = join(other){
    case (None,(a,b)) => params(a,b)
    case (Some(_),(a,b)) => options(a,b)
  }

  def appendParamSupersedeOption(other:ParameterAndOptions):ParameterAndOptions = joinSeparate(other)(_++_)((_, a) => a)
  def appendParamIgnoreOption(other:ParameterAndOptions):ParameterAndOptions = joinSeparate(other)(_++_)((a, _) => a)
  def appendOptionSupersedeParam(other:ParameterAndOptions):ParameterAndOptions = joinSeparate(other)((_, a) => a)(_++_)
  def appendOptionIgnoreParam(other:ParameterAndOptions):ParameterAndOptions = joinSeparate(other)((a, _) => a)(_++_)

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
        case Some(listValue) => options.addOne((key, listValue.map(_.toString())))
        case None => options.addOne(key, Seq(value.toString()))
      }
    }
    new ParameterAndOptions(Seq.empty, options.result())
  }
}
