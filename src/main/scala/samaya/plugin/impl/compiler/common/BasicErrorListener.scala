package samaya.plugin.impl.compiler.common

import org.antlr.v4.runtime.{BaseErrorListener, RecognitionException, Recognizer}
import samaya.compilation.ErrorManager.{Error, PlainMessage, SourceParsing, feedback}

class BasicErrorListener(file:String) extends BaseErrorListener{
  override def syntaxError(recognizer: Recognizer[_, _], offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException): Unit = {
    feedback(PlainMessage(s"file $file line $line:$charPositionInLine $msg", Error, SourceParsing()))
  }
}
