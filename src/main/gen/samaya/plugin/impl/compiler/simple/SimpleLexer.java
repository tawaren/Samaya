// Generated from C:/Users/Markus Knecht/Dropbox/Privat/UZH/PhD/code/Samaya/src/main/antlr/samaya/plugin/impl/compiler/simple\Simple.g4 by ANTLR 4.7.2
package samaya.plugin.impl.compiler.simple;

    package samaya.plugin.impl.compiler.simple;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SimpleLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, BLOCK_COMMENT=26, LINE_COMMENT=27, COMMA=28, DOT=29, AMP=30, 
		TEXT=31, NUM=32, MODULE=33, DATA=34, ERROR=35, FUNCTION=36, EXTERNAL=37, 
		LIT=38, RETURN=39, THROW=40, PHANTOM=41, DROP=42, COPY=43, PERSIST=44, 
		INSPECT=45, CONSUME=46, CREATE=47, EMBED=48, ID=49, WS=50;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
			"T__17", "T__18", "T__19", "T__20", "T__21", "T__22", "T__23", "T__24", 
			"BLOCK_COMMENT", "LINE_COMMENT", "COMMA", "DOT", "AMP", "TEXT", "NUM", 
			"MODULE", "DATA", "ERROR", "FUNCTION", "EXTERNAL", "LIT", "RETURN", "THROW", 
			"PHANTOM", "DROP", "COPY", "PERSIST", "INSPECT", "CONSUME", "CREATE", 
			"EMBED", "ID", "WS", "DIGIT", "ALPHA", "SIGN", "INT"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'import'", "'_'", "'{'", "'}'", "':'", "'('", "')'", "'public'", 
			"'private'", "'protected'", "'risky'", "'borrow'", "'#'", "'@'", "'image'", 
			"'flatten'", "'try'", "'match'", "'='", "'''", "'catch'", "'case'", "'=>'", 
			"'['", "']'", null, null, "','", "'.'", "'&'", null, null, "'module'", 
			"'data'", "'error'", "'function'", "'external'", "'lit'", "'return'", 
			"'throw'", "'phantom'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, "BLOCK_COMMENT", "LINE_COMMENT", "COMMA", "DOT", "AMP", "TEXT", 
			"NUM", "MODULE", "DATA", "ERROR", "FUNCTION", "EXTERNAL", "LIT", "RETURN", 
			"THROW", "PHANTOM", "DROP", "COPY", "PERSIST", "INSPECT", "CONSUME", 
			"CREATE", "EMBED", "ID", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public SimpleLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Simple.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\64\u01c7\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3"+
		"\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t"+
		"\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\16"+
		"\3\16\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\24"+
		"\3\24\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\27"+
		"\3\30\3\30\3\30\3\31\3\31\3\32\3\32\3\33\3\33\3\33\3\33\7\33\u00df\n\33"+
		"\f\33\16\33\u00e2\13\33\3\33\3\33\3\33\5\33\u00e7\n\33\3\33\3\33\3\34"+
		"\3\34\3\34\3\34\7\34\u00ef\n\34\f\34\16\34\u00f2\13\34\3\34\3\34\3\35"+
		"\3\35\3\36\3\36\3\37\3\37\3 \3 \7 \u00fe\n \f \16 \u0101\13 \3 \3 \3 "+
		"\7 \u0106\n \f \16 \u0109\13 \3 \5 \u010c\n \3!\5!\u010f\n!\3!\3!\3\""+
		"\3\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3$\3%\3%\3%\3%"+
		"\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3&\3&\3&\3&\3\'\3\'\3\'\3\'\3(\3(\3(\3"+
		"(\3(\3(\3(\3)\3)\3)\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3*\3+\3+\3+\3+\3+\3"+
		"+\3+\3+\5+\u0158\n+\3,\3,\3,\3,\3,\3,\3,\3,\5,\u0162\n,\3-\3-\3-\3-\3"+
		"-\3-\3-\3-\3-\3-\3-\3-\3-\3-\5-\u0172\n-\3.\3.\3.\3.\3.\3.\3.\3.\3.\3"+
		".\3.\3.\3.\3.\5.\u0182\n.\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\5"+
		"/\u0192\n/\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60"+
		"\5\60\u01a0\n\60\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\5\61"+
		"\u01ac\n\61\3\62\3\62\3\62\7\62\u01b1\n\62\f\62\16\62\u01b4\13\62\3\63"+
		"\6\63\u01b7\n\63\r\63\16\63\u01b8\3\63\3\63\3\64\3\64\3\65\3\65\3\66\3"+
		"\66\3\67\6\67\u01c4\n\67\r\67\16\67\u01c5\5\u00e0\u00ff\u0107\28\3\3\5"+
		"\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21"+
		"!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!"+
		"A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\2i\2k\2m\2\3\2\7\4"+
		"\2\f\f\17\17\5\2\13\f\17\17\"\"\3\2\62;\5\2C\\aac|\4\2--//\2\u01d4\2\3"+
		"\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2"+
		"\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31"+
		"\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2"+
		"\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2"+
		"\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2"+
		"\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2"+
		"I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3"+
		"\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2"+
		"\2\2c\3\2\2\2\2e\3\2\2\2\3o\3\2\2\2\5v\3\2\2\2\7x\3\2\2\2\tz\3\2\2\2\13"+
		"|\3\2\2\2\r~\3\2\2\2\17\u0080\3\2\2\2\21\u0082\3\2\2\2\23\u0089\3\2\2"+
		"\2\25\u0091\3\2\2\2\27\u009b\3\2\2\2\31\u00a1\3\2\2\2\33\u00a8\3\2\2\2"+
		"\35\u00aa\3\2\2\2\37\u00ac\3\2\2\2!\u00b2\3\2\2\2#\u00ba\3\2\2\2%\u00be"+
		"\3\2\2\2\'\u00c4\3\2\2\2)\u00c6\3\2\2\2+\u00c8\3\2\2\2-\u00ce\3\2\2\2"+
		"/\u00d3\3\2\2\2\61\u00d6\3\2\2\2\63\u00d8\3\2\2\2\65\u00da\3\2\2\2\67"+
		"\u00ea\3\2\2\29\u00f5\3\2\2\2;\u00f7\3\2\2\2=\u00f9\3\2\2\2?\u010b\3\2"+
		"\2\2A\u010e\3\2\2\2C\u0112\3\2\2\2E\u0119\3\2\2\2G\u011e\3\2\2\2I\u0124"+
		"\3\2\2\2K\u012d\3\2\2\2M\u0136\3\2\2\2O\u013a\3\2\2\2Q\u0141\3\2\2\2S"+
		"\u0147\3\2\2\2U\u0157\3\2\2\2W\u0161\3\2\2\2Y\u0171\3\2\2\2[\u0181\3\2"+
		"\2\2]\u0191\3\2\2\2_\u019f\3\2\2\2a\u01ab\3\2\2\2c\u01ad\3\2\2\2e\u01b6"+
		"\3\2\2\2g\u01bc\3\2\2\2i\u01be\3\2\2\2k\u01c0\3\2\2\2m\u01c3\3\2\2\2o"+
		"p\7k\2\2pq\7o\2\2qr\7r\2\2rs\7q\2\2st\7t\2\2tu\7v\2\2u\4\3\2\2\2vw\7a"+
		"\2\2w\6\3\2\2\2xy\7}\2\2y\b\3\2\2\2z{\7\177\2\2{\n\3\2\2\2|}\7<\2\2}\f"+
		"\3\2\2\2~\177\7*\2\2\177\16\3\2\2\2\u0080\u0081\7+\2\2\u0081\20\3\2\2"+
		"\2\u0082\u0083\7r\2\2\u0083\u0084\7w\2\2\u0084\u0085\7d\2\2\u0085\u0086"+
		"\7n\2\2\u0086\u0087\7k\2\2\u0087\u0088\7e\2\2\u0088\22\3\2\2\2\u0089\u008a"+
		"\7r\2\2\u008a\u008b\7t\2\2\u008b\u008c\7k\2\2\u008c\u008d\7x\2\2\u008d"+
		"\u008e\7c\2\2\u008e\u008f\7v\2\2\u008f\u0090\7g\2\2\u0090\24\3\2\2\2\u0091"+
		"\u0092\7r\2\2\u0092\u0093\7t\2\2\u0093\u0094\7q\2\2\u0094\u0095\7v\2\2"+
		"\u0095\u0096\7g\2\2\u0096\u0097\7e\2\2\u0097\u0098\7v\2\2\u0098\u0099"+
		"\7g\2\2\u0099\u009a\7f\2\2\u009a\26\3\2\2\2\u009b\u009c\7t\2\2\u009c\u009d"+
		"\7k\2\2\u009d\u009e\7u\2\2\u009e\u009f\7m\2\2\u009f\u00a0\7{\2\2\u00a0"+
		"\30\3\2\2\2\u00a1\u00a2\7d\2\2\u00a2\u00a3\7q\2\2\u00a3\u00a4\7t\2\2\u00a4"+
		"\u00a5\7t\2\2\u00a5\u00a6\7q\2\2\u00a6\u00a7\7y\2\2\u00a7\32\3\2\2\2\u00a8"+
		"\u00a9\7%\2\2\u00a9\34\3\2\2\2\u00aa\u00ab\7B\2\2\u00ab\36\3\2\2\2\u00ac"+
		"\u00ad\7k\2\2\u00ad\u00ae\7o\2\2\u00ae\u00af\7c\2\2\u00af\u00b0\7i\2\2"+
		"\u00b0\u00b1\7g\2\2\u00b1 \3\2\2\2\u00b2\u00b3\7h\2\2\u00b3\u00b4\7n\2"+
		"\2\u00b4\u00b5\7c\2\2\u00b5\u00b6\7v\2\2\u00b6\u00b7\7v\2\2\u00b7\u00b8"+
		"\7g\2\2\u00b8\u00b9\7p\2\2\u00b9\"\3\2\2\2\u00ba\u00bb\7v\2\2\u00bb\u00bc"+
		"\7t\2\2\u00bc\u00bd\7{\2\2\u00bd$\3\2\2\2\u00be\u00bf\7o\2\2\u00bf\u00c0"+
		"\7c\2\2\u00c0\u00c1\7v\2\2\u00c1\u00c2\7e\2\2\u00c2\u00c3\7j\2\2\u00c3"+
		"&\3\2\2\2\u00c4\u00c5\7?\2\2\u00c5(\3\2\2\2\u00c6\u00c7\7)\2\2\u00c7*"+
		"\3\2\2\2\u00c8\u00c9\7e\2\2\u00c9\u00ca\7c\2\2\u00ca\u00cb\7v\2\2\u00cb"+
		"\u00cc\7e\2\2\u00cc\u00cd\7j\2\2\u00cd,\3\2\2\2\u00ce\u00cf\7e\2\2\u00cf"+
		"\u00d0\7c\2\2\u00d0\u00d1\7u\2\2\u00d1\u00d2\7g\2\2\u00d2.\3\2\2\2\u00d3"+
		"\u00d4\7?\2\2\u00d4\u00d5\7@\2\2\u00d5\60\3\2\2\2\u00d6\u00d7\7]\2\2\u00d7"+
		"\62\3\2\2\2\u00d8\u00d9\7_\2\2\u00d9\64\3\2\2\2\u00da\u00db\7\61\2\2\u00db"+
		"\u00dc\7,\2\2\u00dc\u00e0\3\2\2\2\u00dd\u00df\13\2\2\2\u00de\u00dd\3\2"+
		"\2\2\u00df\u00e2\3\2\2\2\u00e0\u00e1\3\2\2\2\u00e0\u00de\3\2\2\2\u00e1"+
		"\u00e6\3\2\2\2\u00e2\u00e0\3\2\2\2\u00e3\u00e4\7,\2\2\u00e4\u00e7\7\61"+
		"\2\2\u00e5\u00e7\7\2\2\3\u00e6\u00e3\3\2\2\2\u00e6\u00e5\3\2\2\2\u00e7"+
		"\u00e8\3\2\2\2\u00e8\u00e9\b\33\2\2\u00e9\66\3\2\2\2\u00ea\u00eb\7\61"+
		"\2\2\u00eb\u00ec\7\61\2\2\u00ec\u00f0\3\2\2\2\u00ed\u00ef\n\2\2\2\u00ee"+
		"\u00ed\3\2\2\2\u00ef\u00f2\3\2\2\2\u00f0\u00ee\3\2\2\2\u00f0\u00f1\3\2"+
		"\2\2\u00f1\u00f3\3\2\2\2\u00f2\u00f0\3\2\2\2\u00f3\u00f4\b\34\2\2\u00f4"+
		"8\3\2\2\2\u00f5\u00f6\7.\2\2\u00f6:\3\2\2\2\u00f7\u00f8\7\60\2\2\u00f8"+
		"<\3\2\2\2\u00f9\u00fa\7(\2\2\u00fa>\3\2\2\2\u00fb\u00ff\7)\2\2\u00fc\u00fe"+
		"\13\2\2\2\u00fd\u00fc\3\2\2\2\u00fe\u0101\3\2\2\2\u00ff\u0100\3\2\2\2"+
		"\u00ff\u00fd\3\2\2\2\u0100\u0102\3\2\2\2\u0101\u00ff\3\2\2\2\u0102\u010c"+
		"\7)\2\2\u0103\u0107\7$\2\2\u0104\u0106\13\2\2\2\u0105\u0104\3\2\2\2\u0106"+
		"\u0109\3\2\2\2\u0107\u0108\3\2\2\2\u0107\u0105\3\2\2\2\u0108\u010a\3\2"+
		"\2\2\u0109\u0107\3\2\2\2\u010a\u010c\7$\2\2\u010b\u00fb\3\2\2\2\u010b"+
		"\u0103\3\2\2\2\u010c@\3\2\2\2\u010d\u010f\5k\66\2\u010e\u010d\3\2\2\2"+
		"\u010e\u010f\3\2\2\2\u010f\u0110\3\2\2\2\u0110\u0111\5m\67\2\u0111B\3"+
		"\2\2\2\u0112\u0113\7o\2\2\u0113\u0114\7q\2\2\u0114\u0115\7f\2\2\u0115"+
		"\u0116\7w\2\2\u0116\u0117\7n\2\2\u0117\u0118\7g\2\2\u0118D\3\2\2\2\u0119"+
		"\u011a\7f\2\2\u011a\u011b\7c\2\2\u011b\u011c\7v\2\2\u011c\u011d\7c\2\2"+
		"\u011dF\3\2\2\2\u011e\u011f\7g\2\2\u011f\u0120\7t\2\2\u0120\u0121\7t\2"+
		"\2\u0121\u0122\7q\2\2\u0122\u0123\7t\2\2\u0123H\3\2\2\2\u0124\u0125\7"+
		"h\2\2\u0125\u0126\7w\2\2\u0126\u0127\7p\2\2\u0127\u0128\7e\2\2\u0128\u0129"+
		"\7v\2\2\u0129\u012a\7k\2\2\u012a\u012b\7q\2\2\u012b\u012c\7p\2\2\u012c"+
		"J\3\2\2\2\u012d\u012e\7g\2\2\u012e\u012f\7z\2\2\u012f\u0130\7v\2\2\u0130"+
		"\u0131\7g\2\2\u0131\u0132\7t\2\2\u0132\u0133\7p\2\2\u0133\u0134\7c\2\2"+
		"\u0134\u0135\7n\2\2\u0135L\3\2\2\2\u0136\u0137\7n\2\2\u0137\u0138\7k\2"+
		"\2\u0138\u0139\7v\2\2\u0139N\3\2\2\2\u013a\u013b\7t\2\2\u013b\u013c\7"+
		"g\2\2\u013c\u013d\7v\2\2\u013d\u013e\7w\2\2\u013e\u013f\7t\2\2\u013f\u0140"+
		"\7p\2\2\u0140P\3\2\2\2\u0141\u0142\7v\2\2\u0142\u0143\7j\2\2\u0143\u0144"+
		"\7t\2\2\u0144\u0145\7q\2\2\u0145\u0146\7y\2\2\u0146R\3\2\2\2\u0147\u0148"+
		"\7r\2\2\u0148\u0149\7j\2\2\u0149\u014a\7c\2\2\u014a\u014b\7p\2\2\u014b"+
		"\u014c\7v\2\2\u014c\u014d\7q\2\2\u014d\u014e\7o\2\2\u014eT\3\2\2\2\u014f"+
		"\u0150\7f\2\2\u0150\u0151\7t\2\2\u0151\u0152\7q\2\2\u0152\u0158\7r\2\2"+
		"\u0153\u0154\7F\2\2\u0154\u0155\7t\2\2\u0155\u0156\7q\2\2\u0156\u0158"+
		"\7r\2\2\u0157\u014f\3\2\2\2\u0157\u0153\3\2\2\2\u0158V\3\2\2\2\u0159\u015a"+
		"\7e\2\2\u015a\u015b\7q\2\2\u015b\u015c\7r\2\2\u015c\u0162\7{\2\2\u015d"+
		"\u015e\7E\2\2\u015e\u015f\7q\2\2\u015f\u0160\7r\2\2\u0160\u0162\7{\2\2"+
		"\u0161\u0159\3\2\2\2\u0161\u015d\3\2\2\2\u0162X\3\2\2\2\u0163\u0164\7"+
		"r\2\2\u0164\u0165\7g\2\2\u0165\u0166\7t\2\2\u0166\u0167\7u\2\2\u0167\u0168"+
		"\7k\2\2\u0168\u0169\7u\2\2\u0169\u0172\7v\2\2\u016a\u016b\7R\2\2\u016b"+
		"\u016c\7g\2\2\u016c\u016d\7t\2\2\u016d\u016e\7u\2\2\u016e\u016f\7k\2\2"+
		"\u016f\u0170\7u\2\2\u0170\u0172\7v\2\2\u0171\u0163\3\2\2\2\u0171\u016a"+
		"\3\2\2\2\u0172Z\3\2\2\2\u0173\u0174\7k\2\2\u0174\u0175\7p\2\2\u0175\u0176"+
		"\7u\2\2\u0176\u0177\7r\2\2\u0177\u0178\7g\2\2\u0178\u0179\7e\2\2\u0179"+
		"\u0182\7v\2\2\u017a\u017b\7K\2\2\u017b\u017c\7p\2\2\u017c\u017d\7u\2\2"+
		"\u017d\u017e\7r\2\2\u017e\u017f\7g\2\2\u017f\u0180\7e\2\2\u0180\u0182"+
		"\7v\2\2\u0181\u0173\3\2\2\2\u0181\u017a\3\2\2\2\u0182\\\3\2\2\2\u0183"+
		"\u0184\7e\2\2\u0184\u0185\7q\2\2\u0185\u0186\7p\2\2\u0186\u0187\7u\2\2"+
		"\u0187\u0188\7w\2\2\u0188\u0189\7o\2\2\u0189\u0192\7g\2\2\u018a\u018b"+
		"\7E\2\2\u018b\u018c\7q\2\2\u018c\u018d\7p\2\2\u018d\u018e\7u\2\2\u018e"+
		"\u018f\7w\2\2\u018f\u0190\7o\2\2\u0190\u0192\7g\2\2\u0191\u0183\3\2\2"+
		"\2\u0191\u018a\3\2\2\2\u0192^\3\2\2\2\u0193\u0194\7e\2\2\u0194\u0195\7"+
		"t\2\2\u0195\u0196\7g\2\2\u0196\u0197\7c\2\2\u0197\u0198\7v\2\2\u0198\u01a0"+
		"\7g\2\2\u0199\u019a\7E\2\2\u019a\u019b\7t\2\2\u019b\u019c\7g\2\2\u019c"+
		"\u019d\7c\2\2\u019d\u019e\7v\2\2\u019e\u01a0\7g\2\2\u019f\u0193\3\2\2"+
		"\2\u019f\u0199\3\2\2\2\u01a0`\3\2\2\2\u01a1\u01a2\7g\2\2\u01a2\u01a3\7"+
		"o\2\2\u01a3\u01a4\7d\2\2\u01a4\u01a5\7g\2\2\u01a5\u01ac\7f\2\2\u01a6\u01a7"+
		"\7G\2\2\u01a7\u01a8\7o\2\2\u01a8\u01a9\7d\2\2\u01a9\u01aa\7g\2\2\u01aa"+
		"\u01ac\7f\2\2\u01ab\u01a1\3\2\2\2\u01ab\u01a6\3\2\2\2\u01acb\3\2\2\2\u01ad"+
		"\u01b2\5i\65\2\u01ae\u01b1\5i\65\2\u01af\u01b1\5g\64\2\u01b0\u01ae\3\2"+
		"\2\2\u01b0\u01af\3\2\2\2\u01b1\u01b4\3\2\2\2\u01b2\u01b0\3\2\2\2\u01b2"+
		"\u01b3\3\2\2\2\u01b3d\3\2\2\2\u01b4\u01b2\3\2\2\2\u01b5\u01b7\t\3\2\2"+
		"\u01b6\u01b5\3\2\2\2\u01b7\u01b8\3\2\2\2\u01b8\u01b6\3\2\2\2\u01b8\u01b9"+
		"\3\2\2\2\u01b9\u01ba\3\2\2\2\u01ba\u01bb\b\63\3\2\u01bbf\3\2\2\2\u01bc"+
		"\u01bd\t\4\2\2\u01bdh\3\2\2\2\u01be\u01bf\t\5\2\2\u01bfj\3\2\2\2\u01c0"+
		"\u01c1\t\6\2\2\u01c1l\3\2\2\2\u01c2\u01c4\5g\64\2\u01c3\u01c2\3\2\2\2"+
		"\u01c4\u01c5\3\2\2\2\u01c5\u01c3\3\2\2\2\u01c5\u01c6\3\2\2\2\u01c6n\3"+
		"\2\2\2\25\2\u00e0\u00e6\u00f0\u00ff\u0107\u010b\u010e\u0157\u0161\u0171"+
		"\u0181\u0191\u019f\u01ab\u01b0\u01b2\u01b8\u01c5\4\2\3\2\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}