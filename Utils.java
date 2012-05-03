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

import java.io.*;
import java.util.*;

class Utils
{
	///////////////////////////////////////////////////////////////////////////
	public static String getPath(String root, String path)
	  throws IOException
	{
		File f;

		if (path == null)
			return null;

		if (path.indexOf('/') == 0)
			f = new File(path);
		else if (root.lastIndexOf('/') != root.length()-1)
			f = new File(root + "/" + path);
		else
			f = new File(root + path);

		//debug(f.getCanonicalPath());
		
		return f.getCanonicalPath();
	}

	///////////////////////////////////////////////////////////////////////////
	public static int parseObject(Token[] tokens, int first, int last, Hashtable obj)
	{
		Token t = tokens[first];
		if (t.type != Token.LC)
			error("error");

		int l2 = findCloser(tokens, first);
		if (l2 > last)
			error("Expected } before line %d", tokens[last-1].lineNum);
		last = l2;

		first++;
		t = tokens[first++];

		while (first < last)
		{
			if (t.type != Token.NAME && t.type != Token.STRING)
				error("Expected name on line %d", t.lineNum);

			String name = t.value;

			t = tokens[first++];

			if (t.type != Token.COLON)
				error("Expected ':' on line %d", t.lineNum);

			t = tokens[first];
			switch (t.type)
			{
				case Token.STRING:
				case Token.TRUE:
				case Token.FALSE:
				case Token.NUMBER:
					first++;
					obj.put(name, t.value);
					break;

				case Token.LB:
					ArrayList a = new ArrayList();
					first = parseArray(tokens, first, last, a);
					obj.put(name, a);
					break;

				case Token.LC:
					Hashtable o = new Hashtable();
					first = parseObject(tokens, first, last, o);
					obj.put(name, o);
					break;

				case Token.FUNCTION:
					while (t.type != Token.LC)
						t = tokens[++first];
					first = findCloser(tokens, first);

					debug("found function in object on line " + t.lineNum);
					break;
			}

			t = tokens[first++];
			if (t.type == Token.RC)
			{
				if (first != last)
					error("Found unexpected } on line %d", t.lineNum);
				return first;
			}

			if (t.type != Token.COMMA)
				error("Expected comma on line %d", t.lineNum);

			t = tokens[first++];
		}

		return first;
	}

	/////////////////////////////////////////////////////////////////////////////
	public static int parseArray(Token[] tokens, int first, int last, ArrayList arr)
	{
		Token t = tokens[first];
		if (t.type != Token.LB)
			error("error");

		int l2 = findCloser(tokens, first);
		if (l2 > last)
			error("Expected ] before line %d", tokens[last-1].lineNum);
		last = l2;
		first++;

		while (first < last)
		{
			t = tokens[first];
			switch (t.type)
			{
				case Token.STRING:
				case Token.TRUE:
				case Token.FALSE:
					first++;
					arr.add(t.value);
					break;

				case Token.LB:
					ArrayList a = new ArrayList();
					first = parseArray(tokens, first, last, a);
					arr.add(a);
					break;

				case Token.LC:
					Hashtable o = new Hashtable();
					first = parseObject(tokens, first, last, o);
					arr.add(o);
					break;
			}

			t = tokens[first++];
			if (t.type == Token.RB)
			{
				if (first != last)
					error("Found unexpected ] on line %d", t.lineNum);
				return first;
			}

			if (t.type != Token.COMMA)
				error("Expected comma on line %d", t.lineNum);
		}

		error("] not found");
		return 0;
	}

	/////////////////////////////////////////////////////////////////////////////
	//parseValue(Token[] tokens, int first, int last, ArrayList arr)
	//{
	//}

	///////////////////////////////////////////////////////////////////////////
	public static int findCloser(Token[] tokens, int first)
	{
		int nesting   = 1;
		int searchFor = 0;
		Token opener = tokens[first++];

		if (opener.type == Token.LP)
			searchFor = Token.RP;
		else if (opener.type == Token.LC)
			searchFor = Token.RC;
		else if (opener.type == Token.LB)
			searchFor = Token.RB;
		else
			return first;

		while (first < tokens.length)
		{
			Token t = tokens[first++];
			if (t.type == searchFor)
				nesting--;
			if (t.type == opener.type)
				nesting++;

			if (nesting == 0)
				return first;
		}

		error("Unmatched %s on line %d", opener.value, opener.lineNum);
		return 0;
	}

	///////////////////////////////////////////////////////////////////////////
	public static Token[] tokenize(String src)
	{
		Tokenizer tz = new Tokenizer(src);

		Token t;

		ArrayList tokens = new ArrayList(10000);
		while ((t = tz.getToken()) != null)
			tokens.add(t);
		
		Token[] tokenArray = new Token[tokens.size()];
		for (int i = 0; i < tokens.size(); i++)
		    tokenArray[i] = (Token)tokens.get(i);

		return tokenArray;
	}

	///////////////////////////////////////////////////////////////////////////
	public static Token[] getTokens(String filename)
		throws Throwable
	{
		int len = (int)(new File(filename)).length();
		if (len == 0)
			return null;

		char[] src = new char[len];
		FileReader fr = new FileReader(filename);
		len = fr.read(src, 0, len);
		fr.close();

		// The file length (from file.length()) may not match the number
		// of characters read - the file length is in bytes, but the file
		// can use UTF-8 encoding, meaning there can be fewer characters
		// than bytes in the file.  This line re-allocates the src
		// array so it will only include file data, with no left over space.
		return tokenize(new String(src, 0, len));
	}

	///////////////////////////////////////////////////////////////////////////
	public static void message(String name, Object... args)
	{
		String msg = (String)messages.get(name);
		if (msg == null)
			msg = name;

		System.out.println(String.format(msg, args));
	}

	///////////////////////////////////////////////////////////////////////////
	public static void debug(String name, Object... args)
	{
		message(name, args);
	}

	///////////////////////////////////////////////////////////////////////////
	public static void warn(String name, Object... args)
	{
		message(name, args);
	}

	///////////////////////////////////////////////////////////////////////////
	public static void error(String name, Object... args)
	{
		message(name, args);
		System.exit(0);
	}

	///////////////////////////////////////////////////////////////////////////
	private static Hashtable messages;

	static
	{
		String[] msgs =
		{
			"Read %d tokens",
			"Unable to read config file 'compactor.cfg'"
		};

		String[] names =
		{
			"configTokens",
			"noConfigFile"
		};

		messages = new Hashtable(msgs.length);
		for (int i = 0; i < msgs.length; i++)
			messages.put(names[i], msgs[i]);
	}
}
