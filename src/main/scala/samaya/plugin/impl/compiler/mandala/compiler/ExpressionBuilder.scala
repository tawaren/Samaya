package samaya.plugin.impl.compiler.mandala.compiler

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.MandalaParser
import samaya.plugin.impl.compiler.mandala.MandalaParser.{PatternsContext, TypeRefContext}
import samaya.structure.Attribute
import samaya.structure.types._
import samaya.toolbox.process.TypeInference
import samaya.toolbox.process.TypeInference.TypeVar

import scala.:+
import scala.jdk.CollectionConverters._
import scala.collection.immutable.ListMap

//todo: add code id everywhere
//todo: enforce single return for single return opcodes in syntax
trait ExpressionBuilder extends CompilerToolbox{
  self: ComponentResolver with ComponentBuilder=>

  private var returnIds: Option[Seq[Id]] = None
  var availableBindings = Set.empty[String]
  var inspectModeOn = false

  def withDefaultReturns[T](defaults: Seq[Id])(body: => T): T = {
    val prev = returnIds
    returnIds = Some(defaults)
    val res: T = body
    returnIds = prev
    res
  }

  def withBindings[T](bindings:Set[String])(body: => T):T = {
    val old = availableBindings
    availableBindings = availableBindings ++ bindings
    val res = body
    availableBindings = old
    res
  }

  def withInspectMode[T](mode:Boolean)(body: => T):T = {
    val old = inspectModeOn
    inspectModeOn = mode
    val res = body
    inspectModeOn = old
    res
  }

  private def adaptSource(src: SourceId)(id:Id):Id = Id(id.name, src)

  def processBody(ctx: MandalaParser.FunBodyContext, bindings:Set[String]) : Option[Seq[OpCode]] = {
    if(ctx == null) return None
    Some(processBlock(ctx.tailExp(), bindings))
  }

  def processBlock(ctx: MandalaParser.TailExpContext, bindings:Set[String]): Seq[OpCode] = {
    withBindings(bindings){
      visitExp(ctx)._2
    }
  }

  def visitExp(ctx:ParseTree):(Seq[Ref], Seq[OpCode]) = {
    visit(ctx) match {
      case (ids:Seq[Ref], codes:Seq[OpCode]) => (ids,codes)
      case _ =>  (Seq.empty, Seq.empty)
    }
  }

  private def idAttr(id: Id): AttrId = AttrId(id, Seq.empty)


  private def getExpReturn(ctx:ParserRuleContext):Id = {
    adaptSource(sourceIdFromContext(ctx))(getExpReturns(ctx, 1).head)
  }

  private def getExpReturns(ctx:ParserRuleContext, amount:Int):Seq[Id] = {
    val src = sourceIdFromContext(ctx)
    returnIds match {
      case Some(ids) =>
        val adaptedIds = ids.map(adaptSource(src))
        if(ids.size != amount) {
          feedback(LocatedMessage(s"Expression produced $amount results but ${ids.size} results were expected",src,Error, Compiler()))
          val missing = Math.max(amount - ids.size,0)
          val padding = Seq.fill(missing)(freshIdFromContext(ctx))
          adaptedIds ++ padding
        } else {
          adaptedIds
        }
      case None => Seq.fill(amount)(freshIdFromContext(ctx))
    }
  }

  //todo: do we have to mark the Seq.empty?? as None?? to prevent errors further up??
  private def getExpReturns(ctx:ParserRuleContext, func:Func):Seq[Id] = {
    if(!func.isUnknown) {
      getExpReturns(ctx, func.returnInfo(context).size)
    } else {
      val src = sourceIdFromContext(ctx)
      returnIds.getOrElse(Seq.empty).map(adaptSource(src))
    }
  }

  override def visitTypeHint(ctx: MandalaParser.TypeHintContext): Type = visitTypeRef(ctx.typeRef())

  override def visitHex(ctx: MandalaParser.HexContext): Lit = {
    val text = ctx.HEX().getText
    val arr = Array.newBuilder[Byte]

    for (i <- 0 until ((text.length / 2)-1)) {
      val index = (i+1) * 2
      arr += Integer.parseInt(text.substring(index, index + 2), 16).toByte
    }
    Lit(arr.result(), isNum = false)
  }

  override def visitNumber(ctx: MandalaParser.NumberContext): Lit  = Lit(BigInt(ctx.NUM().getText).toByteArray, isNum = true)

