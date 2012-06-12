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

class DupFind
{
	///////////////////////////////////////////////////////////////////////////
	public static void main(String[] args)
		throws IOException, Throwable
	{
		int i,
			totalTokens = 0;

		// Read the config file
		Config cfg = readConfigFile();

		// args.length != 0 --> Only process sources/files from the command line
		if (args.length != 0)
		{
			for (i = 0; i < cfg.sources.length; i++)
				cfg.sources[i].def = false;

			for (i = 0; i < args.length; i++)
			{
				boolean found = false;
				for (int j = 0; j < cfg.sources.length; j++)
				{
					String name = cfg.sources[j].name;
					if (name != null && name.compareTo(args[i]) == 0)
					{
						found = true;
						cfg.sources[j].def = true;
					}
				}

				if (!found)
					cfg.addSource(args[i]);
			}

		}

		String info = "";
		for (i = 0; i < cfg.sources.length; i++)
		{
			if (cfg.sources[i].def)
			{
				if (info.length() > 0)
					info += ", ";
				info += cfg.sources[i].name;
			}
		}

		if (cfg.cpd)
			System.out.println("<?xml version=\"1.0\" encoding=\"MacRoman\"?>\n<pmd-cpd>");
		else
			System.out.println("Reading source from " + info);

		Hashtable paths = new Hashtable();

		// Read all the sources
		for (i = 0; i < cfg.sources.length; i++)
		{
			if (cfg.sources[i].def)
				totalTokens += readFiles(paths, cfg.sources[i]);
		}

		int totalMatches = 0;
		int numToCheck = cfg.max;
		while (numToCheck >= cfg.min)
		{
			totalMatches += checkForDuplicatedCodeSegments(paths, numToCheck, cfg.fuzzy, cfg.cpd);
			numToCheck -= cfg.increment;
		}

		int matchedTokens = 0;
		
		ArrayList a = new ArrayList();
		Enumeration e = paths.elements();
		JsFile f = null;
		while (e.hasMoreElements())
		{
			f = (JsFile)e.nextElement();
			a.add(f);
		}

		if (cfg.cpd)
			System.out.println("    <summary>\n        <files>");
		else
			System.out.println("\n\nDuplicated tokens by file:\n--------------------------");

		Collections.sort(a, f);
		for (i = 0; i < a.size(); i++)
		{
			f = (JsFile)a.get(i);

			matchedTokens += f.duplicated;
			if (f.duplicated > 0)
			{
				if (cfg.cpd)
					System.out.println("            <filesummary file=\"" + f.path + "\" tokens=\"" + f.duplicated + "\"/>");
				else
					System.out.println(f.duplicated + ":" + f.path);
			}
		}

		if (cfg.cpd)
			System.out.println("        </files>");

		float perc = (int)(matchedTokens * 1000 / totalTokens);
		perc /= 10;

		if (cfg.cpd)
		{
			System.out.println("        <overallsummary segments=\"" + totalMatches + 
								"\" tokens=\"" + matchedTokens + "\" totaltokens=\"" + totalTokens + "\" percentage=\"" + perc + "\"/>");
			System.out.println("    </summary>\n</pmd-cpd>");
		}
		else
			System.out.println("\nFound " + totalMatches + " duplicated segments.  " 
								+ matchedTokens + " duplicated tokens out of " + totalTokens + " total (" + perc + "%).");
	}

