package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Error, LocatedMessage, PlainMessage, feedback}
import samaya.plugin.impl.compiler.mandala.MandalaParser
import samaya.plugin.impl.compiler.mandala.components.MandalaTransactionCompilerOutput
import samaya.structure.{Attribute, Result}
import samaya.structure.types.{Id, SourceId, Type}
import samaya.toolbox.process.TypeInference

import scala.collection.JavaConverters._

trait TransactionBuilder extends CompilerToolbox{
  self: ExpressionBuilder with ComponentBuilder with SigCompiler =>

  override def visitTransaction(ctx: MandalaParser.TransactionContext): Unit = {
    val name = visitName(ctx.name)
    val sourceId = sourceIdFromContext(ctx)
    val bodyBindings = ctx.params().p.asScala.map(n => visitName(n.name))
    withComponentBuilder(name) {
      val (resResults, body) = if(ctx.rets() != null) {
        val res = withFreshIndex{
          ctx.rets().r.asScala.map(visitRet)
        }
        val defaultIds = res.map(b => Id(b.name, b.src))
        val code = withDefaultReturns(defaultIds){processBody(ctx.funBody, bodyBindings.toSet)}
        (res,code)
      } else {
        val code = processBody(ctx.funBody, bodyBindings.toSet)
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
        feedback(LocatedMessage("Transactions must have a body", sourceId, Error))
        return
      }

      build(new MandalaTransactionCompilerOutput(
        name,
        ctx.TRANSACTIONAL() != null,
        withFreshIndex(ctx.params().p.asScala.map(visitParam)),
        resResults,
        body.get,
        sourceId
      ),sourceId)
    }
  }
}