  override def visitLiteral(ctx: MandalaParser.LiteralContext): (Seq[Ref], Seq[OpCode]) = {
    val lit = visit(ctx.lit()).asInstanceOf[Lit]
    val ret = getExpReturn(ctx)
    val src = sourceIdFromContext(ctx)
    //todo: make checker that enforces lit size
    //todo: make transformer (or add to inferenzer) that pads lits
    (Seq(ret), Seq(OpCode.Lit(TypedId(ret, Seq.empty, TypeVar(src)),lit,src)))
  }

  def visitPatternBinding(ctx: ParserRuleContext, typeRef:TypeRefContext, patterns:PatternsContext): (AttrId, Set[String], Seq[OpCode], Type) = {
    val id = freshIdFromContext(ctx)
    val src = sourceIdFromContext(ctx)
    val typ = visitTypeRef(typeRef)
    val (rets, processors) = visitUnpackPattern(patterns, id)
    (AttrId(id,Seq.empty), rets, TypeInference.TypeHint(id,typ,src) +: processors, typ)
  }

  override def visitBinding(ctx: MandalaParser.BindingContext): (AttrId, Set[String], Seq[OpCode]) = {
    val attr = if(ctx.CONTEXT() != null) Seq(Attribute("context", Attribute.Unit)) else Seq.empty
    if(ctx.typeRef() != null){
      val (id, bindings, code, typ) = visitPatternBinding(ctx, ctx.typeRef(), ctx.patterns())
      val hint = TypeInference.TypeHint(id,typ,sourceIdFromContext(ctx));
      return  (id, bindings, hint +: code)
    }

    val id =  if(ctx.name() == null) {
      freshIdFromContext(ctx)
    } else {
      idFromToken(visitToken(ctx.name()))
    }
    val attrId = AttrId(id,attr)
    if(ctx.typeHint() != null){
      val typ = visitTypeRef(ctx.typeHint().typeRef())
      (attrId, Set(id.name), Seq(TypeInference.TypeHint(id,typ,sourceIdFromContext(ctx))))
    } else {
      (attrId, Set(id.name), Seq.empty)
    }
  }

  override def visitPatterns(ctx: PatternsContext): (Seq[AttrId], Set[String], Seq[OpCode]) = {
    if(ctx == null) return (Seq.empty[AttrId], Set.empty[String], Seq.empty[OpCode])
    ctx.p.asScala.map(visitBinding).foldLeft((Seq.empty[AttrId], Set.empty[String], Seq.empty[OpCode])){
      case ((idAgg,bindAgg,codeAgg),(id,binds,code)) => (idAgg :+ id, bindAgg++binds, codeAgg ++ code)
    }
  }

  var isTailExp = false
  private def withIsTailExp[T](value:Boolean)(body: =>T)={
    val oldIsTailExp = isTailExp
    isTailExp = value
    val res = body
    isTailExp = oldIsTailExp
    res
  }

  override def visitArgTailExp(ctx: MandalaParser.ArgTailExpContext): (Seq[Ref], Seq[OpCode])  = {
    withIsTailExp(true){
      super.visitArgTailExp(ctx).asInstanceOf[(Seq[Ref], Seq[OpCode]) ]
    }
  }

  override def visitArgExp(ctx: MandalaParser.ArgExpContext): (Seq[Ref], Seq[OpCode])  = {
    withIsTailExp(false){
      super.visitArgExp(ctx).asInstanceOf[(Seq[Ref], Seq[OpCode]) ]
    }
  }


  private def createOpCall(name:String, args:Seq[Ref], ctx:ParserRuleContext): (Seq[Ref], OpCode)  = {
    // make normal function call
    val sourceId = sourceIdFromContext(ctx)
    val parts = Seq(name)
    val addFun = resolveFunc(parts, None, None, Some(args.length), sourceId)
    val rets = getExpReturns(ctx, addFun)
    (rets, OpCode.Invoke(rets.map(idAttr), addFun, args, sourceId))
  }


