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
public class Tokenizer
{
    public int getOffset() { return in.offset; }

    void init(char[] in, boolean extended, boolean getRegex)
    {
        this.in = new TokenizeReader(in);
        this.extended = extended;
        this.getRegex = getRegex;

        if (getRegex)
            flags = TSF_REGEXP;
    }
    
    public Tokenizer(char[] in, boolean extended, boolean getRegex)
    {
        init(in, extended, getRegex);
    }
    
    public Tokenizer(char[] in)
    {
        init(in, false, false);
    }
    
    public Tokenizer(String in)
    {
        init(in.toCharArray(), false, false);
    }
    
    public Tokenizer(String in, boolean extended, boolean getRegex)
    {
        init(in.toCharArray(), extended, getRegex);
    }
    
    public int TSF_REGEXP	= 1;
    public int flags = 0;//TSF_REGEXP;
    public boolean extended;
    public boolean getRegex;
    
    Token lastToken;
    
    public Token peekToken()
    {
        if (lastToken == null)
            lastToken = getToken();

        return lastToken;
    }

    public Token nextToken()
    {
        Token next = peekToken();
        lastToken = null;
        return next;
    }

    /*
     * 
     */
    public Token getToken()
    {
        Token t = internalGetToken();
        if (t == null)
            return t;

        if (!getRegex &&
                  ((t.type == Token.NAME)
                || (t.type == Token.NUMBER)
                || (t.type == Token.RB)
                || (t.type == Token.RP)
                || (t.type == Token.DOT)))
            flags = 0;
        else
            flags = TSF_REGEXP;
            
        return t;
    }
    
    private int tokenStart;
    private int charOnLineStart;
    
    private Token internalGetToken()
    {
        int c;

        do
        {
            in.mark(0);
            c = in.read();
            tokenStart = in.offset;
            charOnLineStart = in.charOnLine;
        } while (isJSSpace(c) || (c == '\n'));
        
        if (c == EOF_CHAR)
            return null;

        int lineNum = in.line;
        
        // Identifier?
        if (Character.isJavaIdentifierStart((char)c))
        {
            while (Character.isJavaIdentifierPart((char)in.read()))
                ;
            in.unread();

            String str = in.getString(0);
            Token result;
            if ((result = Token.stringToKeyword(str, tokenStart, in.line, charOnLineStart, in.lineStart)) != null)
                return result;
            return createToken(Token.NAME, str, lineNum);
        }

        // Number?
        if (isDigit(c) || (c == '.' && isDigit(in.peek())))
        {
            int base = 10;

            if (c == '0')
            {
                c = in.read();
                if (c == 'x' || c == 'X')
                {
                    c = in.read();
                    base = 16;
                }
            }

            while (isXDigit(c))
            {
                if (base < 16 && isAlpha(c))
                    break;

                c = in.read();
            }

            if (base == 10 && (c == '.' || c == 'e' || c == 'E'))
            {
                if (c == '.')
                {
                    do
                    {
                        c = in.read();
                    } while (isDigit(c));
                }

                if (c == 'e' || c == 'E')
                {
                    c = in.read();
                    if (c == '+' || c == '-')
                    {
                        c = in.read();
                    }

                    if (!isDigit(c))
                        return createToken(ERROR, "Missing exponent: " + in.getString(0), lineNum);

                    do
                    {
                        c = in.read();
                    } while (isDigit(c));
                }
            }
            
            in.unread();
            return createToken(Token.NUMBER, in.getString(0), lineNum);
        }
        
        // String?
        if (c == '"' || c == '\'')
        {
            int q = c;
            
            c = in.read();

            while (c != q)
            {
                if (c == '\n' || c == EOF_CHAR)
                {
                    in.unread();
                    return createToken(ERROR, "Unterminated string literal: " + in.getString(0), lineNum);
                }
                
                if (c == '\\')
                    in.read();
                
                c = in.read();
            }

            return createToken(Token.STRING, in.getString(0), lineNum);
        }
        
        int t = getTokenType(c);
        switch (t)
        {
            case RETRY:
                return getToken();
                
            default:
                return createToken(t, in.getString(0), lineNum);
        }
    }
    
    private Token createToken(int t, String v, int lineNum)
    {
        return new Token(t, v, tokenStart, lineNum, charOnLineStart, in.lineStart);
    }

