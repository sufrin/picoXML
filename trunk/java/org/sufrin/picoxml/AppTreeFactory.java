
package org.sufrin.picoxml;

import java.util.*;

public class AppTreeFactory implements XMLTreeFactory<AppTree>
{
  public AppElement newElement(String kind, Map<String, String> atts)
  {
    return new AppElement(kind, atts);
  }

  public AppElement newRoot()
  {
    return newElement("", null);
  }

  public AppText newPCData(String name, boolean cdata)
  {
    return new AppPCData(name, cdata);
  }

  public AppText newComment(String data)
  {
    return new AppComment(data);
  }

  public boolean canComment()
  {
    return true;
  }

  public AppTree newPI(String text)
  {
    return new AppPI(text);
  }

  public boolean canPI()
  {
    return true;
  }

  public AppTree newDTD(String text)
  {
    return null;
  }

  public boolean canDTD()
  {
    return false;
  }
}