  private def visitBinOpExp(name:String, exp1: => (Seq[Ref], Seq[OpCode]), exp2: => (Seq[Ref], Seq[OpCode]), ctx:ParserRuleContext): (Seq[Ref], Seq[OpCode]) = {
    withIsTailExp(false) {
      val op1Id = freshIdFromContext(ctx)
      val op2Id = freshIdFromContext(ctx)
      val (res1,producingCodes1) = withDefaultReturns(Seq(op1Id)){ exp1 }
      val (res2,producingCodes2) = withDefaultReturns(Seq(op2Id)){ exp2 }
      val args = Seq(res1.headOption.getOrElse(op1Id), res2.headOption.getOrElse(op2Id))
      val producers = producingCodes1 ++ producingCodes2
      val (rets, code) =  createOpCall(name, args, ctx)
      (rets, producers :+ code)
    }
  }

  private def visitUnOpExp(name:String, exp1: => (Seq[Ref], Seq[OpCode]), ctx:ParserRuleContext): (Seq[Ref], Seq[OpCode]) = {
    withIsTailExp(false) {
      val op1Id = freshIdFromContext(ctx)
      val (res1,producers) = withDefaultReturns(Seq(op1Id)){ exp1 }
      val args = Seq(res1.headOption.getOrElse(op1Id))
      val (rets, code) =  createOpCall(name, args, ctx)
      (rets, producers :+ code)
    }
  }

  override def visitOrExp(ctx: MandalaParser.OrExpContext): (Seq[Ref], Seq[OpCode]) = {
    visitBinOpExp("or", visitExp(ctx.op1), visitExp(ctx.op2), ctx)
  }

  override def visitXorExp(ctx: MandalaParser.XorExpContext): (Seq[Ref], Seq[OpCode]) = {
    visitBinOpExp("xor", visitExp(ctx.op1), visitExp(ctx.op2), ctx)
  }

  override def visitAndExp(ctx: MandalaParser.AndExpContext): (Seq[Ref], Seq[OpCode]) = {
    visitBinOpExp("and", visitExp(ctx.op1), visitExp(ctx.op2), ctx)
  }

  override def visitCmpExp(ctx: MandalaParser.CmpExpContext): (Seq[Ref], Seq[OpCode]) = {
    if(ctx.LT() != null) {
      if(ctx.EQ() != null) {
        visitBinOpExp("lte", visitExp(ctx.op1), visitExp(ctx.op2), ctx)
      } else {
        visitBinOpExp("lt", visitExp(ctx.op1), visitExp(ctx.op2), ctx)
      }
    } else {
      if(ctx.EQ() != null) {
        visitBinOpExp("gte", visitExp(ctx.op1), visitExp(ctx.op2), ctx)
      } else {
        visitBinOpExp("gt", visitExp(ctx.op1), visitExp(ctx.op2), ctx)
      }
    }
  }

  override def visitEQExp(ctx: MandalaParser.EQExpContext): (Seq[Ref], Seq[OpCode]) = {
    if(ctx.BANG() != null) {
      visitUnOpExp("not", visitBinOpExp("eq", visitExp(ctx.op1), visitExp(ctx.op2), ctx), ctx);
    } else {
      visitBinOpExp("eq", visitExp(ctx.op1), visitExp(ctx.op2), ctx)
    }
  }

  override def visitAddSubExp(ctx: MandalaParser.AddSubExpContext): (Seq[Ref], Seq[OpCode]) = {
    val op = if(ctx.ADD() != null) {
      "add"
    } else {
      "sub"
    }
    visitBinOpExp(op, visitExp(ctx.op1), visitExp(ctx.op2), ctx)
  }

  override def visitMulDivExp(ctx: MandalaParser.MulDivExpContext): (Seq[Ref], Seq[OpCode]) = {
    val op = if(ctx.MUL() != null) {
      "mul"
    } else if(ctx.DIV() != null){
      "div"
    } else {
      "mod"
    }
    visitBinOpExp(op, visitExp(ctx.op1), visitExp(ctx.op2), ctx)
  }

  override def visitUnExp(ctx: MandalaParser.UnExpContext): (Seq[Ref], Seq[OpCode]) = {
    if(ctx.BANG() != null){
      visitUnOpExp("not", visitExp(ctx.op1), ctx)
    } else {
      visitUnOpExp("inv", visitExp(ctx.op1), ctx)
    }
  }

