package samaya.plugin.impl.compiler.mandala

import org.antlr.v4.runtime.{ParserRuleContext, Token}
import samaya.plugin.impl.compiler.simple.{MandalaBaseVisitor, MandalaParser}
import samaya.plugin.impl.compiler.simple.MandalaParser.PathContext
import samaya.structure.types.{InputSourceId, Location, Region, SourceId}

import scala.collection.JavaConverters._

class MandalaDependencyExtractorVisitor(file:String) extends MandalaBaseVisitor[Map[Seq[String], Seq[SourceId]]]{

  private def locationFromToken(t:Token, isEnd:Boolean):Location = if(isEnd) {
    Location.Combined(file, t.getLine, t.getCharPositionInLine + t.getText.length + 1, t.getStopIndex)
  } else {
    Location.Combined(file, t.getLine, t.getCharPositionInLine +1, t.getStartIndex)
  }

  private def regionFromContext(p:ParserRuleContext):Region = Region(
    start = locationFromToken(p.start, isEnd = false),
    end = locationFromToken(p.stop, isEnd = true),
  )

  def sourceIdFromContext(p:ParserRuleContext):SourceId = new InputSourceId(regionFromContext(p))
  //Todo: Remember Local Stuff & Ommit from Dependency

  private var imports = Map[String,Seq[String]]()
  private var availableBindings = Set.empty[String]

  private def startsUpper(elem:String) = elem.length > 0 && elem.charAt(0).isUpper

  override def visitImport_(ctx: MandalaParser.Import_Context): Map[Seq[String], Seq[SourceId]] = {
    val elems = ctx.path().part.asScala.map(name => getName(name).getText)
    if(ctx.wildcard() != null) {
      Map(elems -> Seq(sourceIdFromContext(ctx)))
    } else {
      val last = elems.last
      imports = imports.updated(last,elems)
      Map(elems -> Seq(sourceIdFromContext(ctx)))
    }
  }

  override def defaultResult(): Map[Seq[String], Seq[SourceId]]  = Map.empty
  override def aggregateResult(aggregate:Map[Seq[String], Seq[SourceId]], nextResult:Map[Seq[String], Seq[SourceId]] ):  Map[Seq[String], Seq[SourceId]]  = {
    var newMap = aggregate
    for((k,v) <- nextResult) {
      newMap = newMap.updated(k, newMap.getOrElse(k,Seq.empty) ++ v)
    }
    newMap
  }

  private var availableGenerics = Set.empty[String]
  private var availableComponents = Set.empty[String]
  private var availableModules = Set.empty[String]
  private var componentGenerics = Set.empty[String]

  override def visitTransaction(ctx: MandalaParser.TransactionContext): Map[Seq[String], Seq[SourceId]] = {
    val res = super.visitTransaction(ctx)
    availableComponents = Set.empty[String]
    availableGenerics = Set.empty[String]
    componentGenerics = Set.empty[String]
    availableModules = availableModules + getName(ctx.name()).getText
    res
  }

  override def visitModule(ctx: MandalaParser.ModuleContext): Map[Seq[String], Seq[SourceId]] = {
    val res = super.visitModule(ctx)
    availableComponents = Set.empty[String]
    availableGenerics = Set.empty[String]
    componentGenerics = Set.empty[String]
    availableModules = availableModules +  getName(ctx.name()).getText
    res
  }

  override def visitClass_(ctx: MandalaParser.Class_Context): Map[Seq[String], Seq[SourceId]] = {
    val res = super.visitClass_(ctx)
    availableComponents = Set.empty[String]
    availableGenerics = Set.empty[String]
    componentGenerics = extractGenericArgs(ctx.genericArgs())
    availableModules = availableModules +  getName(ctx.name()).getText
    res
  }

  override def visitData(ctx: MandalaParser.DataContext): Map[Seq[String], Seq[SourceId]]  = {
    availableGenerics = componentGenerics ++ extractGenericArgs(ctx.genericArgs())
    val res = super.visitData(ctx)
    if(ctx != null && ctx.name != null) {
      availableComponents += getName(ctx.name).getText
    }
    res
  }

  /*
   override def visitImplement(ctx: MandalaParser.ImplementContext): Set[Seq[String]]  = {
    val bodyBindings = ctx.captures.p.asScala.map(n => getName(n.name).getText) ++ ctx.paramBindings.i.asScala.map(b => getName(b.name()).getText)
    withBindings(bodyBindings.toSet) {
      availableGenerics = componentGenerics ++ extractGenericArgs(ctx.genericArgs())
      val res = super.visitImplement(ctx)
      if (ctx != null && ctx.name != null) {
        availableComponents += getName(ctx.name).getText
      }
      res
    }
  }

  override def visitSig(ctx: MandalaParser.SigContext): Set[Seq[String]]  = {
    availableGenerics = componentGenerics ++ extractGenericArgs(ctx.genericArgs())
    val res = super.visitSig(ctx)
    if(ctx != null && ctx.name != null) {
      availableComponents += getName(ctx.name).getText
    }
    res
  }*/

  private def withBindings[T](bindings:Set[String])(body: => T):T = {
    val old = availableBindings
    availableBindings = availableBindings ++ bindings
    val res = body
    availableBindings = old
    res
  }