	///////////////////////////////////////////////////////////////////////////
	private static int checkForDuplicatedCodeSegments(Hashtable paths, int tokensToMatch, boolean fuzzy, boolean cpd)
	{
		Hashtable segments = new Hashtable();

		int i;

 		// Enumerate over all files, computing a hash for every segment of 'tokensToMatch' consecutive tokens
		Enumeration e = paths.elements();
		while (e.hasMoreElements())
		{
			JsFile m = (JsFile)e.nextElement();

			Token[] tokens = m.tokens;

			int tp = 1;
			int ts = 0;
			if (tokens.length < tokensToMatch)
				continue;

			for (i = 0; i < tokensToMatch-1; i++)
			{
				tp *= tokens[i].type;		// tp => token product.  The product of all token types
				ts += tokens[i].type;		// ts => token sum.  The sum of all token types
			}

			do
			{
				ts += tokens[i].type;
				tp *= tokens[i].type;

				CodeSegment cs = new CodeSegment(m, i+1-tokensToMatch, ts, tp);

				ArrayList a = (ArrayList)segments.get(ts*tp + ts + tp);
				if (a == null)
				{
					a = new ArrayList();
					segments.put(ts*tp + ts + tp, a);
				}
				a.add(cs);

				i++;

				ts -= tokens[i-tokensToMatch].type;
				tp /= tokens[i-tokensToMatch].type;

			} while (i < tokens.length);
		}

		int totalMatches = 0;
		e = segments.elements();
		while (e.hasMoreElements())
		{
			ArrayList a = (ArrayList)e.nextElement();

			//Utils.debug("Completed " + count + " of " + segments.size() + ".  Next length = " + a.size() + ".  Matches so far: " + totalMatches);

			for (i = 0; i < a.size()-1; i++)
			{
				CodeSegment cs1 = (CodeSegment)a.get(i);
				if (cs1.foundDup)
					continue;

				boolean reported = false;
				for (int j = 0; j < tokensToMatch; j++)
				{
					if (cs1.info.reported[cs1.offset + j])
					{
						reported = true;
						break;
					}
				}
				if (reported)
					continue;

				Token[] t1 = cs1.info.tokens;
				String dup = "";
				int count = 0;

				for (int j = i+1; j < a.size(); j++)
				{
					CodeSegment cs2 = (CodeSegment)a.get(j);

					if (cs1.tokenSum != cs2.tokenSum || cs1.tokenProduct != cs2.tokenProduct)
						continue;

					Token[] t2 = cs2.info.tokens;

					boolean match = true;
					boolean prevDot = false;
					for (int k = 0; k < tokensToMatch; k++)
					{
						if (cs2.info.reported[cs2.offset+k])
						{
							match = false;
							break;
						}

						Token token1 = t1[cs1.offset+k];
						Token token2 = t2[cs2.offset+k];

						if (token1.type != token2.type)
						{
							match = false;
							break;
						}

						if (fuzzy)
						{
							if (!prevDot && token1.type == Token.NAME)
								continue;
						}

						if (token1.src.compareTo(token2.src) != 0)
						{
							match = false;
							break;
						}

						prevDot = false;
						if (token1.type == Token.DOT)
							prevDot = true;
					}

					if (match)
					{
						if (!cs1.foundDup)
						{
							cs1.foundDup = true;
							String code = dumpCode(cs1, tokensToMatch, count == 0, cpd);
							count++;
							dup = code + "\n" + dup;
						}

						cs2.foundDup = true;
						String code = dumpCode(cs2, tokensToMatch, count == 0, cpd);
						count++;
						dup = code + "\n" + dup;
					}
				}

				if (count > 1)
				{
					totalMatches += count;
					if (cpd)
					{
						System.out.print("    <duplication lines=\"" + numLines + "\" tokens=\"" + tokensToMatch + "\">\n");
						System.out.print(dup);
						System.out.print("    </duplication>\n");
					}
					else
						System.out.print("-------- " + tokensToMatch + " tokens --------\n" + dup);
				}
			}
		}

		return totalMatches;
	}

	///////////////////////////////////////////////////////////////////////////
	private static String dumpCode(CodeSegment cs, int len, boolean includeSrc, boolean cpd)
	{
		String text;

		JsFile f = cs.info;

		f.duplicated += len;
		
		String src = "";
		for (int i = 0; i < len; i++)
		{
			f.reported[cs.offset + i] = true;

			src += f.tokens[cs.offset + i].src;
			int type = f.tokens[cs.offset + i].type;
			int nextType = -1;
			if (cs.offset+i+1 < f.tokens.length)
				nextType = f.tokens[cs.offset + i + 1].type;

			if (type == Token.FUNCTION || type == Token.RETURN || type == Token.VAR ||
			    type == Token.CASE || type == Token.NEW || (type == nextType))
				src += " ";
		}

		numLines = f.tokens[cs.offset + len - 1].lineNum - f.tokens[cs.offset].lineNum + 1;

		if (cpd)
		{
			text = "        <file line=\"" + f.tokens[cs.offset].lineNum + "\" path=\"" + f.path + "\"/>";
			src  = "        <codefragment>\n            " + src.replace("<", "&lt;") + "\n        </codefragment>";
		}
		else
			text = f.path + ", line " + f.tokens[cs.offset].lineNum;

		if (includeSrc)
			return text + "\n" + src;

		return text;
	}