    private int getTokenType(int c)
    {
        switch (c)
        {
            case ';':	return Token.SEMI;
            case '[':	return Token.LB;
            case ']':	return Token.RB;
            case '{':	return Token.LC;
            case '}':	return Token.RC;
            case '(':	return Token.LP;
            case ')':	return Token.RP;
            case ',':	return Token.COMMA;
            case '?':	return Token.HOOK;


            case '.':
                return Token.DOT;

            case ':':
                return Token.COLON;

            case '|':
                if (in.match('|'))
                    return Token.OR;
                if (in.match('='))
                    return Token.ORASSIGN;
                return Token.BITOR;

            case '^':
                if (in.match('='))
                    return Token.XORASSIGN;
                return Token.BITXOR;

            case '&':
                if (in.match('&'))
                    return Token.AND;
                if (in.match('='))
                    return Token.ANDASSIGN;
                return Token.BITAND;

            case '=':
                if (in.match('='))
                {
                    if (in.match('='))
                        return Token.SHEQ;
                    return Token.EQ;
                }
                
                return Token.ASSIGN;

            case '!':
                if (in.match('='))
                {
                    if (in.match('='))
                        return Token.SHNE;
                    return Token.NE;
                }

                return Token.NOT;

            case '<':
                if (in.match('<'))
                {
                    if (in.match('='))
                        return Token.LSHASSIGN;

                    return Token.LSH;
                }

                if (in.match('='))
                    return Token.LE;

                return Token.LT;

            case '>':
                if (in.match('>'))
                {
                    if (in.match('>'))
                    {
                        if (in.match('='))
                            return Token.URSHASSIGN;

                        return Token.URSH;
                    }

                    if (in.match('='))
                        return Token.RSHASSIGN;

                    return Token.RSH;
                }

                if (in.match('='))
                    return Token.GE;

                return Token.GT;

            case '*':
                if (in.match('='))
                    return Token.MULASSIGN;

                return Token.MUL;

            case '/':
                in.mark(1);
                
                // is it a // comment?
                if (in.match('/'))
                {
                    /* skip to end of line */
                    while ((c = in.read()) != EOF_CHAR && c != '\n')
                        ;

                    if (c != EOF_CHAR)
                        in.unread();

                    if (extended)
                        return Token.COMMENT;//System.out.println("Comment: " + in.getString(0));
                    return RETRY;
                }
                
                // Is it a /* comment?
                if (in.match('*'))
                {
                    while ((c = in.read()) != -1 && !(c == '*' && in.match('/')))
                    {
                        if (c == '/' && in.match('*'))
                        {
                            if (in.match('/'))
                            {
                                if (extended)
                                    return Token.COMMENT;//System.out.println("Comment: " + in.getString(0));
                                return RETRY;
                            }

                            return ERR_NESTED_COMMENT;
                        }
                    }

                    if (c == EOF_CHAR)
                        return ERR_UNTERMINATED_COMMENT;

                    if (extended)
                        return Token.COMMENT;//System.out.println("Comment: " + in.getString(0));
                    return RETRY;
                }

                // is it a regexp?
                if ((flags & TSF_REGEXP) != 0)
                {
                    while ((c = in.read()) != '/')
                    {
                        if (c == '\n' || c == EOF_CHAR)
                        {
                            in.unread();
                            return ERR_UNTERMINATED_REGEXP;
                        }

                        if (c == '\\')
                            in.read();
                    }

                    while (true)
                    {
                        if (!in.match('g') && !in.match('i') && !in.match('m'))
                            break;
                    }

                    if (isAlpha(in.peek()))
                        return ERR_INVALID_REGEXP_FLAG;

                    return Token.REGEXP;
                }

                if (in.match('='))
                    return Token.DIVASSIGN;

                return Token.DIV;

            case '%':
                if (in.match('='))
                    return Token.MODASSIGN;
                return Token.MOD;

            case '~':
                return Token.BITNOT;

            case '+':
                if (in.match('='))
                    return Token.ADDASSIGN;
                if (in.match('+'))
                    return Token.INC;
                return Token.ADD;

            case '-':
                if (in.match('='))
                    return Token.SUBASSIGN;
                if (in.match('-'))
                    return Token.DEC;
                return Token.SUB;

            case '\\':
                if (extended)
                    return Token.BS;
                break;
        }

        return ERR_UNKNOWN_TOKEN;
    }
    

