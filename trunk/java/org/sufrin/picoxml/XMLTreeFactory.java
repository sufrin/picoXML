
package org.sufrin.picoxml;

import java.util.*;

public interface XMLTreeFactory<Tree>
 {
  Composite<Tree> newElement(String kind, Map<String, String> atts);

  Composite<Tree> newRoot();

  Tree newPCData(String text, boolean cdata);

  Tree newComment(String text);

  Tree newPI(String text);
  
  Tree newDTD(String text);

  boolean canComment();

  boolean canPI();
  
  boolean canDTD();
  
}
