package samaya.plugin.impl.compiler.common

import org.antlr.v4.runtime.{BaseErrorListener, RecognitionException, Recognizer}
import samaya.compilation.ErrorManager.{Error, PlainMessage, SourceParsing, feedback}
import samaya.types.ContentAddressable

class BasicErrorListener(context:ContentAddressable) extends BaseErrorListener{
  override def syntaxError(recognizer: Recognizer[_, _], offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException): Unit = {
    feedback(PlainMessage(s"${context.identifier.fullName}@$line:$charPositionInLine $msg \n in ${context.location}", Error, SourceParsing()))
  }
}
