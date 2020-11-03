package samaya.plugin.impl.compiler.mandala.compiler

import org.antlr.v4.runtime.{ParserRuleContext, Token}
import samaya.compilation.ErrorManager.{Error, LocatedMessage, feedback}
import samaya.plugin.impl.compiler.mandala.{Environment, MandalaBaseVisitor, MandalaParser}
import samaya.structure.{Component, Interface}
import samaya.structure.types._

trait CompilerToolbox extends MandalaBaseVisitor[Any] {
  def env:Environment

  //used to ensure we do not produce multiples of same name & classifiers
  private var entries:Set[(String, Set[String])] = Set.empty

  def build(comp:Component,src:SourceId): Option[Interface[Component]] = {
    val key = (comp.name, comp.classifier)
    if(entries.contains(key)) {
      feedback(LocatedMessage(s"Component with name ${comp.name} and classifier ${comp.classifier} already exists",src,Error))
    } else {
      entries = entries + key
    }
    env.builder(comp)
  }

  private def locationFromToken(t:Token, isEnd:Boolean):Location = if(isEnd) {
    Location.Combined(env.file, t.getLine, t.getCharPositionInLine + t.getText.length + 1, t.getStopIndex)
  } else {
    Location.Combined(env.file, t.getLine, t.getCharPositionInLine +1, t.getStartIndex)
  }

  private def regionFromContext(p:ParserRuleContext):Region = Region(
    start = locationFromToken(p.start, isEnd = false),
    end = locationFromToken(p.stop, isEnd = true),
  )

  def sourceIdFromContext(p:ParserRuleContext):SourceId = new InputSourceId(regionFromContext(p))


  private var idCounter:Int = -1
  private var nameCounter = -1
  def withFreshCounters[T](body: => T):T ={
    val oldIdCounter = idCounter
    val oldNameCounter = nameCounter
    idCounter = -1
    val res = body
    idCounter = oldIdCounter
    nameCounter = oldNameCounter
    res
  }

  def freshIdFromContext(p:ParserRuleContext):Id = {
    idCounter+=1
    Id(idCounter,sourceIdFromContext(p))
  }

  private def freshName():String = {
    nameCounter+=1
    "#"+nameCounter
  }

  private var index = -1
  def nextIndex():Int = {
    index += 1
    index
  }

  def withFreshIndex[T](body: => T):T ={
    val oldIndex = index
    index = -1
    val res = body
    index = oldIndex
    res
  }


  def idFromToken(t:Token):Id = Id(t.getText,new InputSourceId(regionFromToken(t)))

  private def regionFromToken(t:Token):Region = Region(
    start = locationFromToken(t, isEnd = false),
    end = locationFromToken(t, isEnd = true),
  )

  override def visitName(ctx: MandalaParser.NameContext): String = {
    if (ctx == null) {
      freshName()
    } else {
      visitToken(ctx).getText
    }
  }

  def visitToken(ctx: MandalaParser.NameContext): Token = {
    if (ctx.RAW_ID() != null) {
      ctx.RAW_ID().getSymbol
    } else {
      ctx.keywords().id
    }
  }
}
