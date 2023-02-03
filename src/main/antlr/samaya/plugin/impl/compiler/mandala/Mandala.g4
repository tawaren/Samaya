// define a grammar called Simple
//todo: disallow keywords as variable names
grammar Mandala;

@header {
    package samaya.plugin.impl.compiler.mandala;
}

file: header* component*;
component: module | transaction | class_ | topInstance;
header: import_;
import_: IMPORT path wildcard
       | IMPORT path (AS name)?
       ;

wildcard: DOT '_' ;

transaction: TRANSACTIONAL? TRANSACTION name params rets? funBody;
module : system? MODULE name '{' entry=moduleEntry* '}';
class_: SYSTEM? CLASS_ name genericArgs '{' entry=classEntry* '}';
topInstance: SYSTEM? instanceDef;

moduleEntry : dataDef
            //| signatureDef  //Deprecated
            //| implementDef  //Deprecated
            | functionDef
            | instanceDef
            | typeAliasDef
            ;

classEntry : functionDef;

instanceEntry: aliasDef;

dataDef : accessibilities ext? capabilities DATA name generics=genericArgs? ctrs? #Data;
functionDef : accessibilities OVERLOADED? TRANSACTIONAL? EXTERNAL? FUNCTION name generics=genericArgs? params rets? funBody? #Function;
instanceDef : INSTANCE name generics=genericArgs? FOR compRef '{' entry=instanceEntry* '}' #Instance;
aliasDef: IMPLEMENT name generics=genericArgs? WITH baseRef;
typeAliasDef: TYPE name generics=genericArgs? EQ typeRef;

//Accessibility
accessibilities: accessibility*; //Helper rule so visitor is generated
accessibility : GLOBAL                                                                                      #Global
              | GLOBAL '(' (p+=permission) (COMMA p+=permission)* ')'                                       #Global
              | LOCAL                                                                                       #Local
              | LOCAL '(' (p+=permission) (COMMA p+=permission)* ')'                                        #Local
              | GUARDED '[' (g+=name) (COMMA g+=name)* ']'                                                  #Guarded
              | GUARDED '[' (g+=name) (COMMA g+=name)* ']' '(' (p+=permission) (COMMA p+=permission)* ')'   #Guarded
              ;

permission : CREATE | CONSUME | INSPECT | CALL | DEFINE ;

capabilities: ( PRIMITIVE | substructural | scoped | persistancy | volatility )*;

//todo: add the caps directly and allow them instead of the compinators
//  check needs to become better Copy + Drop = io
substructural: STANDARD | RELEVANT | AFFINE | LINEAR;
scoped:  BOUNDED | UNBOUNDED;
persistancy: PERSISTED | TEMPORARY;
volatility: VALUE | VOITAILE;

//Generics
genericArgs : '[' (generics+=genericDef) (COMMA generics+=genericDef)* ']';
genericDef : PHANTOM? capabilities name;

//Data
ext: EXTERNAL '(' size=NUM ')';
system : SYSTEM '(' id=NUM ')'
       | SYSTEM
       ;

//Todo: use | syntax like merge
ctrs: '{' (c+=ctr)* '}'
    | fields
    | '(' ')'
    ;
ctr: name fields? ;

fields: '(' (f+=field) (COMMA f+=field)* ')' ;
field: name ':' typeRef ;

//Function and Implement and Signature
params: '(' (p+=param) (COMMA p+=param)* ')'
       | '(' ')'
       ;

param: IMPLICIT? CONTEXT? CONSUME? name ':' typeRef
     | IMPLICIT? CONSUME? typeRef patterns
     ;

rets: ':' '(' (r+=ret) (COMMA r+=ret)* ')'
    | ':' r+=ret
    | ':' '(' ')'
    ;
ret: (name ':')? typeRef ;

//Code: Function and Implement
funBody: '{' tailExp '}'
       | EQ tailExp
       ;

// todo: have a bind as let short form:
//       exp |> (...) => exp
//         we can even skip (...) and autobind to $0,$1, $2, ... or it if only 1
//       same as: Let (...) = exp in exp
//todo: have a record return
//      (name:exp, test:exp)

