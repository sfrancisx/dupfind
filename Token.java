/*
Copyright (c) 2012, Yahoo! Inc.  All rights reserved.

Redistribution and use of this software in source and binary forms, 
with or without modification, are permitted provided that the following 
conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of Yahoo! Inc. nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of Yahoo! Inc.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS 
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class Token
{
    public int		type;			// token type
    public String	src;			// the token's string in the souce

    public String	value;			// The token's value (for STRING & REGEXP tokens)
    public int		intValue;		// The token's integer value (for NUMBER tokens)
    
    public int		start;			// Index of 1st character in the source
    public int		end;			// Index of 1st character after the token in the source
    public int		length;			// end - start

    public int		lineNum;		// Line number of the token
    public int		charOnLine;		// Column number of token start
    public int		lineStart;		// Index of the start of the line containing the token

    Token(int type, String string, int offset, int lineNum, int charOnLine, int lineStart)
    {
        this.type	= type;
        this.src	= string;
        
        this.start = offset - 1;
        this.length = string.length();
        this.end = this.start + this.length;
        
        if (this.type == STRING || this.type == REGEXP)
        {
            string = string.substring(1, string.length()-1);
            if (this.type == STRING)
            {
                // TODO: Handle other escaped characters
                string = string.replace("\\\'", "'");
                string = string.replace("\\\"", "\"");
                string = string.replace("\\\\", "\\");
            }
            
            this.value = string;
        }
        else
            this.value = this.src;

        this.value = this.value.intern();
        
        if (this.type == NUMBER)
        {
            try
            {
                this.intValue = (new java.lang.Integer(string)).intValue();
            }
            catch (Exception e)
            {
            }
        }
            
        this.lineNum = lineNum;
        this.charOnLine = charOnLine;
        this.lineStart = lineStart;
    }
    
//    public static final int
//            CONTEXTUAL	= 1,
//            RESERVED	= 2,
//            LITERAL		= 3;

//    public Token makeIdentifier(int reserved)
//    {
//        switch (type)
//        {
//            //case BREAK: case CASE: case CAST: case CATCH: case CLASS: case CONST:
//            //case CONTINUE: case DEBUGGER: case DEFAULT: case DELETE: case DO:
//            //case DYNAMIC: case ELSE: case FALSE: case FINAL: case FINALLY: case FOR: 
//            //case FUNCTION: case IF: case IN: case INSTANCEOF: case INTERFACE:
//            //case IS: case LET: case LIKE: case NAMESPACE: case NATIVE: case NEW:
//            //case NULL: case OVERRIDE: case RETURN: case STATIC: case SUPER:
//            //case SWITCH: case THIS: case THROW: case TRUE: case TRY: case TYPE:
//            //case TYPEOF: case USE: case VAR: case VOID: case WHILE: case WITH:
//            //case YIELD: case PROTO:
//            case BREAK: case CASE: case CATCH: 
//            case CONTINUE: case DEBUGGER: case DEFAULT: case DELETE: case DO:
//            case ELSE: case FALSE: case FINALLY: case FOR: 
//            case FUNCTION: case IF: case IN: case INSTANCEOF: 
//            case NEW:
//            case NULL: case RETURN:
//            case SWITCH: case THIS: case THROW: case TRUE: case TRY:
//            case TYPEOF: case VAR: case WHILE: case WITH:
//                if (reserved != RESERVED)
//                    return null;

////			case NAME: case EACH: case EXTENDS: case GENERATOR: case GET:
////			case IMPLEMENTS: case SET: case STANDARD: case STRICT: case UNDEFINED:
//            case NAME: 
//            case UNDEFINED:
//                return new Token(NAME, value_, offset, lineNum, charOnLine, lineStart);
            
//            case STRING:
//                if (reserved == LITERAL)
//                    return new Token(NAME, value_, offset, lineNum, charOnLine, lineStart);
            
//            default:
//                return null;
//        }
//    }
    
//    public boolean isCompoundAssignment()
//    {
//        if (type == MULASSIGN || type == DIVASSIGN || type == MODASSIGN
//            || type == ADDASSIGN || type == SUBASSIGN || type == LSHASSIGN
//            || type == URSHASSIGN || type == RSHASSIGN || type == ANDASSIGN
//            || type == XORASSIGN || type == ORASSIGN || type == LOGANDASSIGN
//            || type == LOGORASSIGN)
//                return true;
        
//        return false;
//    }
    
//    public static String getTokenString(int type)
//    {
//        switch (type)
//        {
//            case SEMI:
//                return ";";
//            case OR:
//                return "||";
//            case AND:
//                return "&&";
//            case RP:
//                return ")";
                
//            default:
//                return "(unimplemented for token " + type + ")";
//        }
//    }
    
    /*
     * 
     */
    public static Token stringToKeyword(String name, int offset, int line, int charOnLine, int lineStart)
    {
        Integer result = (Integer) keywords.get(name);
        if (result == null)
            return null;

        return new Token(result.intValue(), name, offset, line, charOnLine, lineStart);
    }
    
    public static final int
        NAME		= 1,
        NUMBER		= 2,
        STRING		= 3,

        FIRST_KEYWORD	= 20,
        
        BREAK		= 20,
        CASE		= 21,
        CONTINUE	= 22, 
        DEFAULT		= 23, 
        DELETE		= 24,
        DO			= 25, 
        ELSE		= 26, 
        FALSE		= 28, 
        FOR			= 29, 
        FUNCTION	= 30,
        IF			= 31, 
        IN			= 32, 
        NEW			= 33, 
        NULL		= 34, 
        RETURN		= 35, 
        SWITCH		= 36,
        THIS		= 37, 
        TRUE		= 38, 
        TYPEOF		= 39, 
        VAR			= 40, 
        VOID		= 41, 
        WHILE		= 42,
        WITH		= 43,

        FIRST_JS2_KEYWORD	= 50,

