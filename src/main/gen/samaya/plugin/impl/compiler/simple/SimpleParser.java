// Generated from C:/Users/Markus Knecht/Dropbox/Privat/UZH/PhD/code/Samaya/src/main/antlr/samaya/plugin/impl/compiler/simple\Simple.g4 by ANTLR 4.7.2
package samaya.plugin.impl.compiler.simple;

    package samaya.plugin.impl.compiler.simple;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SimpleParser extends Parser {
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
		INSPECT=45, CONSUME=46, CREATE=47, EMBED=48, ID=49, WS=50, INT=51;
	public static final int
		RULE_file = 0, RULE_import_ = 1, RULE_wildcard = 2, RULE_module = 3, RULE_component = 4, 
		RULE_dataDef = 5, RULE_functionDef = 6, RULE_errorDef = 7, RULE_ext = 8, 
		RULE_litSize = 9, RULE_ctrs = 10, RULE_ctr = 11, RULE_fields = 12, RULE_field = 13, 
		RULE_visibility = 14, RULE_risky = 15, RULE_protection = 16, RULE_params = 17, 
		RULE_param = 18, RULE_rets = 19, RULE_ret = 20, RULE_borrows = 21, RULE_funBody = 22, 
		RULE_code = 23, RULE_r_return = 24, RULE_r_throw = 25, RULE_stm = 26, 
		RULE_assig = 27, RULE_lit = 28, RULE_handler = 29, RULE_branch = 30, RULE_extracts = 31, 
		RULE_args = 32, RULE_capability = 33, RULE_genericArgs = 34, RULE_genericDef = 35, 
		RULE_typeRef = 36, RULE_typeRefArgs = 37, RULE_path = 38;
	private static String[] makeRuleNames() {
		return new String[] {
			"file", "import_", "wildcard", "module", "component", "dataDef", "functionDef", 
			"errorDef", "ext", "litSize", "ctrs", "ctr", "fields", "field", "visibility", 
			"risky", "protection", "params", "param", "rets", "ret", "borrows", "funBody", 
			"code", "r_return", "r_throw", "stm", "assig", "lit", "handler", "branch", 
			"extracts", "args", "capability", "genericArgs", "genericDef", "typeRef", 
			"typeRefArgs", "path"
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
			"CREATE", "EMBED", "ID", "WS", "INT"
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

	@Override
	public String getGrammarFileName() { return "Simple.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SimpleParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class FileContext extends ParserRuleContext {
		public ModuleContext module() {
			return getRuleContext(ModuleContext.class,0);
		}
		public List<Import_Context> import_() {
			return getRuleContexts(Import_Context.class);
		}
		public Import_Context import_(int i) {
			return getRuleContext(Import_Context.class,i);
		}
		public FileContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_file; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterFile(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitFile(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitFile(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FileContext file() throws RecognitionException {
		FileContext _localctx = new FileContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_file);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(81);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(78);
				import_();
				}
				}
				setState(83);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(84);
			module();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Import_Context extends ParserRuleContext {
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
		}
		public WildcardContext wildcard() {
			return getRuleContext(WildcardContext.class,0);
		}
		public Import_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_import_; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterImport_(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitImport_(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitImport_(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Import_Context import_() throws RecognitionException {
		Import_Context _localctx = new Import_Context(_ctx, getState());
		enterRule(_localctx, 2, RULE_import_);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(86);
			match(T__0);
			setState(87);
			path();
			setState(89);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(88);
				wildcard();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WildcardContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(SimpleParser.DOT, 0); }
		public WildcardContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_wildcard; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterWildcard(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitWildcard(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitWildcard(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WildcardContext wildcard() throws RecognitionException {
		WildcardContext _localctx = new WildcardContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_wildcard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(91);
			match(DOT);
			setState(92);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ModuleContext extends ParserRuleContext {
		public Token name;
		public ComponentContext components;
		public TerminalNode MODULE() { return getToken(SimpleParser.MODULE, 0); }
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public List<ComponentContext> component() {
			return getRuleContexts(ComponentContext.class);
		}
		public ComponentContext component(int i) {
			return getRuleContext(ComponentContext.class,i);
		}
		public ModuleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_module; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterModule(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitModule(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitModule(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModuleContext module() throws RecognitionException {
		ModuleContext _localctx = new ModuleContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_module);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(94);
			match(MODULE);
			setState(95);
			((ModuleContext)_localctx).name = match(ID);
			setState(96);
			match(T__2);
			setState(100);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << DATA) | (1L << ERROR) | (1L << EXTERNAL) | (1L << LIT) | (1L << DROP) | (1L << COPY) | (1L << PERSIST) | (1L << INSPECT) | (1L << CONSUME) | (1L << CREATE) | (1L << EMBED))) != 0)) {
				{
				{
				setState(97);
				((ModuleContext)_localctx).components = component();
				}
				}
				setState(102);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(103);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ComponentContext extends ParserRuleContext {
		public DataDefContext dataDef() {
			return getRuleContext(DataDefContext.class,0);
		}
		public ErrorDefContext errorDef() {
			return getRuleContext(ErrorDefContext.class,0);
		}
		public FunctionDefContext functionDef() {
			return getRuleContext(FunctionDefContext.class,0);
		}
		public ComponentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_component; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterComponent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitComponent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitComponent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComponentContext component() throws RecognitionException {
		ComponentContext _localctx = new ComponentContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_component);
		try {
			setState(108);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(105);
				dataDef();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(106);
				errorDef();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(107);
				functionDef();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataDefContext extends ParserRuleContext {
		public DataDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataDef; }
	 
		public DataDefContext() { }
		public void copyFrom(DataDefContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class DataContext extends DataDefContext {
		public CapabilityContext capabilities;
		public Token name;
		public GenericArgsContext generics;
		public TerminalNode DATA() { return getToken(SimpleParser.DATA, 0); }
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public ExtContext ext() {
			return getRuleContext(ExtContext.class,0);
		}
		public LitSizeContext litSize() {
			return getRuleContext(LitSizeContext.class,0);
		}
		public CtrsContext ctrs() {
			return getRuleContext(CtrsContext.class,0);
		}
		public List<CapabilityContext> capability() {
			return getRuleContexts(CapabilityContext.class);
		}
		public CapabilityContext capability(int i) {
			return getRuleContext(CapabilityContext.class,i);
		}
		public GenericArgsContext genericArgs() {
			return getRuleContext(GenericArgsContext.class,0);
		}
		public DataContext(DataDefContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterData(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitData(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataDefContext dataDef() throws RecognitionException {
		DataDefContext _localctx = new DataDefContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_dataDef);
		int _la;
		try {
			_localctx = new DataContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(111);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTERNAL) {
				{
				setState(110);
				ext();
				}
			}

			setState(114);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIT) {
				{
				setState(113);
				litSize();
				}
			}

			setState(119);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DROP) | (1L << COPY) | (1L << PERSIST) | (1L << INSPECT) | (1L << CONSUME) | (1L << CREATE) | (1L << EMBED))) != 0)) {
				{
				{
				setState(116);
				((DataContext)_localctx).capabilities = capability();
				}
				}
				setState(121);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(122);
			match(DATA);
			setState(123);
			((DataContext)_localctx).name = match(ID);
			setState(125);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__23) {
				{
				setState(124);
				((DataContext)_localctx).generics = genericArgs();
				}
			}

			setState(128);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__2) {
				{
				setState(127);
				ctrs();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionDefContext extends ParserRuleContext {
		public FunctionDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDef; }
	 
		public FunctionDefContext() { }
		public void copyFrom(FunctionDefContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class FunctionContext extends FunctionDefContext {
		public Token name;
		public GenericArgsContext generics;
		public VisibilityContext visibility() {
			return getRuleContext(VisibilityContext.class,0);
		}
		public TerminalNode FUNCTION() { return getToken(SimpleParser.FUNCTION, 0); }
		public ParamsContext params() {
			return getRuleContext(ParamsContext.class,0);
		}
		public RetsContext rets() {
			return getRuleContext(RetsContext.class,0);
		}
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public ExtContext ext() {
			return getRuleContext(ExtContext.class,0);
		}
		public RiskyContext risky() {
			return getRuleContext(RiskyContext.class,0);
		}
		public FunBodyContext funBody() {
			return getRuleContext(FunBodyContext.class,0);
		}
		public GenericArgsContext genericArgs() {
			return getRuleContext(GenericArgsContext.class,0);
		}
		public FunctionContext(FunctionDefContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDefContext functionDef() throws RecognitionException {
		FunctionDefContext _localctx = new FunctionDefContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_functionDef);
		int _la;
		try {
			_localctx = new FunctionContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(131);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTERNAL) {
				{
				setState(130);
				ext();
				}
			}

			setState(133);
			visibility();
			setState(135);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__10) {
				{
				setState(134);
				risky();
				}
			}

			setState(137);
			match(FUNCTION);
			setState(138);
			((FunctionContext)_localctx).name = match(ID);
			setState(140);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__23) {
				{
				setState(139);
				((FunctionContext)_localctx).generics = genericArgs();
				}
			}

			setState(142);
			params();
			setState(143);
			match(T__4);
			setState(144);
			rets();
			setState(146);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__2) {
				{
				setState(145);
				funBody();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ErrorDefContext extends ParserRuleContext {
		public ErrorDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_errorDef; }
	 
		public ErrorDefContext() { }
		public void copyFrom(ErrorDefContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ErrorContext extends ErrorDefContext {
		public Token name;
		public TerminalNode ERROR() { return getToken(SimpleParser.ERROR, 0); }
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public ErrorContext(ErrorDefContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterError(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitError(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitError(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorDefContext errorDef() throws RecognitionException {
		ErrorDefContext _localctx = new ErrorDefContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_errorDef);
		try {
			_localctx = new ErrorContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			match(ERROR);
			setState(149);
			((ErrorContext)_localctx).name = match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExtContext extends ParserRuleContext {
		public Token size;
		public TerminalNode EXTERNAL() { return getToken(SimpleParser.EXTERNAL, 0); }
		public TerminalNode INT() { return getToken(SimpleParser.INT, 0); }
		public ExtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ext; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterExt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitExt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitExt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExtContext ext() throws RecognitionException {
		ExtContext _localctx = new ExtContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_ext);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(151);
			match(EXTERNAL);
			setState(152);
			match(T__5);
			setState(153);
			((ExtContext)_localctx).size = match(INT);
			setState(154);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LitSizeContext extends ParserRuleContext {
		public Token size;
		public TerminalNode LIT() { return getToken(SimpleParser.LIT, 0); }
		public TerminalNode INT() { return getToken(SimpleParser.INT, 0); }
		public LitSizeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_litSize; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterLitSize(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitLitSize(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitLitSize(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LitSizeContext litSize() throws RecognitionException {
		LitSizeContext _localctx = new LitSizeContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_litSize);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(156);
			match(LIT);
			setState(157);
			match(T__5);
			setState(158);
			((LitSizeContext)_localctx).size = match(INT);
			setState(159);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CtrsContext extends ParserRuleContext {
		public CtrContext ctr;
		public List<CtrContext> c = new ArrayList<CtrContext>();
		public List<CtrContext> ctr() {
			return getRuleContexts(CtrContext.class);
		}
		public CtrContext ctr(int i) {
			return getRuleContext(CtrContext.class,i);
		}
		public CtrsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ctrs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterCtrs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitCtrs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitCtrs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CtrsContext ctrs() throws RecognitionException {
		CtrsContext _localctx = new CtrsContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_ctrs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(161);
			match(T__2);
			setState(165);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ID) {
				{
				{
				setState(162);
				((CtrsContext)_localctx).ctr = ctr();
				((CtrsContext)_localctx).c.add(((CtrsContext)_localctx).ctr);
				}
				}
				setState(167);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(168);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CtrContext extends ParserRuleContext {
		public Token name;
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public FieldsContext fields() {
			return getRuleContext(FieldsContext.class,0);
		}
		public CtrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ctr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterCtr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitCtr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitCtr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CtrContext ctr() throws RecognitionException {
		CtrContext _localctx = new CtrContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_ctr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(170);
			((CtrContext)_localctx).name = match(ID);
			setState(172);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(171);
				fields();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldsContext extends ParserRuleContext {
		public FieldContext field;
		public List<FieldContext> f = new ArrayList<FieldContext>();
		public List<FieldContext> field() {
			return getRuleContexts(FieldContext.class);
		}
		public FieldContext field(int i) {
			return getRuleContext(FieldContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public FieldsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fields; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterFields(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitFields(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitFields(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldsContext fields() throws RecognitionException {
		FieldsContext _localctx = new FieldsContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_fields);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(174);
			match(T__5);
			{
			setState(175);
			((FieldsContext)_localctx).field = field();
			((FieldsContext)_localctx).f.add(((FieldsContext)_localctx).field);
			}
			setState(180);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(176);
				match(COMMA);
				setState(177);
				((FieldsContext)_localctx).field = field();
				((FieldsContext)_localctx).f.add(((FieldsContext)_localctx).field);
				}
				}
				setState(182);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(183);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldContext extends ParserRuleContext {
		public Token name;
		public TypeRefContext typeRef() {
			return getRuleContext(TypeRefContext.class,0);
		}
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_field);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(185);
			((FieldContext)_localctx).name = match(ID);
			setState(186);
			match(T__4);
			setState(187);
			typeRef();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VisibilityContext extends ParserRuleContext {
		public VisibilityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_visibility; }
	 
		public VisibilityContext() { }
		public void copyFrom(VisibilityContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ProtectedContext extends VisibilityContext {
		public ProtectionContext protection() {
			return getRuleContext(ProtectionContext.class,0);
		}
		public ProtectedContext(VisibilityContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterProtected(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitProtected(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitProtected(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PrivateContext extends VisibilityContext {
		public PrivateContext(VisibilityContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterPrivate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitPrivate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitPrivate(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PublicContext extends VisibilityContext {
		public PublicContext(VisibilityContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterPublic(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitPublic(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitPublic(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VisibilityContext visibility() throws RecognitionException {
		VisibilityContext _localctx = new VisibilityContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_visibility);
		try {
			setState(193);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__7:
				_localctx = new PublicContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(189);
				match(T__7);
				}
				break;
			case T__8:
				_localctx = new PrivateContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(190);
				match(T__8);
				}
				break;
			case T__9:
				_localctx = new ProtectedContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(191);
				match(T__9);
				setState(192);
				protection();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RiskyContext extends ParserRuleContext {
		public PathContext path;
		public List<PathContext> r = new ArrayList<PathContext>();
		public List<PathContext> path() {
			return getRuleContexts(PathContext.class);
		}
		public PathContext path(int i) {
			return getRuleContext(PathContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public RiskyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_risky; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterRisky(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitRisky(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitRisky(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RiskyContext risky() throws RecognitionException {
		RiskyContext _localctx = new RiskyContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_risky);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(195);
			match(T__10);
			setState(196);
			match(T__5);
			{
			setState(197);
			((RiskyContext)_localctx).path = path();
			((RiskyContext)_localctx).r.add(((RiskyContext)_localctx).path);
			}
			setState(202);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(198);
				match(COMMA);
				setState(199);
				((RiskyContext)_localctx).path = path();
				((RiskyContext)_localctx).r.add(((RiskyContext)_localctx).path);
				}
				}
				setState(204);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(205);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProtectionContext extends ParserRuleContext {
		public Token ID;
		public List<Token> p = new ArrayList<Token>();
		public List<TerminalNode> ID() { return getTokens(SimpleParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SimpleParser.ID, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public ProtectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_protection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterProtection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitProtection(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitProtection(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProtectionContext protection() throws RecognitionException {
		ProtectionContext _localctx = new ProtectionContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_protection);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(207);
			match(T__5);
			{
			setState(208);
			((ProtectionContext)_localctx).ID = match(ID);
			((ProtectionContext)_localctx).p.add(((ProtectionContext)_localctx).ID);
			}
			setState(213);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(209);
				match(COMMA);
				setState(210);
				((ProtectionContext)_localctx).ID = match(ID);
				((ProtectionContext)_localctx).p.add(((ProtectionContext)_localctx).ID);
				}
				}
				setState(215);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(216);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ParamsContext extends ParserRuleContext {
		public ParamContext param;
		public List<ParamContext> p = new ArrayList<ParamContext>();
		public List<ParamContext> param() {
			return getRuleContexts(ParamContext.class);
		}
		public ParamContext param(int i) {
			return getRuleContext(ParamContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public ParamsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_params; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterParams(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitParams(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitParams(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParamsContext params() throws RecognitionException {
		ParamsContext _localctx = new ParamsContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_params);
		int _la;
		try {
			setState(231);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(218);
				match(T__5);
				{
				setState(219);
				((ParamsContext)_localctx).param = param();
				((ParamsContext)_localctx).p.add(((ParamsContext)_localctx).param);
				}
				setState(224);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(220);
					match(COMMA);
					setState(221);
					((ParamsContext)_localctx).param = param();
					((ParamsContext)_localctx).p.add(((ParamsContext)_localctx).param);
					}
					}
					setState(226);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(227);
				match(T__6);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(229);
				match(T__5);
				setState(230);
				match(T__6);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ParamContext extends ParserRuleContext {
		public Token name;
		public TypeRefContext type;
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public TypeRefContext typeRef() {
			return getRuleContext(TypeRefContext.class,0);
		}
		public TerminalNode CONSUME() { return getToken(SimpleParser.CONSUME, 0); }
		public ParamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_param; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterParam(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitParam(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitParam(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParamContext param() throws RecognitionException {
		ParamContext _localctx = new ParamContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_param);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(234);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CONSUME) {
				{
				setState(233);
				match(CONSUME);
				}
			}

			setState(236);
			((ParamContext)_localctx).name = match(ID);
			setState(237);
			match(T__4);
			setState(238);
			((ParamContext)_localctx).type = typeRef();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RetsContext extends ParserRuleContext {
		public RetContext ret;
		public List<RetContext> r = new ArrayList<RetContext>();
		public List<RetContext> ret() {
			return getRuleContexts(RetContext.class);
		}
		public RetContext ret(int i) {
			return getRuleContext(RetContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public RetsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rets; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterRets(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitRets(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitRets(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RetsContext rets() throws RecognitionException {
		RetsContext _localctx = new RetsContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_rets);
		int _la;
		try {
			setState(254);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(240);
				match(T__5);
				{
				setState(241);
				((RetsContext)_localctx).ret = ret();
				((RetsContext)_localctx).r.add(((RetsContext)_localctx).ret);
				}
				setState(246);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(242);
					match(COMMA);
					setState(243);
					((RetsContext)_localctx).ret = ret();
					((RetsContext)_localctx).r.add(((RetsContext)_localctx).ret);
					}
					}
					setState(248);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(249);
				match(T__6);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(251);
				match(T__5);
				setState(252);
				match(T__6);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(253);
				((RetsContext)_localctx).ret = ret();
				((RetsContext)_localctx).r.add(((RetsContext)_localctx).ret);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RetContext extends ParserRuleContext {
		public Token name;
		public TypeRefContext type;
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public TypeRefContext typeRef() {
			return getRuleContext(TypeRefContext.class,0);
		}
		public BorrowsContext borrows() {
			return getRuleContext(BorrowsContext.class,0);
		}
		public RetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ret; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterRet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitRet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitRet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RetContext ret() throws RecognitionException {
		RetContext _localctx = new RetContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_ret);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(257);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__11) {
				{
				setState(256);
				borrows();
				}
			}

			setState(259);
			((RetContext)_localctx).name = match(ID);
			setState(260);
			match(T__4);
			setState(261);
			((RetContext)_localctx).type = typeRef();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BorrowsContext extends ParserRuleContext {
		public Token ID;
		public List<Token> b = new ArrayList<Token>();
		public List<TerminalNode> ID() { return getTokens(SimpleParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SimpleParser.ID, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public BorrowsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_borrows; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterBorrows(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitBorrows(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitBorrows(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BorrowsContext borrows() throws RecognitionException {
		BorrowsContext _localctx = new BorrowsContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_borrows);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(263);
			match(T__11);
			setState(264);
			match(T__5);
			{
			setState(265);
			((BorrowsContext)_localctx).ID = match(ID);
			((BorrowsContext)_localctx).b.add(((BorrowsContext)_localctx).ID);
			}
			setState(270);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(266);
				match(COMMA);
				setState(267);
				((BorrowsContext)_localctx).ID = match(ID);
				((BorrowsContext)_localctx).b.add(((BorrowsContext)_localctx).ID);
				}
				}
				setState(272);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(273);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunBodyContext extends ParserRuleContext {
		public CodeContext code() {
			return getRuleContext(CodeContext.class,0);
		}
		public FunBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_funBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterFunBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitFunBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitFunBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunBodyContext funBody() throws RecognitionException {
		FunBodyContext _localctx = new FunBodyContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_funBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(275);
			match(T__2);
			setState(276);
			code();
			setState(277);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CodeContext extends ParserRuleContext {
		public R_returnContext r_return() {
			return getRuleContext(R_returnContext.class,0);
		}
		public List<StmContext> stm() {
			return getRuleContexts(StmContext.class);
		}
		public StmContext stm(int i) {
			return getRuleContext(StmContext.class,i);
		}
		public R_throwContext r_throw() {
			return getRuleContext(R_throwContext.class,0);
		}
		public CodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_code; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterCode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitCode(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitCode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CodeContext code() throws RecognitionException {
		CodeContext _localctx = new CodeContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_code);
		int _la;
		try {
			setState(287);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__5:
			case RETURN:
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(282);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__5 || _la==ID) {
					{
					{
					setState(279);
					stm();
					}
					}
					setState(284);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(285);
				r_return();
				}
				break;
			case THROW:
				enterOuterAlt(_localctx, 2);
				{
				setState(286);
				r_throw();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class R_returnContext extends ParserRuleContext {
		public Token ID;
		public List<Token> r = new ArrayList<Token>();
		public TerminalNode RETURN() { return getToken(SimpleParser.RETURN, 0); }
		public List<TerminalNode> ID() { return getTokens(SimpleParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SimpleParser.ID, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public R_returnContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_r_return; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterR_return(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitR_return(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitR_return(this);
			else return visitor.visitChildren(this);
		}
	}

	public final R_returnContext r_return() throws RecognitionException {
		R_returnContext _localctx = new R_returnContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_r_return);
		int _la;
		try {
			setState(306);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(289);
				match(RETURN);
				setState(290);
				match(T__5);
				{
				setState(291);
				((R_returnContext)_localctx).ID = match(ID);
				((R_returnContext)_localctx).r.add(((R_returnContext)_localctx).ID);
				}
				setState(296);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(292);
					match(COMMA);
					setState(293);
					((R_returnContext)_localctx).ID = match(ID);
					((R_returnContext)_localctx).r.add(((R_returnContext)_localctx).ID);
					}
					}
					setState(298);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(299);
				match(T__6);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(300);
				match(RETURN);
				setState(301);
				((R_returnContext)_localctx).ID = match(ID);
				((R_returnContext)_localctx).r.add(((R_returnContext)_localctx).ID);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(302);
				match(RETURN);
				setState(303);
				match(T__5);
				setState(304);
				match(T__6);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(305);
				match(RETURN);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class R_throwContext extends ParserRuleContext {
		public PathContext err;
		public TerminalNode THROW() { return getToken(SimpleParser.THROW, 0); }
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
		}
		public R_throwContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_r_throw; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterR_throw(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitR_throw(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitR_throw(this);
			else return visitor.visitChildren(this);
		}
	}

	public final R_throwContext r_throw() throws RecognitionException {
		R_throwContext _localctx = new R_throwContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_r_throw);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(308);
			match(THROW);
			setState(309);
			((R_throwContext)_localctx).err = path();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StmContext extends ParserRuleContext {
		public StmContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stm; }
	 
		public StmContext() { }
		public void copyFrom(StmContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class FetchContext extends StmContext {
		public Token var;
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public TerminalNode AMP() { return getToken(SimpleParser.AMP, 0); }
		public FetchContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterFetch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitFetch(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitFetch(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExtractImageContext extends StmContext {
		public Token var;
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public ExtractImageContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterExtractImage(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitExtractImage(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitExtractImage(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SwitchContext extends StmContext {
		public Token var;
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public TerminalNode AMP() { return getToken(SimpleParser.AMP, 0); }
		public List<BranchContext> branch() {
			return getRuleContexts(BranchContext.class);
		}
		public BranchContext branch(int i) {
			return getRuleContext(BranchContext.class,i);
		}
		public SwitchContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterSwitch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitSwitch(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitSwitch(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LiteralContext extends StmContext {
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
		}
		public LitContext lit() {
			return getRuleContext(LitContext.class,0);
		}
		public TerminalNode AMP() { return getToken(SimpleParser.AMP, 0); }
		public LiteralContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GetContext extends StmContext {
		public Token var;
		public Token target;
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public TerminalNode DOT() { return getToken(SimpleParser.DOT, 0); }
		public List<TerminalNode> ID() { return getTokens(SimpleParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SimpleParser.ID, i);
		}
		public TerminalNode AMP() { return getToken(SimpleParser.AMP, 0); }
		public GetContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterGet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitGet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitGet(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LetContext extends StmContext {
		public CodeContext body;
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public CodeContext code() {
			return getRuleContext(CodeContext.class,0);
		}
		public LetContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterLet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitLet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitLet(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TryContext extends StmContext {
		public CodeContext body;
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public CodeContext code() {
			return getRuleContext(CodeContext.class,0);
		}
		public List<HandlerContext> handler() {
			return getRuleContexts(HandlerContext.class);
		}
		public HandlerContext handler(int i) {
			return getRuleContext(HandlerContext.class,i);
		}
		public TryContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterTry(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitTry(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitTry(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PackContext extends StmContext {
		public Token ctrName;
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public TypeRefContext typeRef() {
			return getRuleContext(TypeRefContext.class,0);
		}
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public TerminalNode AMP() { return getToken(SimpleParser.AMP, 0); }
		public ArgsContext args() {
			return getRuleContext(ArgsContext.class,0);
		}
		public PackContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterPack(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitPack(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitPack(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UnpackContext extends StmContext {
		public Token var;
		public ExtractsContext extracts() {
			return getRuleContext(ExtractsContext.class,0);
		}
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public TerminalNode AMP() { return getToken(SimpleParser.AMP, 0); }
		public UnpackContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterUnpack(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitUnpack(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitUnpack(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ImageContext extends StmContext {
		public Token var;
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public ImageContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterImage(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitImage(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitImage(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InvokeContext extends StmContext {
		public AssigContext assig() {
			return getRuleContext(AssigContext.class,0);
		}
		public TypeRefContext typeRef() {
			return getRuleContext(TypeRefContext.class,0);
		}
		public ArgsContext args() {
			return getRuleContext(ArgsContext.class,0);
		}
		public InvokeContext(StmContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterInvoke(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitInvoke(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitInvoke(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StmContext stm() throws RecognitionException {
		StmContext _localctx = new StmContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_stm);
		int _la;
		try {
			setState(387);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				_localctx = new PackContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(311);
				assig();
				setState(313);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AMP) {
					{
					setState(312);
					match(AMP);
					}
				}

				setState(315);
				typeRef();
				setState(316);
				match(T__12);
				setState(317);
				((PackContext)_localctx).ctrName = match(ID);
				setState(319);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
				case 1:
					{
					setState(318);
					args();
					}
					break;
				}
				}
				break;
			case 2:
				_localctx = new LiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(321);
				assig();
				setState(323);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AMP) {
					{
					setState(322);
					match(AMP);
					}
				}

				setState(325);
				path();
				setState(326);
				lit();
				}
				break;
			case 3:
				_localctx = new InvokeContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(328);
				assig();
				setState(329);
				typeRef();
				setState(330);
				args();
				}
				break;
			case 4:
				_localctx = new GetContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(332);
				assig();
				setState(334);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AMP) {
					{
					setState(333);
					match(AMP);
					}
				}

				setState(336);
				((GetContext)_localctx).var = match(ID);
				setState(337);
				match(DOT);
				setState(338);
				((GetContext)_localctx).target = match(ID);
				}
				break;
			case 5:
				_localctx = new FetchContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(340);
				assig();
				setState(342);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AMP) {
					{
					setState(341);
					match(AMP);
					}
				}

				setState(344);
				((FetchContext)_localctx).var = match(ID);
				}
				break;
			case 6:
				_localctx = new UnpackContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(346);
				extracts();
				setState(347);
				match(T__13);
				setState(349);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AMP) {
					{
					setState(348);
					match(AMP);
					}
				}

				setState(351);
				((UnpackContext)_localctx).var = match(ID);
				}
				break;
			case 7:
				_localctx = new ImageContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(353);
				assig();
				setState(354);
				match(T__14);
				setState(355);
				((ImageContext)_localctx).var = match(ID);
				}
				break;
			case 8:
				_localctx = new ExtractImageContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(357);
				assig();
				setState(358);
				match(T__15);
				setState(359);
				((ExtractImageContext)_localctx).var = match(ID);
				}
				break;
			case 9:
				_localctx = new LetContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(361);
				assig();
				setState(362);
				match(T__2);
				setState(363);
				((LetContext)_localctx).body = code();
				setState(364);
				match(T__3);
				}
				break;
			case 10:
				_localctx = new TryContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(366);
				assig();
				setState(367);
				match(T__16);
				setState(368);
				match(T__2);
				setState(369);
				((TryContext)_localctx).body = code();
				setState(370);
				match(T__3);
				setState(372); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(371);
					handler();
					}
					}
					setState(374); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__20 );
				}
				break;
			case 11:
				_localctx = new SwitchContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(376);
				assig();
				setState(378);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AMP) {
					{
					setState(377);
					match(AMP);
					}
				}

				setState(380);
				match(T__17);
				setState(381);
				((SwitchContext)_localctx).var = match(ID);
				setState(383); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(382);
					branch();
					}
					}
					setState(385); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__21 );
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssigContext extends ParserRuleContext {
		public Token ID;
		public List<Token> val = new ArrayList<Token>();
		public List<TerminalNode> ID() { return getTokens(SimpleParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SimpleParser.ID, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public AssigContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assig; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterAssig(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitAssig(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitAssig(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssigContext assig() throws RecognitionException {
		AssigContext _localctx = new AssigContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_assig);
		int _la;
		try {
			setState(401);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(389);
				((AssigContext)_localctx).ID = match(ID);
				((AssigContext)_localctx).val.add(((AssigContext)_localctx).ID);
				setState(390);
				match(T__18);
				}
				break;
			case T__5:
				enterOuterAlt(_localctx, 2);
				{
				setState(391);
				match(T__5);
				{
				setState(392);
				((AssigContext)_localctx).ID = match(ID);
				((AssigContext)_localctx).val.add(((AssigContext)_localctx).ID);
				}
				setState(397);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(393);
					match(COMMA);
					setState(394);
					((AssigContext)_localctx).ID = match(ID);
					((AssigContext)_localctx).val.add(((AssigContext)_localctx).ID);
					}
					}
					setState(399);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(400);
				match(T__6);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LitContext extends ParserRuleContext {
		public LitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lit; }
	 
		public LitContext() { }
		public void copyFrom(LitContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class NumberContext extends LitContext {
		public TerminalNode NUM() { return getToken(SimpleParser.NUM, 0); }
		public NumberContext(LitContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterNumber(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitNumber(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitNumber(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SymbolContext extends LitContext {
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public SymbolContext(LitContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterSymbol(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitSymbol(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitSymbol(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StringContext extends LitContext {
		public TerminalNode TEXT() { return getToken(SimpleParser.TEXT, 0); }
		public StringContext(LitContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LitContext lit() throws RecognitionException {
		LitContext _localctx = new LitContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_lit);
		try {
			setState(407);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__19:
				_localctx = new SymbolContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(403);
				match(T__19);
				setState(404);
				match(ID);
				}
				break;
			case TEXT:
				_localctx = new StringContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(405);
				match(TEXT);
				}
				break;
			case NUM:
				_localctx = new NumberContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(406);
				match(NUM);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HandlerContext extends ParserRuleContext {
		public CodeContext body;
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
		}
		public CodeContext code() {
			return getRuleContext(CodeContext.class,0);
		}
		public HandlerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_handler; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterHandler(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitHandler(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitHandler(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HandlerContext handler() throws RecognitionException {
		HandlerContext _localctx = new HandlerContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_handler);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(409);
			match(T__20);
			setState(410);
			path();
			setState(411);
			match(T__2);
			setState(412);
			((HandlerContext)_localctx).body = code();
			setState(413);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BranchContext extends ParserRuleContext {
		public Token name;
		public CodeContext body;
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public CodeContext code() {
			return getRuleContext(CodeContext.class,0);
		}
		public ExtractsContext extracts() {
			return getRuleContext(ExtractsContext.class,0);
		}
		public BranchContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_branch; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterBranch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitBranch(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitBranch(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BranchContext branch() throws RecognitionException {
		BranchContext _localctx = new BranchContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_branch);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(415);
			match(T__21);
			setState(416);
			((BranchContext)_localctx).name = match(ID);
			setState(418);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(417);
				extracts();
				}
			}

			setState(420);
			match(T__22);
			setState(421);
			match(T__2);
			setState(422);
			((BranchContext)_localctx).body = code();
			setState(423);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExtractsContext extends ParserRuleContext {
		public Token ID;
		public List<Token> p = new ArrayList<Token>();
		public List<TerminalNode> ID() { return getTokens(SimpleParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SimpleParser.ID, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public ExtractsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extracts; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterExtracts(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitExtracts(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitExtracts(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExtractsContext extracts() throws RecognitionException {
		ExtractsContext _localctx = new ExtractsContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_extracts);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(425);
			match(T__5);
			{
			setState(426);
			((ExtractsContext)_localctx).ID = match(ID);
			((ExtractsContext)_localctx).p.add(((ExtractsContext)_localctx).ID);
			}
			setState(431);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(427);
				match(COMMA);
				setState(428);
				((ExtractsContext)_localctx).ID = match(ID);
				((ExtractsContext)_localctx).p.add(((ExtractsContext)_localctx).ID);
				}
				}
				setState(433);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(434);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgsContext extends ParserRuleContext {
		public Token ID;
		public List<Token> a = new ArrayList<Token>();
		public List<TerminalNode> ID() { return getTokens(SimpleParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SimpleParser.ID, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public ArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_args; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgsContext args() throws RecognitionException {
		ArgsContext _localctx = new ArgsContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_args);
		int _la;
		try {
			setState(448);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,46,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(436);
				match(T__5);
				{
				setState(437);
				((ArgsContext)_localctx).ID = match(ID);
				((ArgsContext)_localctx).a.add(((ArgsContext)_localctx).ID);
				}
				setState(442);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(438);
					match(COMMA);
					setState(439);
					((ArgsContext)_localctx).ID = match(ID);
					((ArgsContext)_localctx).a.add(((ArgsContext)_localctx).ID);
					}
					}
					setState(444);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(445);
				match(T__6);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(446);
				match(T__5);
				setState(447);
				match(T__6);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CapabilityContext extends ParserRuleContext {
		public TerminalNode DROP() { return getToken(SimpleParser.DROP, 0); }
		public TerminalNode COPY() { return getToken(SimpleParser.COPY, 0); }
		public TerminalNode PERSIST() { return getToken(SimpleParser.PERSIST, 0); }
		public TerminalNode INSPECT() { return getToken(SimpleParser.INSPECT, 0); }
		public TerminalNode CONSUME() { return getToken(SimpleParser.CONSUME, 0); }
		public TerminalNode CREATE() { return getToken(SimpleParser.CREATE, 0); }
		public TerminalNode EMBED() { return getToken(SimpleParser.EMBED, 0); }
		public CapabilityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_capability; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterCapability(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitCapability(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitCapability(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CapabilityContext capability() throws RecognitionException {
		CapabilityContext _localctx = new CapabilityContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_capability);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(450);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DROP) | (1L << COPY) | (1L << PERSIST) | (1L << INSPECT) | (1L << CONSUME) | (1L << CREATE) | (1L << EMBED))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GenericArgsContext extends ParserRuleContext {
		public GenericDefContext genericDef;
		public List<GenericDefContext> generics = new ArrayList<GenericDefContext>();
		public List<GenericDefContext> genericDef() {
			return getRuleContexts(GenericDefContext.class);
		}
		public GenericDefContext genericDef(int i) {
			return getRuleContext(GenericDefContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public GenericArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterGenericArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitGenericArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitGenericArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericArgsContext genericArgs() throws RecognitionException {
		GenericArgsContext _localctx = new GenericArgsContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_genericArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(452);
			match(T__23);
			{
			setState(453);
			((GenericArgsContext)_localctx).genericDef = genericDef();
			((GenericArgsContext)_localctx).generics.add(((GenericArgsContext)_localctx).genericDef);
			}
			setState(458);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(454);
				match(COMMA);
				setState(455);
				((GenericArgsContext)_localctx).genericDef = genericDef();
				((GenericArgsContext)_localctx).generics.add(((GenericArgsContext)_localctx).genericDef);
				}
				}
				setState(460);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(461);
			match(T__24);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GenericDefContext extends ParserRuleContext {
		public CapabilityContext capabilities;
		public Token name;
		public TerminalNode ID() { return getToken(SimpleParser.ID, 0); }
		public TerminalNode PHANTOM() { return getToken(SimpleParser.PHANTOM, 0); }
		public List<CapabilityContext> capability() {
			return getRuleContexts(CapabilityContext.class);
		}
		public CapabilityContext capability(int i) {
			return getRuleContext(CapabilityContext.class,i);
		}
		public GenericDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericDef; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterGenericDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitGenericDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitGenericDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericDefContext genericDef() throws RecognitionException {
		GenericDefContext _localctx = new GenericDefContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_genericDef);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(464);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PHANTOM) {
				{
				setState(463);
				match(PHANTOM);
				}
			}

			setState(469);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DROP) | (1L << COPY) | (1L << PERSIST) | (1L << INSPECT) | (1L << CONSUME) | (1L << CREATE) | (1L << EMBED))) != 0)) {
				{
				{
				setState(466);
				((GenericDefContext)_localctx).capabilities = capability();
				}
				}
				setState(471);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(472);
			((GenericDefContext)_localctx).name = match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeRefContext extends ParserRuleContext {
		public TypeRefArgsContext targs;
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
		}
		public TypeRefArgsContext typeRefArgs() {
			return getRuleContext(TypeRefArgsContext.class,0);
		}
		public TypeRefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeRef; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterTypeRef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitTypeRef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitTypeRef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeRefContext typeRef() throws RecognitionException {
		TypeRefContext _localctx = new TypeRefContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_typeRef);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(474);
			path();
			setState(476);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__23) {
				{
				setState(475);
				((TypeRefContext)_localctx).targs = typeRefArgs();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeRefArgsContext extends ParserRuleContext {
		public TypeRefContext typeRef;
		public List<TypeRefContext> targs = new ArrayList<TypeRefContext>();
		public List<TypeRefContext> typeRef() {
			return getRuleContexts(TypeRefContext.class);
		}
		public TypeRefContext typeRef(int i) {
			return getRuleContext(TypeRefContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SimpleParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SimpleParser.COMMA, i);
		}
		public TypeRefArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeRefArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterTypeRefArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitTypeRefArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitTypeRefArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeRefArgsContext typeRefArgs() throws RecognitionException {
		TypeRefArgsContext _localctx = new TypeRefArgsContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_typeRefArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(478);
			match(T__23);
			{
			setState(479);
			((TypeRefArgsContext)_localctx).typeRef = typeRef();
			((TypeRefArgsContext)_localctx).targs.add(((TypeRefArgsContext)_localctx).typeRef);
			}
			setState(484);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(480);
				match(COMMA);
				setState(481);
				((TypeRefArgsContext)_localctx).typeRef = typeRef();
				((TypeRefArgsContext)_localctx).targs.add(((TypeRefArgsContext)_localctx).typeRef);
				}
				}
				setState(486);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(487);
			match(T__24);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathContext extends ParserRuleContext {
		public Token ID;
		public List<Token> part = new ArrayList<Token>();
		public List<TerminalNode> ID() { return getTokens(SimpleParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SimpleParser.ID, i);
		}
		public List<TerminalNode> DOT() { return getTokens(SimpleParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(SimpleParser.DOT, i);
		}
		public PathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_path; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).enterPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SimpleListener ) ((SimpleListener)listener).exitPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SimpleVisitor ) return ((SimpleVisitor<? extends T>)visitor).visitPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathContext path() throws RecognitionException {
		PathContext _localctx = new PathContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_path);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(489);
			((PathContext)_localctx).ID = match(ID);
			((PathContext)_localctx).part.add(((PathContext)_localctx).ID);
			}
			setState(494);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(490);
					match(DOT);
					setState(491);
					((PathContext)_localctx).ID = match(ID);
					((PathContext)_localctx).part.add(((PathContext)_localctx).ID);
					}
					} 
				}
				setState(496);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\65\u01f4\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\3\2\7\2R\n\2\f\2\16"+
		"\2U\13\2\3\2\3\2\3\3\3\3\3\3\5\3\\\n\3\3\4\3\4\3\4\3\5\3\5\3\5\3\5\7\5"+
		"e\n\5\f\5\16\5h\13\5\3\5\3\5\3\6\3\6\3\6\5\6o\n\6\3\7\5\7r\n\7\3\7\5\7"+
		"u\n\7\3\7\7\7x\n\7\f\7\16\7{\13\7\3\7\3\7\3\7\5\7\u0080\n\7\3\7\5\7\u0083"+
		"\n\7\3\b\5\b\u0086\n\b\3\b\3\b\5\b\u008a\n\b\3\b\3\b\3\b\5\b\u008f\n\b"+
		"\3\b\3\b\3\b\3\b\5\b\u0095\n\b\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\13\3"+
		"\13\3\13\3\13\3\13\3\f\3\f\7\f\u00a6\n\f\f\f\16\f\u00a9\13\f\3\f\3\f\3"+
		"\r\3\r\5\r\u00af\n\r\3\16\3\16\3\16\3\16\7\16\u00b5\n\16\f\16\16\16\u00b8"+
		"\13\16\3\16\3\16\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\5\20\u00c4\n"+
		"\20\3\21\3\21\3\21\3\21\3\21\7\21\u00cb\n\21\f\21\16\21\u00ce\13\21\3"+
		"\21\3\21\3\22\3\22\3\22\3\22\7\22\u00d6\n\22\f\22\16\22\u00d9\13\22\3"+
		"\22\3\22\3\23\3\23\3\23\3\23\7\23\u00e1\n\23\f\23\16\23\u00e4\13\23\3"+
		"\23\3\23\3\23\3\23\5\23\u00ea\n\23\3\24\5\24\u00ed\n\24\3\24\3\24\3\24"+
		"\3\24\3\25\3\25\3\25\3\25\7\25\u00f7\n\25\f\25\16\25\u00fa\13\25\3\25"+
		"\3\25\3\25\3\25\3\25\5\25\u0101\n\25\3\26\5\26\u0104\n\26\3\26\3\26\3"+
		"\26\3\26\3\27\3\27\3\27\3\27\3\27\7\27\u010f\n\27\f\27\16\27\u0112\13"+
		"\27\3\27\3\27\3\30\3\30\3\30\3\30\3\31\7\31\u011b\n\31\f\31\16\31\u011e"+
		"\13\31\3\31\3\31\5\31\u0122\n\31\3\32\3\32\3\32\3\32\3\32\7\32\u0129\n"+
		"\32\f\32\16\32\u012c\13\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\5\32\u0135"+
		"\n\32\3\33\3\33\3\33\3\34\3\34\5\34\u013c\n\34\3\34\3\34\3\34\3\34\5\34"+
		"\u0142\n\34\3\34\3\34\5\34\u0146\n\34\3\34\3\34\3\34\3\34\3\34\3\34\3"+
		"\34\3\34\3\34\5\34\u0151\n\34\3\34\3\34\3\34\3\34\3\34\3\34\5\34\u0159"+
		"\n\34\3\34\3\34\3\34\3\34\3\34\5\34\u0160\n\34\3\34\3\34\3\34\3\34\3\34"+
		"\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34"+
		"\3\34\3\34\6\34\u0177\n\34\r\34\16\34\u0178\3\34\3\34\5\34\u017d\n\34"+
		"\3\34\3\34\3\34\6\34\u0182\n\34\r\34\16\34\u0183\5\34\u0186\n\34\3\35"+
		"\3\35\3\35\3\35\3\35\3\35\7\35\u018e\n\35\f\35\16\35\u0191\13\35\3\35"+
		"\5\35\u0194\n\35\3\36\3\36\3\36\3\36\5\36\u019a\n\36\3\37\3\37\3\37\3"+
		"\37\3\37\3\37\3 \3 \3 \5 \u01a5\n \3 \3 \3 \3 \3 \3!\3!\3!\3!\7!\u01b0"+
		"\n!\f!\16!\u01b3\13!\3!\3!\3\"\3\"\3\"\3\"\7\"\u01bb\n\"\f\"\16\"\u01be"+
		"\13\"\3\"\3\"\3\"\5\"\u01c3\n\"\3#\3#\3$\3$\3$\3$\7$\u01cb\n$\f$\16$\u01ce"+
		"\13$\3$\3$\3%\5%\u01d3\n%\3%\7%\u01d6\n%\f%\16%\u01d9\13%\3%\3%\3&\3&"+
		"\5&\u01df\n&\3\'\3\'\3\'\3\'\7\'\u01e5\n\'\f\'\16\'\u01e8\13\'\3\'\3\'"+
		"\3(\3(\3(\7(\u01ef\n(\f(\16(\u01f2\13(\3(\2\2)\2\4\6\b\n\f\16\20\22\24"+
		"\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLN\2\3\3\2,\62\2\u0210\2"+
		"S\3\2\2\2\4X\3\2\2\2\6]\3\2\2\2\b`\3\2\2\2\nn\3\2\2\2\fq\3\2\2\2\16\u0085"+
		"\3\2\2\2\20\u0096\3\2\2\2\22\u0099\3\2\2\2\24\u009e\3\2\2\2\26\u00a3\3"+
		"\2\2\2\30\u00ac\3\2\2\2\32\u00b0\3\2\2\2\34\u00bb\3\2\2\2\36\u00c3\3\2"+
		"\2\2 \u00c5\3\2\2\2\"\u00d1\3\2\2\2$\u00e9\3\2\2\2&\u00ec\3\2\2\2(\u0100"+
		"\3\2\2\2*\u0103\3\2\2\2,\u0109\3\2\2\2.\u0115\3\2\2\2\60\u0121\3\2\2\2"+
		"\62\u0134\3\2\2\2\64\u0136\3\2\2\2\66\u0185\3\2\2\28\u0193\3\2\2\2:\u0199"+
		"\3\2\2\2<\u019b\3\2\2\2>\u01a1\3\2\2\2@\u01ab\3\2\2\2B\u01c2\3\2\2\2D"+
		"\u01c4\3\2\2\2F\u01c6\3\2\2\2H\u01d2\3\2\2\2J\u01dc\3\2\2\2L\u01e0\3\2"+
		"\2\2N\u01eb\3\2\2\2PR\5\4\3\2QP\3\2\2\2RU\3\2\2\2SQ\3\2\2\2ST\3\2\2\2"+
		"TV\3\2\2\2US\3\2\2\2VW\5\b\5\2W\3\3\2\2\2XY\7\3\2\2Y[\5N(\2Z\\\5\6\4\2"+
		"[Z\3\2\2\2[\\\3\2\2\2\\\5\3\2\2\2]^\7\37\2\2^_\7\4\2\2_\7\3\2\2\2`a\7"+
		"#\2\2ab\7\63\2\2bf\7\5\2\2ce\5\n\6\2dc\3\2\2\2eh\3\2\2\2fd\3\2\2\2fg\3"+
		"\2\2\2gi\3\2\2\2hf\3\2\2\2ij\7\6\2\2j\t\3\2\2\2ko\5\f\7\2lo\5\20\t\2m"+
		"o\5\16\b\2nk\3\2\2\2nl\3\2\2\2nm\3\2\2\2o\13\3\2\2\2pr\5\22\n\2qp\3\2"+
		"\2\2qr\3\2\2\2rt\3\2\2\2su\5\24\13\2ts\3\2\2\2tu\3\2\2\2uy\3\2\2\2vx\5"+
		"D#\2wv\3\2\2\2x{\3\2\2\2yw\3\2\2\2yz\3\2\2\2z|\3\2\2\2{y\3\2\2\2|}\7$"+
		"\2\2}\177\7\63\2\2~\u0080\5F$\2\177~\3\2\2\2\177\u0080\3\2\2\2\u0080\u0082"+
		"\3\2\2\2\u0081\u0083\5\26\f\2\u0082\u0081\3\2\2\2\u0082\u0083\3\2\2\2"+
		"\u0083\r\3\2\2\2\u0084\u0086\5\22\n\2\u0085\u0084\3\2\2\2\u0085\u0086"+
		"\3\2\2\2\u0086\u0087\3\2\2\2\u0087\u0089\5\36\20\2\u0088\u008a\5 \21\2"+
		"\u0089\u0088\3\2\2\2\u0089\u008a\3\2\2\2\u008a\u008b\3\2\2\2\u008b\u008c"+
		"\7&\2\2\u008c\u008e\7\63\2\2\u008d\u008f\5F$\2\u008e\u008d\3\2\2\2\u008e"+
		"\u008f\3\2\2\2\u008f\u0090\3\2\2\2\u0090\u0091\5$\23\2\u0091\u0092\7\7"+
		"\2\2\u0092\u0094\5(\25\2\u0093\u0095\5.\30\2\u0094\u0093\3\2\2\2\u0094"+
		"\u0095\3\2\2\2\u0095\17\3\2\2\2\u0096\u0097\7%\2\2\u0097\u0098\7\63\2"+
		"\2\u0098\21\3\2\2\2\u0099\u009a\7\'\2\2\u009a\u009b\7\b\2\2\u009b\u009c"+
		"\7\65\2\2\u009c\u009d\7\t\2\2\u009d\23\3\2\2\2\u009e\u009f\7(\2\2\u009f"+
		"\u00a0\7\b\2\2\u00a0\u00a1\7\65\2\2\u00a1\u00a2\7\t\2\2\u00a2\25\3\2\2"+
		"\2\u00a3\u00a7\7\5\2\2\u00a4\u00a6\5\30\r\2\u00a5\u00a4\3\2\2\2\u00a6"+
		"\u00a9\3\2\2\2\u00a7\u00a5\3\2\2\2\u00a7\u00a8\3\2\2\2\u00a8\u00aa\3\2"+
		"\2\2\u00a9\u00a7\3\2\2\2\u00aa\u00ab\7\6\2\2\u00ab\27\3\2\2\2\u00ac\u00ae"+
		"\7\63\2\2\u00ad\u00af\5\32\16\2\u00ae\u00ad\3\2\2\2\u00ae\u00af\3\2\2"+
		"\2\u00af\31\3\2\2\2\u00b0\u00b1\7\b\2\2\u00b1\u00b6\5\34\17\2\u00b2\u00b3"+
		"\7\36\2\2\u00b3\u00b5\5\34\17\2\u00b4\u00b2\3\2\2\2\u00b5\u00b8\3\2\2"+
		"\2\u00b6\u00b4\3\2\2\2\u00b6\u00b7\3\2\2\2\u00b7\u00b9\3\2\2\2\u00b8\u00b6"+
		"\3\2\2\2\u00b9\u00ba\7\t\2\2\u00ba\33\3\2\2\2\u00bb\u00bc\7\63\2\2\u00bc"+
		"\u00bd\7\7\2\2\u00bd\u00be\5J&\2\u00be\35\3\2\2\2\u00bf\u00c4\7\n\2\2"+
		"\u00c0\u00c4\7\13\2\2\u00c1\u00c2\7\f\2\2\u00c2\u00c4\5\"\22\2\u00c3\u00bf"+
		"\3\2\2\2\u00c3\u00c0\3\2\2\2\u00c3\u00c1\3\2\2\2\u00c4\37\3\2\2\2\u00c5"+
		"\u00c6\7\r\2\2\u00c6\u00c7\7\b\2\2\u00c7\u00cc\5N(\2\u00c8\u00c9\7\36"+
		"\2\2\u00c9\u00cb\5N(\2\u00ca\u00c8\3\2\2\2\u00cb\u00ce\3\2\2\2\u00cc\u00ca"+
		"\3\2\2\2\u00cc\u00cd\3\2\2\2\u00cd\u00cf\3\2\2\2\u00ce\u00cc\3\2\2\2\u00cf"+
		"\u00d0\7\t\2\2\u00d0!\3\2\2\2\u00d1\u00d2\7\b\2\2\u00d2\u00d7\7\63\2\2"+
		"\u00d3\u00d4\7\36\2\2\u00d4\u00d6\7\63\2\2\u00d5\u00d3\3\2\2\2\u00d6\u00d9"+
		"\3\2\2\2\u00d7\u00d5\3\2\2\2\u00d7\u00d8\3\2\2\2\u00d8\u00da\3\2\2\2\u00d9"+
		"\u00d7\3\2\2\2\u00da\u00db\7\t\2\2\u00db#\3\2\2\2\u00dc\u00dd\7\b\2\2"+
		"\u00dd\u00e2\5&\24\2\u00de\u00df\7\36\2\2\u00df\u00e1\5&\24\2\u00e0\u00de"+
		"\3\2\2\2\u00e1\u00e4\3\2\2\2\u00e2\u00e0\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3"+
		"\u00e5\3\2\2\2\u00e4\u00e2\3\2\2\2\u00e5\u00e6\7\t\2\2\u00e6\u00ea\3\2"+
		"\2\2\u00e7\u00e8\7\b\2\2\u00e8\u00ea\7\t\2\2\u00e9\u00dc\3\2\2\2\u00e9"+
		"\u00e7\3\2\2\2\u00ea%\3\2\2\2\u00eb\u00ed\7\60\2\2\u00ec\u00eb\3\2\2\2"+
		"\u00ec\u00ed\3\2\2\2\u00ed\u00ee\3\2\2\2\u00ee\u00ef\7\63\2\2\u00ef\u00f0"+
		"\7\7\2\2\u00f0\u00f1\5J&\2\u00f1\'\3\2\2\2\u00f2\u00f3\7\b\2\2\u00f3\u00f8"+
		"\5*\26\2\u00f4\u00f5\7\36\2\2\u00f5\u00f7\5*\26\2\u00f6\u00f4\3\2\2\2"+
		"\u00f7\u00fa\3\2\2\2\u00f8\u00f6\3\2\2\2\u00f8\u00f9\3\2\2\2\u00f9\u00fb"+
		"\3\2\2\2\u00fa\u00f8\3\2\2\2\u00fb\u00fc\7\t\2\2\u00fc\u0101\3\2\2\2\u00fd"+
		"\u00fe\7\b\2\2\u00fe\u0101\7\t\2\2\u00ff\u0101\5*\26\2\u0100\u00f2\3\2"+
		"\2\2\u0100\u00fd\3\2\2\2\u0100\u00ff\3\2\2\2\u0101)\3\2\2\2\u0102\u0104"+
		"\5,\27\2\u0103\u0102\3\2\2\2\u0103\u0104\3\2\2\2\u0104\u0105\3\2\2\2\u0105"+
		"\u0106\7\63\2\2\u0106\u0107\7\7\2\2\u0107\u0108\5J&\2\u0108+\3\2\2\2\u0109"+
		"\u010a\7\16\2\2\u010a\u010b\7\b\2\2\u010b\u0110\7\63\2\2\u010c\u010d\7"+
		"\36\2\2\u010d\u010f\7\63\2\2\u010e\u010c\3\2\2\2\u010f\u0112\3\2\2\2\u0110"+
		"\u010e\3\2\2\2\u0110\u0111\3\2\2\2\u0111\u0113\3\2\2\2\u0112\u0110\3\2"+
		"\2\2\u0113\u0114\7\t\2\2\u0114-\3\2\2\2\u0115\u0116\7\5\2\2\u0116\u0117"+
		"\5\60\31\2\u0117\u0118\7\6\2\2\u0118/\3\2\2\2\u0119\u011b\5\66\34\2\u011a"+
		"\u0119\3\2\2\2\u011b\u011e\3\2\2\2\u011c\u011a\3\2\2\2\u011c\u011d\3\2"+
		"\2\2\u011d\u011f\3\2\2\2\u011e\u011c\3\2\2\2\u011f\u0122\5\62\32\2\u0120"+
		"\u0122\5\64\33\2\u0121\u011c\3\2\2\2\u0121\u0120\3\2\2\2\u0122\61\3\2"+
		"\2\2\u0123\u0124\7)\2\2\u0124\u0125\7\b\2\2\u0125\u012a\7\63\2\2\u0126"+
		"\u0127\7\36\2\2\u0127\u0129\7\63\2\2\u0128\u0126\3\2\2\2\u0129\u012c\3"+
		"\2\2\2\u012a\u0128\3\2\2\2\u012a\u012b\3\2\2\2\u012b\u012d\3\2\2\2\u012c"+
		"\u012a\3\2\2\2\u012d\u0135\7\t\2\2\u012e\u012f\7)\2\2\u012f\u0135\7\63"+
		"\2\2\u0130\u0131\7)\2\2\u0131\u0132\7\b\2\2\u0132\u0135\7\t\2\2\u0133"+
		"\u0135\7)\2\2\u0134\u0123\3\2\2\2\u0134\u012e\3\2\2\2\u0134\u0130\3\2"+
		"\2\2\u0134\u0133\3\2\2\2\u0135\63\3\2\2\2\u0136\u0137\7*\2\2\u0137\u0138"+
		"\5N(\2\u0138\65\3\2\2\2\u0139\u013b\58\35\2\u013a\u013c\7 \2\2\u013b\u013a"+
		"\3\2\2\2\u013b\u013c\3\2\2\2\u013c\u013d\3\2\2\2\u013d\u013e\5J&\2\u013e"+
		"\u013f\7\17\2\2\u013f\u0141\7\63\2\2\u0140\u0142\5B\"\2\u0141\u0140\3"+
		"\2\2\2\u0141\u0142\3\2\2\2\u0142\u0186\3\2\2\2\u0143\u0145\58\35\2\u0144"+
		"\u0146\7 \2\2\u0145\u0144\3\2\2\2\u0145\u0146\3\2\2\2\u0146\u0147\3\2"+
		"\2\2\u0147\u0148\5N(\2\u0148\u0149\5:\36\2\u0149\u0186\3\2\2\2\u014a\u014b"+
		"\58\35\2\u014b\u014c\5J&\2\u014c\u014d\5B\"\2\u014d\u0186\3\2\2\2\u014e"+
		"\u0150\58\35\2\u014f\u0151\7 \2\2\u0150\u014f\3\2\2\2\u0150\u0151\3\2"+
		"\2\2\u0151\u0152\3\2\2\2\u0152\u0153\7\63\2\2\u0153\u0154\7\37\2\2\u0154"+
		"\u0155\7\63\2\2\u0155\u0186\3\2\2\2\u0156\u0158\58\35\2\u0157\u0159\7"+
		" \2\2\u0158\u0157\3\2\2\2\u0158\u0159\3\2\2\2\u0159\u015a\3\2\2\2\u015a"+
		"\u015b\7\63\2\2\u015b\u0186\3\2\2\2\u015c\u015d\5@!\2\u015d\u015f\7\20"+
		"\2\2\u015e\u0160\7 \2\2\u015f\u015e\3\2\2\2\u015f\u0160\3\2\2\2\u0160"+
		"\u0161\3\2\2\2\u0161\u0162\7\63\2\2\u0162\u0186\3\2\2\2\u0163\u0164\5"+
		"8\35\2\u0164\u0165\7\21\2\2\u0165\u0166\7\63\2\2\u0166\u0186\3\2\2\2\u0167"+
		"\u0168\58\35\2\u0168\u0169\7\22\2\2\u0169\u016a\7\63\2\2\u016a\u0186\3"+
		"\2\2\2\u016b\u016c\58\35\2\u016c\u016d\7\5\2\2\u016d\u016e\5\60\31\2\u016e"+
		"\u016f\7\6\2\2\u016f\u0186\3\2\2\2\u0170\u0171\58\35\2\u0171\u0172\7\23"+
		"\2\2\u0172\u0173\7\5\2\2\u0173\u0174\5\60\31\2\u0174\u0176\7\6\2\2\u0175"+
		"\u0177\5<\37\2\u0176\u0175\3\2\2\2\u0177\u0178\3\2\2\2\u0178\u0176\3\2"+
		"\2\2\u0178\u0179\3\2\2\2\u0179\u0186\3\2\2\2\u017a\u017c\58\35\2\u017b"+
		"\u017d\7 \2\2\u017c\u017b\3\2\2\2\u017c\u017d\3\2\2\2\u017d\u017e\3\2"+
		"\2\2\u017e\u017f\7\24\2\2\u017f\u0181\7\63\2\2\u0180\u0182\5> \2\u0181"+
		"\u0180\3\2\2\2\u0182\u0183\3\2\2\2\u0183\u0181\3\2\2\2\u0183\u0184\3\2"+
		"\2\2\u0184\u0186\3\2\2\2\u0185\u0139\3\2\2\2\u0185\u0143\3\2\2\2\u0185"+
		"\u014a\3\2\2\2\u0185\u014e\3\2\2\2\u0185\u0156\3\2\2\2\u0185\u015c\3\2"+
		"\2\2\u0185\u0163\3\2\2\2\u0185\u0167\3\2\2\2\u0185\u016b\3\2\2\2\u0185"+
		"\u0170\3\2\2\2\u0185\u017a\3\2\2\2\u0186\67\3\2\2\2\u0187\u0188\7\63\2"+
		"\2\u0188\u0194\7\25\2\2\u0189\u018a\7\b\2\2\u018a\u018f\7\63\2\2\u018b"+
		"\u018c\7\36\2\2\u018c\u018e\7\63\2\2\u018d\u018b\3\2\2\2\u018e\u0191\3"+
		"\2\2\2\u018f\u018d\3\2\2\2\u018f\u0190\3\2\2\2\u0190\u0192\3\2\2\2\u0191"+
		"\u018f\3\2\2\2\u0192\u0194\7\t\2\2\u0193\u0187\3\2\2\2\u0193\u0189\3\2"+
		"\2\2\u01949\3\2\2\2\u0195\u0196\7\26\2\2\u0196\u019a\7\63\2\2\u0197\u019a"+
		"\7!\2\2\u0198\u019a\7\"\2\2\u0199\u0195\3\2\2\2\u0199\u0197\3\2\2\2\u0199"+
		"\u0198\3\2\2\2\u019a;\3\2\2\2\u019b\u019c\7\27\2\2\u019c\u019d\5N(\2\u019d"+
		"\u019e\7\5\2\2\u019e\u019f\5\60\31\2\u019f\u01a0\7\6\2\2\u01a0=\3\2\2"+
		"\2\u01a1\u01a2\7\30\2\2\u01a2\u01a4\7\63\2\2\u01a3\u01a5\5@!\2\u01a4\u01a3"+
		"\3\2\2\2\u01a4\u01a5\3\2\2\2\u01a5\u01a6\3\2\2\2\u01a6\u01a7\7\31\2\2"+
		"\u01a7\u01a8\7\5\2\2\u01a8\u01a9\5\60\31\2\u01a9\u01aa\7\6\2\2\u01aa?"+
		"\3\2\2\2\u01ab\u01ac\7\b\2\2\u01ac\u01b1\7\63\2\2\u01ad\u01ae\7\36\2\2"+
		"\u01ae\u01b0\7\63\2\2\u01af\u01ad\3\2\2\2\u01b0\u01b3\3\2\2\2\u01b1\u01af"+
		"\3\2\2\2\u01b1\u01b2\3\2\2\2\u01b2\u01b4\3\2\2\2\u01b3\u01b1\3\2\2\2\u01b4"+
		"\u01b5\7\t\2\2\u01b5A\3\2\2\2\u01b6\u01b7\7\b\2\2\u01b7\u01bc\7\63\2\2"+
		"\u01b8\u01b9\7\36\2\2\u01b9\u01bb\7\63\2\2\u01ba\u01b8\3\2\2\2\u01bb\u01be"+
		"\3\2\2\2\u01bc\u01ba\3\2\2\2\u01bc\u01bd\3\2\2\2\u01bd\u01bf\3\2\2\2\u01be"+
		"\u01bc\3\2\2\2\u01bf\u01c3\7\t\2\2\u01c0\u01c1\7\b\2\2\u01c1\u01c3\7\t"+
		"\2\2\u01c2\u01b6\3\2\2\2\u01c2\u01c0\3\2\2\2\u01c3C\3\2\2\2\u01c4\u01c5"+
		"\t\2\2\2\u01c5E\3\2\2\2\u01c6\u01c7\7\32\2\2\u01c7\u01cc\5H%\2\u01c8\u01c9"+
		"\7\36\2\2\u01c9\u01cb\5H%\2\u01ca\u01c8\3\2\2\2\u01cb\u01ce\3\2\2\2\u01cc"+
		"\u01ca\3\2\2\2\u01cc\u01cd\3\2\2\2\u01cd\u01cf\3\2\2\2\u01ce\u01cc\3\2"+
		"\2\2\u01cf\u01d0\7\33\2\2\u01d0G\3\2\2\2\u01d1\u01d3\7+\2\2\u01d2\u01d1"+
		"\3\2\2\2\u01d2\u01d3\3\2\2\2\u01d3\u01d7\3\2\2\2\u01d4\u01d6\5D#\2\u01d5"+
		"\u01d4\3\2\2\2\u01d6\u01d9\3\2\2\2\u01d7\u01d5\3\2\2\2\u01d7\u01d8\3\2"+
		"\2\2\u01d8\u01da\3\2\2\2\u01d9\u01d7\3\2\2\2\u01da\u01db\7\63\2\2\u01db"+
		"I\3\2\2\2\u01dc\u01de\5N(\2\u01dd\u01df\5L\'\2\u01de\u01dd\3\2\2\2\u01de"+
		"\u01df\3\2\2\2\u01dfK\3\2\2\2\u01e0\u01e1\7\32\2\2\u01e1\u01e6\5J&\2\u01e2"+
		"\u01e3\7\36\2\2\u01e3\u01e5\5J&\2\u01e4\u01e2\3\2\2\2\u01e5\u01e8\3\2"+
		"\2\2\u01e6\u01e4\3\2\2\2\u01e6\u01e7\3\2\2\2\u01e7\u01e9\3\2\2\2\u01e8"+
		"\u01e6\3\2\2\2\u01e9\u01ea\7\33\2\2\u01eaM\3\2\2\2\u01eb\u01f0\7\63\2"+
		"\2\u01ec\u01ed\7\37\2\2\u01ed\u01ef\7\63\2\2\u01ee\u01ec\3\2\2\2\u01ef"+
		"\u01f2\3\2\2\2\u01f0\u01ee\3\2\2\2\u01f0\u01f1\3\2\2\2\u01f1O\3\2\2\2"+
		"\u01f2\u01f0\3\2\2\2\67S[fnqty\177\u0082\u0085\u0089\u008e\u0094\u00a7"+
		"\u00ae\u00b6\u00c3\u00cc\u00d7\u00e2\u00e9\u00ec\u00f8\u0100\u0103\u0110"+
		"\u011c\u0121\u012a\u0134\u013b\u0141\u0145\u0150\u0158\u015f\u0178\u017c"+
		"\u0183\u0185\u018f\u0193\u0199\u01a4\u01b1\u01bc\u01c2\u01cc\u01d2\u01d7"+
		"\u01de\u01e6\u01f0";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}