tailExp: args            #Return
       | prio8           #ArgTailExp
       ;

argExp: prio8 ;

prio8: op1=prio8 OR op2=prio7                         #OrExp
     | prio7                                          #Prio7Exp
     ;

prio7: op1=prio7 XOR op2=prio6                        #XorExp
     | prio6                                          #Prio6Exp
     ;

prio6: op1=prio6 AMP op2=prio5                        #AndExp
     | prio5                                          #Prio5Exp
     ;

prio5: op1=prio5 (EQ EQ | BANG EQ) op2=prio4          #EQExp
     | prio4                                          #Prio4Exp
     ;

prio4: op1=prio4 (LT | LT EQ | GT | GT EQ) op2=prio3  #CmpExp
     | prio3                                          #Prio3Exp
     ;

prio3: op1=prio3 (ADD | SUB) op2=prio2                #AddSubExp
     | prio2                                          #Prio2Exp
     ;
prio2: op1=prio2 (MUL | DIV | MOD) op2=prio1          #MulDivExp
     | prio1                                          #Prio1Exp
     ;

prio1: (BANG | INV ) op1=prio1                        #UnExp
     | prio0                                          #Prio0Exp
     ;

prio0: exp
     | '(' prio8 ')'
     ;

exp: lit                                                #Literal
    | LET topPatterns EQ bind=tailExp IN exec=tailExp   #Let
    | ENSURE argExp IN exec=tailExp                     #Ensure
    | MATCH argExp WITH branches                        #Switch
    | INSPECT argExp WITH branches                      #Inspect
    | IF argExp THEN casT=tailExp ELSE casF=tailExp     #IfThenElse
    | TRY baseRef tryArgs WITH OR? succ OR fail         #TryInvoke
    | TRY baseRef tryArgs WITH OR? fail OR succ         #TryInvoke
    //Todo: can we have operator for that
    | PROJECT '(' argExp ')'                            #Project
    | PROJECT argExp                                    #Project
    | '<' argExp '>'                                    #Project
    | UNPROJECT '(' argExp ')'                          #Unproject
    | UNPROJECT argExp                                  #Unproject
    | '>' argExp '<'                                    #Unproject

    //These two would be ambigous if we make the # optional
    | typeRef '#' (ctrName=name)? args?             #Pack
    | baseRef args                                  #Invoke
    | ROLLBACK typeRefArgs?                         #Rollback
    | name                                          #Symbol
    //Todo: why not argExp for the next two?
    | exp typeHint                                  #Typed
    | exp DOT name                                  #Get
    ;


//Todo: have special encodings codings for the types
//      still allow explicit type
//      allow inferenz from surounding??

//Todo: allow _ to indivate drop

topPatterns: binding
            | patterns
            ;

patterns: '(' (p+=binding) (COMMA p+=binding)* ')'
        | '(' ')'
         ;

binding: CONTEXT? name typeHint?
     | '_' typeHint?
     | typeRef patterns
     ;

typeHint: ':' typeRef;

lit: HEX    #Hex
   | NUM    #Number
   ;

branches: OR? (b+=branch) (OR b+=branch)*;

branch: name patterns? '=>' tailExp
      | name patterns? '{' tailExp '}'
      ;
succ: SUCCESS patterns? '=>' tailExp
    | SUCCESS patterns? '{' tailExp '}'
    ;
fail: FAILURE patterns? '=>' tailExp
    | FAILURE patterns? '{' tailExp '}'
    ;

args : '(' (a+=argExp) (COMMA a+=argExp)* ')'
     | '(' ')'
     ;

tryArgs : '(' (a+=dolExp) (COMMA a+=dolExp)* ')'
     | '(' ')'
     ;
dolExp : DOL? argExp;

typeRef : baseRef
        | PROJECT '(' typeRef ')'
        | '<' typeRef '>'
        | QUEST
        ;
baseRef : path targs=typeRefArgs?
        | path compArgs=typeRefArgs DOT name targs=typeRefArgs?
        ;

compRef : path targs=typeRefArgs?;

typeRefArgs : '[' (targs+=typeRef) (COMMA targs+=typeRef)* ']'
            | '[' ']';