//		CAST		= 50,
        CATCH		= 51,
//		CLASS		= 52,
//		CONST		= 53,
        DEBUGGER	= 54,
//		DYNAMIC		= 55,
//		FINAL		= 56,
        FINALLY		= 57,
        INSTANCEOF	= 58,
//		INTERFACE	= 59,
//		IS			= 60,
//		LET			= 61,
//		LIKE		= 62,
//		NAMESPACE	= 63,
//		NATIVE		= 64,
//		OVERRIDE	= 65,
//		STATIC		= 66,
//		SUPER		= 67,
        THROW		= 68,
        TRY			= 69,
//		TYPE		= 70,
//		USE			= 71,
//		YIELD		= 72,
//		PROTO		= 73,

        // JS2 contextual keywords		
//		EACH		= 74,
//		EXTENDS		= 75,
//		GENERATOR	= 76,
//		GET			= 77,
//		IMPLEMENTS	= 78,
//		SET			= 79,
//		STANDARD	= 80,
//		STRICT		= 81,
        UNDEFINED	= 82,
        
        LAST_KEYWORD		= 99,
        
        SEMI		= 107,		// ;
        LB			= 108,		// [
        RB			= 109,		// ]
        LC			= 110,		// {
        RC			= 111,		// }
        LP			= 112,		// (
        RP			= 113,		// )
        COMMA		= 114,		// ,
        HOOK		= 115,		// ?
        COLON		= 116,		// :
        DOT			= 117,		// .
        OR			= 118,		// ||
        ORASSIGN	= 119,		// ||=
        BITOR		= 120,		// |
        XORASSIGN	= 121,
        BITXOR		= 122,
        AND			= 123,
        ANDASSIGN	= 124,
        BITAND		= 125,
        SHEQ		= 126,		// ===
        EQ			= 127,		// ==
        ASSIGN		= 128,		// =
        SHNE		= 129,		// !==
        NE			= 130,		// !=
        NOT			= 131,
        LSHASSIGN	= 132,
        LSH			= 133,		// <<
        LE			= 134,		// <=
        LT			= 135,		// <
        URSHASSIGN	= 136,
        URSH		= 137,		// >>>
        RSHASSIGN	= 138,
        RSH			= 139,		// >>
        GE			= 140,		// >=
        GT			= 141,		// >
        MULASSIGN	= 142,
        MUL			= 143,		// *
        REGEXP		= 144,
        DIVASSIGN	= 145,
        DIV			= 146,		// /
        MODASSIGN	= 147,
        MOD			= 148,		// %
        BITNOT		= 149,
        ADDASSIGN	= 150,		// +=
        INC			= 151,		// ++
        ADD			= 152,		// +
        SUBASSIGN	= 153,		// -=
        DEC			= 154,		// --
        SUB			= 155,		// -
        
        // Extensions
        BS			= 156,		// \
        COMMENT		= 157;
        
    private static java.util.Hashtable keywords;
    static
    {
        String[] strings =
        {
            // JS1 keywords
            "break", "case", "continue", "default", "delete",
            "do", "else",  "false", "for", "function",
            "if", "in", "new", "null", "return", "switch",
            "this", "true", "typeof", "var", "void", "while",
            "with",

            // JS2 keywords
            //"cast", "catch", "class", "const", "debugger", "dynamic", 
            //"final", "finally", "instanceof", "interface", "is", "let", 
            //"like", "namespace", "native", "override", "static", "super", 
            //"throw", "try", "type", "use", "yield", "__proto__",

            "catch", "debugger",
            "finally", "instanceof", 
            "throw", "try",

            // JS2 contextual keywords
            //"each", "extends", "generator", "get", "implements", "set", 
            //"standard", "strict", "undefined"
            "undefined"
        };

        int[] values =
        {
            // JS1
            BREAK, CASE, CONTINUE, DEFAULT, DELETE,
            DO, ELSE, FALSE, FOR, FUNCTION,
            IF, IN, NEW, NULL, RETURN, SWITCH,
            THIS, TRUE, TYPEOF, VAR, VOID, WHILE,
            WITH,
            
            // JS2
            //CAST, CATCH, CLASS, CONST, DEBUGGER, DYNAMIC,
            //FINAL, FINALLY, INSTANCEOF, INTERFACE, IS, LET,
            //LIKE, NAMESPACE, NATIVE, OVERRIDE, STATIC, SUPER,
            //THROW, TRY, TYPE, USE, YIELD, PROTO,
            CATCH, DEBUGGER,
            FINALLY, INSTANCEOF,
            THROW, TRY,

            // JS2 contextual keywords		
            //EACH, EXTENDS, GENERATOR, GET, IMPLEMENTS, SET,
            //STANDARD, STRICT, UNDEFINED
            UNDEFINED
        };

        keywords = new java.util.Hashtable(strings.length);
        for (int i=0; i < strings.length; i++)
            keywords.put(strings[i], new Integer(values[i]));
    }

}