    /*
     * 
     */
    private static boolean isJSIdentifier(String s)
    {
        int length = s.length();

        if (length == 0 || !Character.isJavaIdentifierStart(s.charAt(0)))
            return false;

        for (int i=1; i<length; i++)
        {
            char c = s.charAt(i);
            if (!Character.isJavaIdentifierPart(c))
            {
                if (c == '\\')
                {
                    if (! ((i + 5) < length)
                      && (s.charAt(i + 1) == 'u')
                      && isXDigit(s.charAt(i + 2))
                      && isXDigit(s.charAt(i + 3))
                      && isXDigit(s.charAt(i + 4))
                      && isXDigit(s.charAt(i + 5)))
                        return false;
                }
            }
        }
        
        return true;
    }
    
    private static boolean isAlpha(int c)
    {
        return ((c >= 'a' && c <= 'z')
             || (c >= 'A' && c <= 'Z'));
    }

    static boolean isDigit(int c)
    {
        return (c >= '0' && c <= '9');
    }

    static boolean isXDigit(int c)
    {
        return ((c >= '0' && c <= '9')
             || (c >= 'a' && c <= 'f')
             || (c >= 'A' && c <= 'F'));
    }

    private static boolean isJSSpace(int c)
    {
        return (c == '\u0020' || c == '\u0009'
             || c == '\u000C' || c == '\u000B'
             || c == '\u00A0' 
             || Character.getType((char)c) == Character.SPACE_SEPARATOR);
    }

    private static boolean isJSLineTerminator(int c)
    {
        return (c == '\n' || c == '\r'
             || c == 0x2028 || c == 0x2029);
    }
    
    private TokenizeReader		in;
    private final static int	EOF_CHAR = -1;

    private static final int
        ERROR						= -1,
        EOF							= 0,
        RETRY						= 500,
        ERR_UNTERM_COMMENT			= 501,
        ERR_NESTED_COMMENT			= 502,
        ERR_UNTERMINATED_COMMENT	= 503,
        ERR_UNTERMINATED_REGEXP		= 504,
        ERR_INVALID_REGEXP_FLAG		= 505,
        ERR_UNKNOWN_TOKEN			= 506;
}

/*
 * 
 */
class TokenizeReader
{
    public TokenizeReader(char[] in)
    {
        this.in		= in;
        offset		= 0;
        
        line		= 1;
        charOnLine	= 0;
        lineStart	= 0;
    }

    // Read the next character.  For \r\n, return just \n.
    public int read()
    {
        // mark 19 is used for unread
        mark(19);

        if (offset == in.length)
            return -1;
            
        if (in[offset] == '\r')
        {
            if ((offset + 1) < in.length)
            {
                if (in[offset + 1] == '\n')
                    offset++;
            }
        }
        
        if (in[offset] == '\n')
        {
            line++;
            charOnLine = 0;
            
            offset++;
            lineStart = offset;
            
            return '\n';
        }
        
        if (in[offset] == '\t')
        {
            charOnLine = charOnLine/4;
            charOnLine++;
            charOnLine = charOnLine*4;
//			charOnLine += 4;
        }
        else
            charOnLine++;
            
        return in[offset++];
    }

    public void unread()
    {
        reset(19);
    }
    
    public boolean match(char c)
    {
        if (offset == in.length)
            return false;
        
        if (in[offset] == c)
        {
            offset++;
            charOnLine++;
            return true;
        }
        
        return false;
    }
    
    // Peek at the next character, without changing anything
    public int peek()
    {
        if (offset == in.length)
            return -1;
        return in[offset];
    }
    
    public void mark(int num)
    {
        markOffset[num] = offset;
        markLine[num] = line;
        markCharOnLine[num] = charOnLine;
        markLineStart[num] = lineStart;
    }
    
    public void reset(int num)
    {
        offset = markOffset[num];
        line = markLine[num];
        charOnLine = markCharOnLine[num];
        lineStart = markLineStart[num];
    }
    
    public String getString(int num)
    {
        return new String(in, markOffset[num], offset - markOffset[num]);
    }

    // The buffer we're reading from
    public	char[]	in;
    public	int		offset;
//	private int		prevOffset;

    // Keep track of where we are for error reporting
    public  int		line;			// line number
    public  int		charOnLine;
    public  int		lineStart;		// start of current line

    // mark offsets.  I only use 2, but it's cheap to allocate a bunch
    private int[]	markOffset = new int[20];
    private int[]	markLine = new int[20];
    private int[]	markCharOnLine = new int[20];
    private int[]	markLineStart = new int[20];
}