path : (part+=name) (DOT part+=name)*;

name : TICK? RAW_ID | TICK keywords; //Todo: can we make the ' optional in keywords as well?

keywords : id=TOP | id=TRANSACTIONAL | id=FOR | id=MODULE | id=TRANSACTION | id=DATA | id=FUNCTION | id=SIGNATURE | id=IMPLEMENT
         | id=EXTERNAL | id=SYSTEM | id=LET | id=IN | id=RETURN | id=ROLLBACK | id=MATCH | id=WITH | id=TRY | id=PHANTOM | id=DROP
         | id=COPY | id=PERSIST | id=PRIMITIVE | id=VALUE | id=UNBOUND | id=INSPECT | id=CONSUME | id=IMPLICIT | id=CALL | id=DEFINE
         | id=PROJECT | id=UNPROJECT | id=CASE | id=IMPORT | id=SUCCESS | id=FAILURE | id=CREATE | id=GLOBAL | id=LOCAL | id=GUARDED
         | id=VOITAILE | id=STANDARD | id=RELEVANT | id=AFFINE | id=LINEAR | id=PERSISTED | id=TEMPORARY | id=BOUNDED | id=UNBOUNDED
         | id=AS | id=TYPE | id=OVERLOADED | id=IF | id=THEN | id=ELSE | id=ENSURE
         ;

BLOCK_COMMENT : '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN) ;
LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN) ;
COMMA: ',';
DOT: '.';
AMP: '&';
BANG: '!';
INV: '~';
QUEST: '?';
DOL: '$';
OR: '|';
XOR: '^';
TICK: '`';
MUL: '*';
UNDIV: '\\';
DIV: '/';
MOD: '%';
ADD: '+';
SUB: '-';
EQ: '=';
LT: '<';
GT: '>';

HEX : '0x' HEXCHAR* ;
NUM : SIGN? INT;

TOP: 'top';
TRANSACTIONAL: 'transactional';
OVERLOADED: 'overloaded';
FOR: 'for';


MODULE : 'module';
TRANSACTION: 'transaction';
CLASS_ : 'class';
INSTANCE : 'instance';
DATA : 'data';
TYPE : 'type';
FUNCTION : 'function';
SIGNATURE: 'signature';
IMPLEMENT: 'implement';

EXTERNAL: 'external';
SYSTEM: 'system';

LET: 'let';
IN: 'in';
RETURN: 'return';
ROLLBACK: 'rollback';
MATCH: 'match';
WITH: 'with';
ENSURE: 'ensure';

TRY: 'try';
PHANTOM : 'phantom';
DROP: 'drop';
COPY: 'copy';
PERSIST: 'persist';
PRIMITIVE: 'primitive';
VALUE: 'value';
UNBOUND: 'unbound';
INSPECT: 'inspect';
CONSUME: 'consume';
IMPLICIT: 'implicit';
CONTEXT: 'context';
CALL: 'call';
DEFINE: 'define';
PROJECT: 'project';
UNPROJECT: 'unproject';
CASE: 'case';
IMPORT: 'import';
USE: 'use';
AS: 'as';
IF: 'if';
THEN: 'then';
ELSE: 'else';
SUCCESS: 'success';
FAILURE: 'failure';
CREATE: 'create';
GLOBAL: 'global';
LOCAL: 'local';
GUARDED: 'guarded';
VOITAILE: 'voitaile';
STANDARD: 'standard';
RELEVANT: 'relevant';
AFFINE: 'affine';
LINEAR: 'linear';
PERSISTED: 'persisted';
TEMPORARY: 'temporary';
BOUNDED: 'bounded';
UNBOUNDED: 'unbounded';

RAW_ID  : ALPHA (ALPHA | DIGIT)* ;

WS  : [ \t\r\n]+ -> skip ;

fragment HEXCHAR: [0-9A-Fa-f] ;
fragment BINCHAR: [0-1] ;
fragment DIGIT: [0-9] ;
fragment ALPHA: [_a-zA-Z];
fragment SIGN : '+' | '-';
fragment INT : DIGIT+ ;
