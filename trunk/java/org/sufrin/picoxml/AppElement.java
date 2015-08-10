
package org.sufrin.picoxml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/** An AppElement represents an XML element tree. */
public class AppElement implements AppTree, Composite<AppTree>, Iterable<AppTree>
{ /** The full element name */
  protected String kind;
  /** Mapping from attribute names to attributes */
  protected Map<String, String> attrs;
  /** Subtrees in order of accession. */
  protected Vector<AppTree> subtrees = new Vector<AppTree>();
  
  /** Signal (usually from a parser) that no more subtrees will be added. */
  public AppTree close()
  {
    return this;
  }
  
  /** Constructor */
  public AppElement(String kind, Map<String, String> attrs)
  {
    this.kind = kind;
    this.attrs = attrs;
  }
  
  /** Add a new subtree */
  public void addTree(AppTree t)
  {
    subtrees.add(t);
  }
  
  /** Get the full name of the element */
  public String getKind()
  {
    return kind;
  }
  
  /** Iterate over the subtrees */
  public Iterator<AppTree> iterator()
  {
    return subtrees.iterator();
  }
  
  public String toString()
  { StringWriter w   = new StringWriter(); 
    PrintWriter  out = new PrintWriter(w);
    this.printTo(out, 0);
    return w.toString();
  }

  public void printTo(PrintWriter out, int indent)
  { for (int i = 0; i < indent; i++)
      out.print(" ");          // Indent to open bracket position
    if (subtrees.size() == 0)  // Can we abbreviate the tree?
      out.print(String.format("<%s%s/>", kind, attrs));
    else
    {
      out.print(String.format("<%s%s>", kind, attrs));
      boolean wasWord = false; // Last printed tree was a Word
      for (AppTree t : subtrees)
      {
        boolean isWord = t instanceof AppPCData;
        boolean needNL = !wasWord || !isWord;
        if (needNL)
          out.println();
        t.printTo(out, needNL ? indent + 2 : 1);
        wasWord = isWord;
      }
      out.println();
      for (int i = 0; i < indent; i++)
        out.print(" "); // Align close bracket with open bracket
      out.print(String.format("</%s>", kind));
    }
  }
}