	///////////////////////////////////////////////////////////////////////////
	private static int readFiles(Hashtable paths, Source s)
	  throws Throwable
	{
		int totalTokens = 0;

		if (s.directories.length == 0)
			totalTokens = readFilesFromDir(paths, s.root, s);
		else
		{
			for (int i = 0; i < s.directories.length; i++)
				totalTokens += readFilesFromDir(paths, Utils.getPath(s.root, s.directories[i]), s);
		}

		return totalTokens;
	}

	///////////////////////////////////////////////////////////////////////////
	private static int readFilesFromDir(Hashtable paths, String dir, Source s)
	  throws Throwable
	{
		int totalTokens = 0;

		File f = new File(dir);

		java.util.regex.Pattern[] include = s.include;
		java.util.regex.Pattern[] exclude = s.exclude;

		for (int i = 0; i < exclude.length; i++)
		{
			java.util.regex.Matcher m = exclude[i].matcher(f.getCanonicalPath());
			if (m.matches())
			{
				//System.out.println("Excluding " + dir);
				return totalTokens;
			}
		}

		if (f.isDirectory())
		{
			String[] files = f.list();
			if (files != null)
			{
				for (int i = 0; i < files.length; i++)
					totalTokens += readFilesFromDir(paths, Utils.getPath(dir, files[i]), s);
			}
		}
		else
		{
			if (include != null)
			{
				boolean includeFile = false;
				for (int i = 0; i < include.length; i++)
				{
					java.util.regex.Matcher m = include[i].matcher(f.getName());
					if (m.matches())
					{
						includeFile = true;
						break;
					}
				}

				if (!includeFile)
				{
					//System.out.println("Not including file " + f.getName());
					return totalTokens;
				}

				//System.out.println("Including " + f.getName());
			}

			dir = f.getCanonicalPath();

			if (paths.get(dir) == null)
				totalTokens += readFile(dir, paths);
			else
				System.out.println("File specified twice: " + dir);
		}

		return totalTokens;
	}

	///////////////////////////////////////////////////////////////////////////
	private static int readFile(String name, Hashtable paths)
	  throws Throwable
	{
		Token[] tokens = Utils.getTokens(name);
		if (tokens == null)
			return 0;

		JsFile jsfile = new JsFile(name, tokens);
		paths.put(name, jsfile);

		return tokens.length;
	}

	///////////////////////////////////////////////////////////////////////////
	private static Config readConfigFile()
		throws Throwable
	{
		// Read the config file into a generic representation
		Token[] tokens = Utils.getTokens("dupfind.cfg");
		if (tokens == null)
			tokens = Utils.getTokens("~/.dupfind.cfg");
		if (tokens == null)
			return new Config();

		Hashtable obj = new Hashtable();
		Utils.parseObject(tokens, 0, tokens.length, obj);

		// Put the generic representation into something easier to use
		return new Config(obj);
	}

	private static int numLines;
}

///////////////////////////////////////////////////////////////////////////
class JsFile
  implements Comparator
{
	public JsFile(String path, Token[] tokens)
	{
		this.path = path;
		this.tokens = tokens;

		reported = new boolean[tokens.length];

		duplicated = 0;
	}

	public int compare(Object o1, Object o2)
	{
		return (((JsFile)o2).duplicated - ((JsFile)o1).duplicated);
	}

	public String		path;
	public Token[]		tokens;
	public boolean[]	reported;

	public int			duplicated;
}

///////////////////////////////////////////////////////////////////////////
class CodeSegment
{
	public CodeSegment(JsFile file, int offset, int tokenSum, int tokenProduct)
	{
		this.info = file;
		this.offset = offset;
		this.tokenSum = tokenSum;
		this.tokenProduct = tokenProduct;
	}

	public JsFile info;
	public int offset;
	public int tokenSum;
	public int tokenProduct;
	public boolean foundDup = false;
}

