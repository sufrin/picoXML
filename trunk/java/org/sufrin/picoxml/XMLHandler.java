package org.sufrin.picoxml;
import java.util.Map;

/** An XMLHandler accepts parsing events that represent the occurence, in an
 *  XML text, of significant (composite) tokens.
 *
 *  An occurence of &lt;kind key1="val1" .../> gives rise to a call of <tt>startElement</tt>
 *  immediately followed by a call of <tt>endElement</tt>.
 */
public interface XMLHandler
{ /** Called on occurence of &lt;kind key1="val1" ...> */
  public void   startElement(String kind, Map<String,String> atts);    
  /** Called on occurence of &lt;/kind> */
  public void   endElement(String kind);                              
  /** &lt;!-- ... -->*/
  public void   acceptComment(CharSequence text);                  
  /** &lt;? ... ?>*/
  public void   acceptDTD(CharSequence text);                  
  /** &lt;!DOCTYPE ... >*/
  public void   acceptPI(CharSequence text);                  
  /** Text (cdata true if text came from CDATA) */
  public void   acceptPCDATA(CharSequence text, boolean cdata);                    
  /** Called before the XML document is read. */
  public void   startDocument();        
  /** Called at the end of the XML document. */
  public void   endDocument();          
  /** Should return the coding for &amp;entityname; */
  public String decodeEntity(String entity);   
}