  override def visitLet(ctx: MandalaParser.LetContext): (Seq[Ref], Seq[OpCode]) = {
    val (ids, binds, processors) = withInspectMode(false){
      if(ctx.topPatterns().binding() != null){
        val (id,binds,processors) = visitBinding(ctx.topPatterns().binding())
        (Seq(id),binds,processors)
      } else {
        visitPatterns(ctx.topPatterns().patterns())
      }
    }

    val (_,producingCodes) = withDefaultReturns(ids.map(_.id)){
      visitExp(ctx.bind)
    }

    val sourceId = sourceIdFromContext(ctx)
    val (results,resultCodes) = withBindings(binds){
      visitExp(ctx.exec)
    }

    (results, (OpCode.Let(ids,producingCodes,sourceId) +: processors) ++ resultCodes)
  }

  def visitUnpackPattern(ctx: PatternsContext, inputRef:Ref): (Set[String], Seq[OpCode]) = {
    val (ids, binds, processors) = visitPatterns(ctx)
    val sourceId = sourceIdFromContext(ctx)
    val unpackCode = if(inspectModeOn) {
      OpCode.InspectUnpack(ids,inputRef, sourceId)
    } else {
      OpCode.Unpack(ids,inputRef, FetchMode.Infer, sourceId)
    }
    (binds, unpackCode +: processors)
  }

  override def visitSymbol(ctx: MandalaParser.SymbolContext): (Seq[Ref], Seq[OpCode]) = {
    val id = idFromToken(visitToken(ctx.name()))
    if(!isTailExp) {
      (Seq(id), Seq.empty)
    } else {
      val ret = getExpReturn(ctx)
      (Seq(ret), Seq(OpCode.Fetch(idAttr(ret),id,FetchMode.Infer, sourceIdFromContext(ctx))))
    }
  }

  //todo: later extend to support fancy mutli arg spreading etc
  override def visitReturn(ctx: MandalaParser.ReturnContext): (Seq[Ref], Seq[OpCode])  = {
    val (args, producers) = visitArgs(ctx.args())
    val rets = getExpReturns(ctx, args.size)
    (rets, producers :+ OpCode.Return(rets.map(idAttr), args, sourceIdFromContext(ctx)))
  }


  //todo: later extend to support fancy mutli arg spreading etc
  override def visitArgs(ctx: MandalaParser.ArgsContext): (Seq[Ref], Seq[OpCode]) = {
    if(ctx == null) return (Seq.empty, Seq.empty)
    val argExps = ctx.a.asScala.map(exp => {
      val id = freshIdFromContext(ctx)
      withDefaultReturns(Seq(id)) {
        visitExp(exp)
      }
    })

    argExps.foldLeft(Seq.empty[Ref], Seq.empty[OpCode]) {
      case ((aggrIds, aggrCode), (ids, code)) => (aggrIds :+ ids.headOption.getOrElse(freshIdFromContext(ctx)), aggrCode ++ code)
    }
  }

  override def visitTryArgs(ctx: MandalaParser.TryArgsContext): (Seq[(Boolean, Ref)], Seq[OpCode]) = {
    if(ctx == null) return (Seq.empty, Seq.empty)
    val argExps = ctx.a.asScala.map(dolExp => {
      val id = freshIdFromContext(ctx)
      withDefaultReturns(Seq(id)) {
        val (id, code) = visitExp(dolExp.argExp())
        (dolExp.DOL() != null, id, code)
      }
    })

    argExps.foldLeft(Seq.empty[(Boolean, Ref)], Seq.empty[OpCode]) {
      case ((aggrIds, aggrCode), (essential, ids, code)) => (aggrIds :+ (essential, ids.headOption.getOrElse(freshIdFromContext(ctx))), aggrCode ++ code)
    }
  }

  override def visitRollback(ctx: MandalaParser.RollbackContext):  (Seq[Ref], Seq[OpCode])  = {
    val sourceId = sourceIdFromContext(ctx)
    val retTypes = if(ctx.typeRefArgs() != null) {
      ctx.typeRefArgs().targs.asScala.map(visitTypeRef).toSeq
    } else {
      returnIds match {
        case Some(retIds) => Seq.fill(retIds.size)(TypeVar(sourceId))
        case None =>
          feedback(LocatedMessage("Can not infere number of returns for rollback",sourceId,Error, Compiler()))
          Seq.empty
      }
    }

    val rets = getExpReturns(ctx, retTypes.size)
    (rets, Seq(OpCode.RollBack(rets.map(idAttr), Seq.empty, retTypes, sourceIdFromContext(ctx))))
  }

