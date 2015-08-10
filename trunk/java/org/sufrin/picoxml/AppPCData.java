package org.sufrin.picoxml;

import java.io.PrintWriter;

public class AppPCData extends AppText
{ /** Was the text derived from a CDATA section */
  boolean cdata = false;

  public AppPCData(String text, boolean cdata)
  {
    super(text);
    this.cdata=cdata;
  }

  public void printTo(PrintWriter out, int indent)
  {
    for (int i = 0; i < indent; i++)
      out.print(" ");
    if (cdata) out.print("<![CDATA[");
    out.print(text);
    if (cdata) out.print("]]>");
  }
}
