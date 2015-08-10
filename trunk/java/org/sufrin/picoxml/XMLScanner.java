//

package org.sufrin.picoxml;

import java.util.*;
import java.io.*;

/**
 * An XMLScanner reads XML text from a Reader, 
 * calling the methods of an XMLHandler at syntacically 
 * significant places in the text.
 *
 */
public class XMLScanner
{ /** XML event handler */
  protected XMLHandler handler;

  protected Reader reader;
  
  /** Line the current character came from */
  protected int chLine; 
  
  /** Line the current token started on */
  protected int tokenLine; 

  public static class XMLSyntax extends RuntimeException
  { public final int    tokenLine;
    public final String error;
    public XMLSyntax(int tokenLine, String error)
    { 
      super(String.format("Line %d: %s", tokenLine, error));
      this.tokenLine=tokenLine;
      this.error=error;
    }
  }

  public XMLScanner()
  {
  }

  public XMLScanner(XMLHandler consumer)
  {
    setHandler(consumer);
  }

  public void setHandler(XMLHandler consumer)
  {
    this.handler = consumer;
  }

  public int lineNumber()
  {
    return tokenLine;
  }

  /** The current lexical symbol */
  protected Lex token = null;

  /**
   * Check the current token is the given token and skip
   * over it.
   */
  protected void skipToken(Lex aToken)
  {
    checkToken(aToken);
    nextToken();
  }

  /**
   * Check the current token is the given token; fail if it
   * isn't.
   */
  protected void checkToken(Lex aToken)
  {
    nextToken();
    if (this.token != aToken)
      throw new XMLSyntax(tokenLine, aToken + " expected; found " + this.token + " " + this.value);
  }

  private enum Lex 
  {
    ENDSTREAM     ("END-OF-XML-STREAM"), 
    POINTBRA      ("<"), 
    POINTKET      (">"), 
    POINTBRASLASH ("</"), 
    SLASHPOINTKET ("/>"), 
    WORD          ("WORD"), 
    IDENTIFIER    ("IDENTIFIER"), 
    CDATA         ("<![CDATA[ ..."), 
    SQUOTE        ("' or \""), 
    EQUALS        ("="), 
    COMMENT       ("<!-- ... -->"), 
    PROCESS       ("<? ... ?>"), 
    DOCTYPE       ("<!DOCTYPE ...");
    
    Lex(String name)
    {
      this.name = name;
    }

    private String name;

    public String toString()
    {
      return name;
    }
  }

  /**
   * Read XML from the given Reader, invoking the current
   * handler's methods at appropriate times.
   */
  public void read(Reader aReader)
  {
    this.reader = aReader;
    this.chLine = 1;
    ch = 0;
    nextToken();
    handler.startDocument();
    while (token != Lex.ENDSTREAM)
    {
      switch (token)
      {
        case DOCTYPE:
          handler.acceptDTD(value);
        break;
        case PROCESS:
          handler.acceptPI(value);
        break;

        case IDENTIFIER:
        case WORD:
        case CDATA:
          handler.acceptPCDATA(value, token == Lex.CDATA);
        break;

        case POINTBRASLASH: // </ tag >
          checkToken(Lex.IDENTIFIER);
          handler.endElement(value);
          checkToken(Lex.POINTKET);
        break;

        case COMMENT: // <!-- ... -->
          handler.acceptComment(value);
        break;

        default:
          throw new XMLSyntax(tokenLine, "Unexpected token: " + token + " " + value);
        case POINTBRA: // <id id="..." ...
        {
          Map<String, String> atts = new Attributes();
          inElement = true;
          checkToken(Lex.IDENTIFIER);
          String tag = value;
          nextToken();

          while (token == Lex.IDENTIFIER)
          {
            String key = value;
            skipToken(Lex.EQUALS);
            if (token == Lex.SQUOTE)
            {
              atts.put(key.intern(), value.intern());
              nextToken();
            }
            else throw new XMLSyntax(tokenLine, "Found " + token + " when string expected in " + key + "=...");
          }

          handler.startElement(tag, atts);
          if (token == Lex.SLASHPOINTKET) // />
            handler.endElement(tag);
          else if (token != Lex.POINTKET) // >
            throw new XMLSyntax(tokenLine, "> expected in start tag: found " + token);
          inElement = false;
        }
      }
      nextToken();
    }
    handler.endDocument();
    try
    {
      aReader.close();
    }
    catch (Exception e)
    {
    }
  }

  /** The current symbol's characters, if it's a class */
  protected String value = null;

  /** The current character */
  protected int ch;

  /** Expansion of the last entity read */
  protected String entity;

  /**
   * True iff currently reading an element header < ... />
   * or < ... >
   */
  protected boolean inElement = false;

