/**
 * 
 */
package org.sufrin.picoxml;

import java.io.PrintWriter;

/**
 * Represents a processing element
 */
public class AppPI extends AppText
{ public AppPI(String text) { super(text); }

  public void printTo(PrintWriter out, int indent)
  {
    for (int i = 0; i < indent; i++)
      out.print(" ");
    out.print("<? ");
    out.print(text);
    out.print(" ?>");
  }
}
