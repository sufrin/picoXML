package org.sufrin.picoxml;

import java.io.PrintWriter;

public class AppText implements AppTree
{
  protected String text;
  
  public AppText(String text)
  {
    this.text=text;
  }

  
  public String toString() { return text; }
  
  public void printTo(PrintWriter out, int indent)
  {
    for (int i = 0; i < indent; i++)
      out.print(" ");
    out.print(text);
  }

}