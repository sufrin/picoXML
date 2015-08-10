package org.sufrin.picoxml;
import java.io.*;

/** A test application that prettyprints every XML file
 *  named on its argument list.
 *  
 */
public class App
{
  public static void main (String[] args) throws Exception
  { XMLParser<AppTree> parser  = new XMLParser<AppTree>(new AppTreeFactory());
    XMLScanner         scanner = new XMLScanner(parser);
    PrintWriter        out     = new PrintWriter(System.out);
    for (String arg: args)
    { scanner.read(new FileReader(arg));
      AppElement root = (AppElement) parser.getTree();
      for (AppTree tree: root) 
      {  tree.printTo(out, 0);
         out.println();
      }
      out.flush();
    }
  }
}
