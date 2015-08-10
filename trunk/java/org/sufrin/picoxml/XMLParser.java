package org.sufrin.picoxml;

import java.util.*;

/**
 * An XMLParser&lt;T> handles XML lexical events by constructing a tree
 * of type <tt>T</tt>.
 * */
public class XMLParser<T> implements XMLHandler
{ protected XMLTreeFactory<T> factory; 
  public  XMLParser(XMLTreeFactory<T> factory) { this.factory=factory; }

  protected Stack<Composite<T>>  stack = new Stack<Composite<T>>();
  protected Stack<String>        kinds = new Stack<String>();
  // Invariant: stack.size()==kinds.size()
  
  /** Open a new element of the given kind with the given attributes */
  public void startElement(String kind, Map<String,String> atts) 
  { stack.push(factory.newElement(kind, atts)); kinds.push(kind); }
  
  /** Close the current topmost element if it is
   *  of the given kind, and add it to its parent element.
   */
  public void endElement(String kind) 
  { String tkind = kinds.pop();    
    if (tkind.equals(kind))
       { T top=stack.pop().close(); stack.peek().addTree(top); }
    else 
       throw new RuntimeException(String.format("Non-nested: <%s>...</%s>", tkind, kind));
  }
  
  /** The root tree after the last endDocument() */
  protected T theTree = null;
  /** Return the root tree */
  public    T getTree()  { return theTree; }
  
  /** Add a comment to the tree if the treeFactory is handling comments */
  public void acceptComment(CharSequence text)
  { if (factory.canComment()) stack.peek().addTree(factory.newComment(text.toString())); }
  
  /** Add PCDATA to the tree */
  public void   acceptPCDATA(CharSequence text, boolean cdata) 
  { stack.peek().addTree(factory.newPCData(text.toString(), cdata)); }
  
  /** Clear the parse stack and be prepared to start parsing a new document */
  public void   startDocument()             
  { stack.clear(); stack.push(factory.newRoot()); kinds.clear(); kinds.push(""); }
  
  /** Finish parsing the current document and make the root tree accessible 
   * if the document was complete
   */
  public void   endDocument()               
  { switch (stack.size())
    { case 1:  theTree = stack.peek().close(); break;
      case 0:  throw new RuntimeException("Document has no elements."); 
      default:
       throw new RuntimeException
          (String.format("Premature end of document in unclosed <%s>", kinds.peek()));
    }
  }

  public String decodeEntity(String entity) { return null; }

  public void acceptDTD(CharSequence text)
  {  
    if (factory.canDTD()) stack.peek().addTree(factory.newDTD(text.toString()));
  }

  public void acceptPI(CharSequence text)
  {  
    if (factory.canPI()) stack.peek().addTree(factory.newPI(text.toString()));
  }
}