  override def visitPack(ctx: MandalaParser.PackContext): (Seq[Ref], Seq[OpCode]) = {
    val (args, producers) = visitArgs(ctx.args())
    val srcId = sourceIdFromContext(ctx)
    val typ = visitTypeRef(ctx.typeRef())
    val ret = getExpReturn(ctx)
    val tag = if(ctx.ctrName != null){
      idFromToken(visitToken(ctx.ctrName))
    } else {
      typ.asAdtType match {
        case Some(adtType) =>
          val ctrs = adtType.ctrs(context)
          assert(ctrs.nonEmpty)
          if(ctrs.size != 1) feedback(LocatedMessage("A Pack for a type with multiple constructors must specify the constructor to use",srcId,Error, Compiler()))
          Id(ctrs.head._1,srcId)
        case None =>
          feedback(LocatedMessage("No constructor available for the specified type",srcId,Error, Compiler()))
          return (Seq(ret), producers)
      }
    }
    (Seq(ret), producers :+ OpCode.Pack(TypedId(ret, Seq.empty, typ), args, tag, FetchMode.Infer, srcId))
  }

  override def visitProject(ctx: MandalaParser.ProjectContext): (Seq[Ref], Seq[OpCode]) = {
    val id = freshIdFromContext(ctx)
    val (args, producers) = withDefaultReturns(Seq(id)) {
      visitExp(ctx.argExp())
    }

    if(args.size != 1) {
      feedback(LocatedMessage(s"A Project opcode takes exactly one value", sourceIdFromContext(ctx), Error, Compiler()))
    }

    val ret = getExpReturn(ctx)
    val arg = args.headOption.getOrElse(freshIdFromContext(ctx))
    (Seq(ret), producers :+ OpCode.Project(idAttr(ret),arg, sourceIdFromContext(ctx)))
  }

  override def visitUnproject(ctx: MandalaParser.UnprojectContext): (Seq[Ref], Seq[OpCode]) = {
    val id = freshIdFromContext(ctx)
    val (args, producers) =  withDefaultReturns(Seq(id)) {
      visitExp(ctx.argExp())
    }
    if(args.size != 1) {
      feedback(LocatedMessage(s"A Unproject opcode takes exactly one value", sourceIdFromContext(ctx), Error, Compiler()))
    }

    val ret = getExpReturn(ctx)
    val arg = args.headOption.getOrElse(freshIdFromContext(ctx))
    (Seq(ret), producers :+ OpCode.UnProject(idAttr(ret),arg, sourceIdFromContext(ctx)))
  }

  override def visitTyped(ctx: MandalaParser.TypedContext): (Seq[Ref], Seq[OpCode]) = {
    withIsTailExp(false) {
      val ret = getExpReturn(ctx)
      val typ = visitTypeHint(ctx.typeHint())
      val (args, producers) =  withDefaultReturns(Seq(ret)) {
        visitExp(ctx.exp())
      }

      if(args.size != 1) {
        feedback(LocatedMessage(s"A Type Check opcode takes exactly one value", sourceIdFromContext(ctx), Error, Compiler()))
      }

      val arg = args.headOption.getOrElse(freshIdFromContext(ctx))
      (args, producers :+ TypeInference.TypeHint(arg, typ, sourceIdFromContext(ctx)))
    }
  }

  override def visitGet(ctx: MandalaParser.GetContext): (Seq[Ref], Seq[OpCode]) = {
    withIsTailExp(false) {
      val id = freshIdFromContext(ctx)
      val (args, producers) = withDefaultReturns(Seq(id)) {
        visitExp(ctx.exp())
      }

      if(args.size != 1) {
        feedback(LocatedMessage(s"A Field get opcode takes exactly one value", sourceIdFromContext(ctx), Error, Compiler()))
      }

      val ret = getExpReturn(ctx)
      val arg = args.headOption.getOrElse(freshIdFromContext(ctx))
      val field = idFromToken(visitToken(ctx.name()))

      (Seq(ret), producers :+ OpCode.Field(idAttr(ret),arg,field,FetchMode.Infer, sourceIdFromContext(ctx)))
    }
  }

  private def visitCase(eCtx: PatternsContext, cCtx: MandalaParser.TailExpContext): (Seq[AttrId], Seq[Ref], Seq[OpCode]) = {
    val (extracts, binds, processors) = visitPatterns(eCtx)
    val (results,blockCodes) = withBindings(binds) {
      visitExp(cCtx)
    }
    (extracts, results, processors ++ blockCodes)
  }

