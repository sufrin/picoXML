package org.sufrin.picoxml;

import java.io.PrintWriter;

public class AppComment extends AppText
{

  public AppComment(String text)
  {
    super(text);
  }

  public void printTo(PrintWriter out, int indent)
  {
    for (int i = 0; i < indent; i++)
      out.print(" ");
    out.print("<!--");
    out.print(text);
    out.print("-->");
  }
}