///////////////////////////////////////////////////////////////////////////
class Config
{
	public Config(Hashtable obj)
	{
		init();
				
		String s = (String)obj.get("min");
		if (s != null)
			min = Integer.decode(s);

		s = (String)obj.get("max");
		if (s != null)
			max = Integer.decode(s);

		s = (String)obj.get("increment");
		if (s != null)
			increment = Integer.decode(s);

		s = (String)obj.get("fuzzy");
		if (s == null || s.compareTo("0") == 0 || s.compareTo("false") == 0)
			fuzzy = false;
		else
			fuzzy = true;

		s = (String)obj.get("cpd");
		if (s == null || s.compareTo("0") == 0 || s.compareTo("false") == 0)
			cpd = false;
		else
			cpd = true;

		ArrayList sources = (ArrayList)obj.get("sources");

		this.sources = new Source[sources.size()];

		boolean anyDefault = false;

		for (int i = 0; i < this.sources.length; i++)
		{
			this.sources[i] = new Source((Hashtable)sources.get(i));
			anyDefault |= this.sources[i].def;
		}

		if (!anyDefault)
		{
			for (int i = 0; i < this.sources.length; i++)
				this.sources[i].def = true;
		}
	}

	public Config()
	{
		init();

		sources = new Source[1];
		sources[0] = new Source();
	}

	public void addSource(String source)
	{
		Source[] oldSources = sources;
		sources = new Source[sources.length+1];
		for (int i = 0; i < oldSources.length; i++)
			sources[i] = oldSources[i];

		sources[sources.length-1] = new Source(source);
	}

	void init()
	{
		min = 30;
		max = 500;
		increment = 10;
		fuzzy = false;
	}

	int				min;
	int				max;
	int				increment;
	boolean			fuzzy;

	boolean			cpd;
	Source[]		sources;
}

///////////////////////////////////////////////////////////////////////////
class Source
{
	public Source(Hashtable action)
	{
		this.root = (String)action.get("root");

		this.name = (String)action.get("name");

		this.def = true;
		String def = (String)action.get("def");
		if (def == null || def.compareTo("0") == 0 || def.compareTo("false") == 0)
			this.def = false;

		ArrayList dirs = (ArrayList)action.get("directories");
		if (dirs == null)
			directories = new String[0];
		else
		{
			directories = new String[dirs.size()];
			for (int i = 0; i < dirs.size(); i++)
				directories[i] = (String)dirs.get(i);
		}

		ArrayList inc = (ArrayList)action.get("include");
		if (inc != null)
		{
			include = new java.util.regex.Pattern[inc.size()];
			for (int i = 0; i < inc.size(); i++)
				include[i] = getFilePattern((String)inc.get(i));
		}

		ArrayList exc = (ArrayList)action.get("exclude");
		if (exc == null)
			exclude = new java.util.regex.Pattern[0];
		else
		{
			exclude = new java.util.regex.Pattern[exc.size()];
			for (int i = 0; i < exc.size(); i++)
				exclude[i] = getFilePattern((String)exc.get(i));
		}
	}

	public Source()
	{
		root = ".";
		directories = new String[0];
		include = new java.util.regex.Pattern[1];
		include[0] = getFilePattern("*.js");

		exclude = new java.util.regex.Pattern[0];

		def = true;
	}

	public Source(String pattern)
	{
		name = pattern;
		directories = new String[0];
		include = new java.util.regex.Pattern[1];

		int index = pattern.lastIndexOf('/');

		if (index == pattern.length() - 1)
		{
			root = pattern;
			include[0] = getFilePattern("*.js");
		}
		else if (index != -1)
		{
			root = pattern.substring(0, index);
			include[0] = getFilePattern(pattern.substring(index+1));
		}
		else
		{
			root = ".";
			include[0] = getFilePattern(pattern);
		}

		exclude = new java.util.regex.Pattern[0];

		def = true;
	}

	/////////////////////////////////////////////////////////////////////
	static java.util.regex.Pattern getFilePattern(String pattern)
	{
		// convert pattern from a file pattern to a regex,
		// i.e.  * -> .*   ? -> .   . -> \.
		pattern = pattern.replace(".", "\\.");
		pattern = pattern.replace("*", ".*");
		pattern = pattern.replace("?", ".");

		return java.util.regex.Pattern.compile(pattern);
	}

	public String						name;
	public String						root;
	public String[]						directories;
	public java.util.regex.Pattern[]	include;
	public java.util.regex.Pattern[]	exclude;
	public boolean						def;
}