  override def visitBranch(ctx: MandalaParser.BranchContext): (Id,(Seq[AttrId], Seq[Ref], Seq[OpCode])) = {
    (idFromToken(visitToken(ctx.name())),visitCase(ctx.patterns(), ctx.tailExp()))
  }

  override def visitBranches(ctx: MandalaParser.BranchesContext): ListMap[Id,(Seq[AttrId],Seq[Ref],Seq[OpCode])] = {
    val branches = ListMap.from(ctx.b.asScala.map(visitBranch).toSeq)
    if(branches.size != ctx.branch().size()) {
      feedback(LocatedMessage(s"Merge branches must have different names", sourceIdFromContext(ctx), Error, Compiler()))
    }
    branches
  }

  override def visitEnsure(ctx: MandalaParser.EnsureContext): (Seq[Id], Seq[OpCode]) = {
    val id = freshIdFromContext(ctx)
    val (args, producers) =  withDefaultReturns(Seq(id)) {
      visitExp(ctx.argExp())
    }

    if(args.size != 1) {
      feedback(LocatedMessage(s"A If then else takes exactly one argument value", sourceIdFromContext(ctx), Error, Compiler()))
    }

    val condSrc = sourceIdFromContext(ctx.argExp())
    val inSrc = sourceIdFromContext(ctx.exec)
    val (inRets, inCase) = visitExp(ctx.exec)
    val retTypes = Seq.fill(inRets.size)(TypeVar(condSrc))
    val rollback = OpCode.RollBack(inRets.map(r => AttrId(r.id, Seq.empty)), Seq.empty, retTypes, condSrc)
    val arg = args.headOption.getOrElse(freshIdFromContext(ctx))
    val branches =  ListMap(Id("True",inSrc) -> (Seq.empty, inCase), Id("False", condSrc) -> (Seq.empty, Seq(rollback)))
    val retIds = getExpReturns(ctx,inRets.size)
    (retIds, producers :+ OpCode.Switch(retIds.map(idAttr),arg,branches,FetchMode.Infer,sourceIdFromContext(ctx)))

  }

  override def visitIfThenElse(ctx: MandalaParser.IfThenElseContext): (Seq[Id], Seq[OpCode]) = {
    val id = freshIdFromContext(ctx)
    val (args, producers) =  withDefaultReturns(Seq(id)) {
      visitExp(ctx.argExp())
    }

    if(args.size != 1) {
      feedback(LocatedMessage(s"A If then else takes exactly one argument value", sourceIdFromContext(ctx), Error, Compiler()))
    }

    val thenSrc = sourceIdFromContext(ctx.casT)
    val elseScr = sourceIdFromContext(ctx.casF)
    val (thenRets, thenCase) = visitExp(ctx.casT)
    val (elseRets, elseCase) = visitExp(ctx.casF)
    val arg = args.headOption.getOrElse(freshIdFromContext(ctx))
    val branches =  ListMap(Id("True",thenSrc) -> (Seq.empty, thenCase), Id("False", elseScr) -> (Seq.empty, elseCase))
    val returns = thenRets.size.max(elseRets.size)
    val retIds = getExpReturns(ctx,returns)
    (retIds, producers :+ OpCode.Switch(retIds.map(idAttr),arg,branches,FetchMode.Infer,sourceIdFromContext(ctx)))

  }

  override def visitSwitch(ctx: MandalaParser.SwitchContext): (Seq[Id], Seq[OpCode]) = {
    val id = freshIdFromContext(ctx)
    val (args, producers) =  withDefaultReturns(Seq(id)) {
      visitExp(ctx.argExp())
    }

    if(args.size != 1) {
      feedback(LocatedMessage(s"A Switch opcode takes exactly one value", sourceIdFromContext(ctx), Error, Compiler()))
    }

    val arg = args.headOption.getOrElse(freshIdFromContext(ctx))
    val branchInfo = withInspectMode(false){
      visitBranches(ctx.branches())
    }
    val branches = branchInfo.map(b => (b._1, (b._2._1, b._2._3)))
    val returns = branchInfo.map(_._2._2.size).max
    val retIds = getExpReturns(ctx,returns)
    (retIds, producers :+ OpCode.Switch(retIds.map(idAttr),arg,branches,FetchMode.Infer,sourceIdFromContext(ctx)))
  }

