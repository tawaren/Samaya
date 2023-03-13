package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Compiler, Error, LocatedMessage, PlainMessage, feedback}
import samaya.plugin.impl.compiler.mandala.MandalaParser
import samaya.plugin.impl.compiler.mandala.components.MandalaTransactionCompilerOutput
import samaya.structure.{Attribute, Result}
import samaya.structure.types.{Id, SourceId, Type}
import samaya.toolbox.process.TypeInference

import scala.jdk.CollectionConverters._

trait TransactionBuilder extends CompilerToolbox{
  self: ExpressionBuilder with ComponentBuilder with SigCompiler =>

  override def visitTransaction(ctx: MandalaParser.TransactionContext): Unit = {
    val name = visitName(ctx.name)
    val sourceId = sourceIdFromContext(ctx)
    withComponentBuilder(name) {
      val (funParams, bodyBindings, processors) = visitParams(ctx.params())
      val (resResults, body) = if(ctx.rets() != null) {
        val res = withFreshIndex{
          ctx.rets().r.asScala.map(visitRet).toSeq
        }
        val defaultIds = res.map(b => Id(b.name, b.src)).toSeq
        val code = withDefaultReturns(defaultIds){
          processBody(ctx.funBody, bodyBindings)
        }
        (res,code)
      } else {
        val code = processBody(ctx.funBody, bodyBindings)
        val last = code.get.filter(!_.isVirtual).last.rets
        val res = withFreshIndex{
          last.map(aid => new Result {
            override val name: String = aid.id.name
            override val index: Int = nextIndex()
            override val typ: Type = TypeInference.TypeVar(sourceId)
            override val attributes: Seq[Attribute] = Seq.empty
            override val src: SourceId = sourceId
          })
        }
        (res, code)
      }

      if(body.isEmpty){
        feedback(LocatedMessage("Transactions must have a body", sourceId, Error, Compiler()))
        return
      }

      build(new MandalaTransactionCompilerOutput(
        name,
        ctx.TRANSACTIONAL() != null,
        funParams,
        resResults,
        processors ++ body.get,
        sourceId
      ),sourceId)
    }
  }
}