  override def visitFunction(ctx: MandalaParser.FunctionContext): Map[Seq[String], Seq[SourceId]] = {
    val bodyBindings = ctx.params().p.asScala.map(n => getName(n.name).getText).toSet
    withBindings(bodyBindings) {
      availableGenerics = componentGenerics ++ extractGenericArgs(ctx.genericArgs())
      val res = super.visitFunction(ctx)
      if(ctx != null && ctx.name != null) {
        availableComponents += getName(ctx.name).getText
      }
      res
    }
  }

  private def extractGenericArgs(ctx: MandalaParser.GenericArgsContext): Set[String] = {
    //this spares the caller the check and simplifies the code
    if(ctx == null) return Set.empty
    ctx.generics.asScala.map(n => getName(n.name).getText).toSet
  }

  private def visitRef(parts:Seq[String], sourceId: SourceId): Map[Seq[String], Seq[SourceId]] = {

    val start = parts.head
    //If it is just a name not a path it can refer to local declarations
    if(parts.size == 1) {
      //May be from local scope
      if(availableGenerics.contains(start)){
        return Map.empty
      }

      //May be from this modules scope
      if(availableComponents.contains(start)){
        return Map.empty
      }
    }

    //If not a local declaration it can still be something imported | absolute path
    val dep = if(availableModules.contains(start)) {
      //It refers to something in same file - no need to compile separately
      Seq.empty
    } else if(imports.contains(start)) {
      if(parts.tail.isEmpty) {
        //this is a ref to a fully qualified import
        //last is componentName
        imports(start).init
      } else {
        //this is a composed import, parts in the import parts in the ref
        imports(start) ++ parts.tail.init //last is componentName
      }
    } else {
      parts.init //last is entryName
    }

    if(dep.isEmpty) {
      Map.empty
    } else {
      Map(dep -> Seq(sourceId))
    }
  }

  override def visitBranch(ctx: MandalaParser.BranchContext): Map[Seq[String], Seq[SourceId]] = {
    val extracts = if(ctx == null || ctx.extracts() == null) {
      Set.empty[String]
    } else {
      ctx.extracts().p.asScala.map(n => getName(n.name()).getText).toSet
    }
    withBindings(extracts) {
      super.visitBranch(ctx)
    }
  }

  override def visitSucc(ctx: MandalaParser.SuccContext): Map[Seq[String], Seq[SourceId]] = {
    val extracts = if(ctx.extracts() != null) {
      ctx.extracts().p.asScala.map(n => getName(n.name()).getText).toSet
    } else {
      Set.empty[String]
    }
    withBindings(extracts) {
      super.visitSucc(ctx)
    }
  }

  override def visitFail(ctx: MandalaParser.FailContext): Map[Seq[String], Seq[SourceId]] = {
    val extracts = if(ctx.extracts() != null) {
      ctx.extracts().p.asScala.map(n => getName(n.name()).getText).toSet
    } else {
      Set.empty[String]
    }
    withBindings(extracts) {
      super.visitFail(ctx)
    }
  }

  //We need special exception here as baseRef can refer to a value
  //Sadly: ID is a valid baseRef and as such we can not differentiate in the grammar
  override def visitInvoke(ctx: MandalaParser.InvokeContext): Map[Seq[String], Seq[SourceId]] = {
    val path = ctx.baseRef().path().part.asScala
    if(path.size == 1 && availableBindings.contains(getName(path.head).getText)) {
      visitArgs(ctx.args())
    } else {
      super.visitInvoke(ctx)
    }
  }

  override def visitTryInvoke(ctx: MandalaParser.TryInvokeContext): Map[Seq[String], Seq[SourceId]] = {
    val path = ctx.baseRef().path().part.asScala
    if(path.size == 1 && availableBindings.contains(getName(path.head).getText)) {
      aggregateResult(
        visitTryArgs(ctx.tryArgs()),
        aggregateResult(
          visitFail(ctx.fail()),
          visitSucc(ctx.succ())
        )
      )
    } else {
      super.visitTryInvoke(ctx)
    }
  }

  override def visitLet(ctx: MandalaParser.LetContext): Map[Seq[String], Seq[SourceId]] = {
    val bindImports = visit(ctx.bind)
    val assigImports = visitAssigs(ctx.assigs())
    val extracts = ctx.assigs().`val`.asScala.map(n => getName(n.name()).getText).toSet
    val execImports = withBindings(extracts) {
      visit(ctx.exec)
    }
    aggregateResult(aggregateResult(bindImports, assigImports), execImports)
  }

  override def visitUnpack(ctx: MandalaParser.UnpackContext): Map[Seq[String], Seq[SourceId]] = {
    val bindImports = visit(ctx.bind)
    val extractImports = visitExtracts(ctx.extracts())
    val extracts = ctx.extracts().p.asScala.map(n => getName(n.name()).getText).toSet
    val execImports = withBindings(extracts) {
      visit(ctx.exec)
    }
    aggregateResult(aggregateResult(bindImports, extractImports), execImports)
  }

  override def visitPath(ctx: PathContext): Map[Seq[String], Seq[SourceId]] = {
    visitRef(ctx.part.asScala.map(getName(_).getText), sourceIdFromContext(ctx))
  }

  def getName(ctx: MandalaParser.NameContext): Token = {
    if(ctx.RAW_ID() != null) {
      ctx.RAW_ID().getSymbol
    } else {
      ctx.keywords().id
    }
  }
}