  override def visitInspect(ctx: MandalaParser.InspectContext): (Seq[Id], Seq[OpCode]) = {
    val id = freshIdFromContext(ctx)
    val (args, producers) =  withDefaultReturns(Seq(id)) {
      visitExp(ctx.argExp())
    }

    if(args.size != 1) {
      feedback(LocatedMessage(s"A Inspect opcode takes exactly one value", sourceIdFromContext(ctx), Error, Compiler()))
    }

    val arg = args.headOption.getOrElse(freshIdFromContext(ctx))
    val branchInfo = withInspectMode(true){
      visitBranches(ctx.branches())
    }
    val branches = branchInfo.map(b => (b._1, (b._2._1, b._2._3)))
    val returns = branchInfo.map(_._2._2.size).max
    val retIds = getExpReturns(ctx,returns)
    (retIds, producers :+ OpCode.InspectSwitch(retIds.map(idAttr),arg,branches,sourceIdFromContext(ctx)))
  }

  //We need special exception here as baseRef can refer to a value
  //Sadly: name is a valid baseRef and as such we can not differentiate in the grammar
  override def visitInvoke(ctx: MandalaParser.InvokeContext): (Seq[Ref], Seq[OpCode]) = {
    val sourceId = sourceIdFromContext(ctx)
    val (args, producers) = visitArgs(ctx.args())
    //Check if this calls a function pointer
    val parts = ctx.baseRef().path().part
    if(parts.size() == 1) {
      //todo: we need to add bindings in the above ^^ stuff (in Unpack & Let)
      if(availableBindings.contains(visitName(parts.get(0)))) {
        if(ctx.baseRef().targs != null) feedback(LocatedMessage(s"Function pointer calls do not take generic arguments", sourceIdFromContext(ctx.baseRef()), Error, Compiler()))
        //it is a value / function pointer, and then we use the invoke sig
        returnIds match {
          case Some(retIds) =>
            val adaptedIds = retIds.map(adaptSource(sourceId))
            return (adaptedIds, producers :+ OpCode.InvokeSig(adaptedIds.map(idAttr), idFromToken(visitToken(parts.get(0))),args, sourceId))
          case None =>
            feedback(LocatedMessage("Calling function pointer without syntactically known number of returns is not supported",sourceId,Error, Compiler()))
            return (Seq.empty, producers :+ OpCode.InvokeSig(Seq.empty, idFromToken(visitToken(parts.get(0))),args, sourceId))
        }
      }
    }

    // make normal function call
    val fun = visitFunRef(ctx.baseRef(), Some(args.length))
    val rets = getExpReturns(ctx, fun)
    (rets, producers :+ OpCode.Invoke(rets.map(idAttr), fun, args, sourceIdFromContext(ctx)))
  }

  //We need special exception here as baseRef can refer to a value
  //Sadly: name is a valid baseRef and as such we can not differentiate in the grammar
  override def visitTryInvoke(ctx: MandalaParser.TryInvokeContext): (Seq[Ref], Seq[OpCode])  = {
    val (args, producers) = visitTryArgs(ctx.tryArgs())
    //Check if this calls a function pointer
    val parts = ctx.baseRef().path().part

    val succInfo = visitCase(ctx.succ().patterns(), ctx.succ().tailExp())
    val failInfo = visitCase(ctx.fail().patterns(), ctx.fail().tailExp())
    val succ = (succInfo._1, succInfo._3)
    val fail = (failInfo._1, failInfo._3)

    if(parts.size() == 1) {
      if(availableBindings.contains(visitName(parts.get(0)))) {
        if(ctx.baseRef().targs != null) feedback(LocatedMessage(s"Function pointer calls do not take generic arguments", sourceIdFromContext(ctx.baseRef()), Error, Compiler()))
        //it is a value / function pointer, and then we use the invoke sig
        val numRets = succInfo._2.size.max(failInfo._2.size)
        val rets = getExpReturns(ctx, numRets)
        return (rets, producers :+ OpCode.TryInvokeSig(rets.map(idAttr), idFromToken(visitToken(parts.get(0))),args, succ, fail, sourceIdFromContext(ctx)))
      }
    }

    // make normal function call
    val fun = visitFunRef(ctx.baseRef(), Some(args.length))
    val rets = getExpReturns(ctx, succInfo._2.size.max(failInfo._2.size))
    (rets, producers :+ OpCode.TryInvoke(rets.map(idAttr), fun, args, succ, fail, sourceIdFromContext(ctx)))
  }
}