  /** Read the next token */
  protected void nextToken()
  {
    if (0 <= ch && ch <= ' ')
    {
      do
      {
        nextRawChar();
      }
      while (0 <= ch && ch <= ' ');
    }
    tokenLine = chLine;
    value = "";
    if (ch == -1)
    {
      token = Lex.ENDSTREAM;
    }
    else
    // ... substantive symbols ...
    if (inElement && ch == '=')
    {
      token = Lex.EQUALS;
      value = "";
      nextRawChar();
    }
    else if (inElement && (ch == '\'' || ch == '"'))
    {
      int close = ch;
      StringBuilder b = new StringBuilder();
      nextChar();
      while (0 <= ch && ch != close)
      {
        if (ch == '&') { b.append(entity); entity=null; }
        else b.append((char) ch);
        nextChar();
      }
      token = Lex.SQUOTE;
      value = b.toString();
      nextRawChar();
    }
    else if (inElement && ch == '/')
    {
      nextRawChar();
      if (ch == '>')
      {
        nextRawChar();
        token = Lex.SLASHPOINTKET;
      }
      else throw new XMLSyntax(tokenLine, "/> expected; found /" + ((char) ch));
    }
    else if (ch == '<')
    {
      nextRawChar();
      if (ch == '/')
      {
        nextRawChar();
        token = Lex.POINTBRASLASH;
      }
      else if (ch == '?')
      {
        nextRawChar();
        int lastch = ch;
        StringBuilder b = new StringBuilder();
        while (0 <= ch && !(ch == '>' && lastch == '?'))
        {
          b.append((char) ch);
          lastch = ch;
          nextRawChar();
        }
        if (ch == -1)
          throw new XMLSyntax(tokenLine, "<? with runaway body ...");
        else
        {
          nextRawChar();
          value = b.substring(0, b.length() - 1);
          token = Lex.PROCESS;
        }
      }
      else if (ch == '!')
      {
        nextRawChar();
        if (ch == '[') // Assume <![CDATA and read to
                        // closing ]]>
        {
          StringBuilder b = new StringBuilder();
          do
          {
            while (0 <= ch && ch != '>')
            {
              b.append((char) ch);
              nextRawChar();
            }
            if (ch > 0)
            {
              b.append((char) ch);
              nextRawChar();
            }
          }
          while (0 <= ch && !endCDATA(b));

          if (ch < 0)
            throw new XMLSyntax(tokenLine, "<![CDATA[ ... ]]> expected; found <!" + b.toString() + " at end of file");
          else if (isCDATA(b))
          {
            value = b.substring(7, b.length() - 3);
            token = Lex.CDATA;
          }
          else throw new XMLSyntax(tokenLine, "<![CDATA[ ... ]]> expected; found <!" + b.toString() + ">");
        }
        else if (ch == 'D') // Assume <!DOCTYPE and read to
                            // matching closing >
        {
          int count = 1;
          while (0 <= ch && count > 0)
          {
            nextRawChar();
            if (ch == '<')
              count++;
            else if (ch == '>')
              count--;
          }
          if (count != 0)
            throw new XMLSyntax(tokenLine, "<!DOCTYPE with runaway body ...");
          token = Lex.DOCTYPE;
          nextRawChar();
        }
        else
        // Assume <!-- comment -->
        {
          StringBuilder b = new StringBuilder();
          do
          {
            while (0 <= ch && ch != '>')
            {
              b.append((char) ch);
              nextRawChar();
            }
            if (ch > 0)
            {
              b.append((char) ch);
              nextRawChar();
            }
          }
          while (0 <= ch && !endComment(b));

          if (isComment(b))
          {
            value = b.substring(2, b.length() - 3);
            token = Lex.COMMENT;
          }
          else throw new XMLSyntax(tokenLine, "<!-- ... --> expected; found <!" + b.toString());
        }
      }
      else token = Lex.POINTBRA;
    }
    else if (ch == '>')
    {
      nextRawChar();
      token = Lex.POINTKET;
    }
    else
    // a new pcdata lump begins
    {
      StringBuilder b = new StringBuilder();
      token = Lex.IDENTIFIER;
      // leading & is a special case
      if (ch == '&')
      {
        token = Lex.WORD;
        nextEnt();
        b.append(entity); entity=null;
        nextChar();
      }
      while (ch > ' ' && ch != '<' && ch != '>' && !(inElement && (ch == '/' || ch == '=')))
      {
        if (ch == '&')
          b.append(entity);
        else b.append((char) ch);
        if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != ':')
          token = Lex.WORD;
        nextChar();
      }
      value = b.toString();
    }
  }

  /** Read the next character -- expanding entities */
  protected void nextChar()
  {
    nextRawChar();
    if (ch == '&')
    {
      nextEnt();
      ch = '&';
    }
  }

  /**
   * Read the next raw character, keeping track of the line
   * number
   */
  protected void nextRawChar()
  {
    try
    {
      ch = reader.read();
      if (ch == '\n')
        chLine++;
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Read and expand the next entity; the variable 'entity'
   * is set to the expansion.
   */
  protected void nextEnt()
  {
    String ent = ""; entity = null;
    nextRawChar();
    while (' ' < ch && ch != ';')
    {
      ent = ent + ((char) ch);
      nextRawChar();
    }         
    entity = handler.decodeEntity(ent);
    if (entity == null) entity = decodeDefaultEntity(tokenLine, ent);
    if (entity == null)     
      throw new XMLSyntax(tokenLine, "Unknown entity: &" + ent + ";");
  }
  
  /** Default simple entity decoding */
  public static String decodeDefaultEntity(int tokenLine, String ent)
  {
    if (ent.equals("amp"))
      return "&";
    else if (ent.equals("lt"))
      return "<";
    else if (ent.equals("gt"))
      return ">";
    else if (ent.equals("apos"))
      return "'";
    else if (ent.equals("quot"))
      return "\"";
    else if (ent.equals("nbsp"))
      return " ";
    else if (ent.startsWith("#"))
      try
      {
        return ""+(char) Integer.parseInt(ent.substring(1));
      }
      catch (NumberFormatException ex)
      {
         throw new XMLSyntax(tokenLine, "Ill-formed numeric entity: &" + ent + ";");
      }
    else return null;
  }

  protected static boolean endComment(StringBuilder b)
  {
    int s = b.length();
    return s > 4 && b.charAt(s - 1) == '>' && b.charAt(s - 2) == '-' && b.charAt(s - 3) == '-';
  }

  protected static boolean isComment(StringBuilder b)
  {
    return endComment(b) && b.charAt(0) == '-' && b.charAt(1) == '-';
  }

  protected static boolean endCDATA(StringBuilder b)
  {
    int s = b.length();
    return s > 4 && b.charAt(s - 1) == '>' && b.charAt(s - 2) == ']' && b.charAt(s - 3) == ']';
  }

  protected static boolean isCDATA(StringBuilder b)
  {
    return endCDATA(b) && (b.length() > 7) && b.substring(0, 7).equals("[CDATA[");
  }

  /** A test rig that prints lexical events, one per line. */
  public static void main(String[] args)
  {
    XMLHandler sax = new XMLHandler()
    {
      void pr(CharSequence s, CharSequence t)
      {
        System.out.println(s + " " + t);
      }

      public void acceptPCDATA(CharSequence chars, boolean cdata)
      {
        pr("WD", "'" + chars + "'");
      }

      public void startElement(String kind, Map<String, String> atts)
      {
        pr("SE", kind + atts);
      }

      public void endElement(String kind)
      {
        pr("EE", kind);
      }

      public void acceptComment(CharSequence data)
      {
        pr("CO", data);
      }

      public void acceptPI(CharSequence data)
      {
        pr("PI", data);
      }

      public void acceptDTD(CharSequence data)
      {
        pr("DT", data);
      }

      public void startDocument()
      {
        pr("ST", "");
      }

      public void endDocument()
      {
        pr("EN", "");
      }

      public String decodeEntity(String s)
      {
        return "&" + s + ";";
      }
    };
    new XMLScanner(sax).read(new InputStreamReader(System.in));
  }

  /**
   * An implementation of Map that /shows/ attribute values in
   * properly-quoted XML form. The values are actually stored
   * is normalized (sequence-of-UTF8 characters) form. 
   */
  public static class Attributes extends LinkedHashMap<String, String>
  {
    public String toString()
    {
      StringBuilder b = new StringBuilder();
      for (String key : keySet())
      {
        b.append(" ");
        b.append(key);
        b.append("='");
        b.append(unQuote(get(key)));
        b.append("'");
      }
      return b.toString();
    }
  }

  /** Re-quote special characters within a string. */
  public static String unQuote(String s)
  {
    int len = s.length();
    StringBuilder quoted = null;
    for (int i = 0; i < len; i++)
    {
      char c = s.charAt(i);
      if (  c == ' ' 
         || c == '&'
         || c == '>' 
         || c == '<' 
         || c == '"' 
         || c == '\'' 
         || c >= 128
         )
      {
        quoted = new StringBuilder();
        break;
      }
    }
    if (quoted == null)
      return s;
    else
    {
      for (int i = 0; i < len; i++)
      {
        char c = s.charAt(i);
        quoted.append
        (( c == '&' ? "&amp;" 
         : c == '>' ? "&gt;" 
         : c == '<' ? "&lt;" 
         : c == '"' ? "&quot;" 
         : c == '\''? "&apos;"
      // :c==' ' ?"&nbsp;"
         : c > 128 ? ("&#" + (int) c + ";") : ("" + c)));
      }
      return quoted.toString();
    }
  }
}

