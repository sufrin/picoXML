
package org.sufrin.picoxml;

/** 
 * A Composite&lt;Tree> can have sub&lt;Tree>s added to it until it is closed.
 * On being closed it returns its essential content in the form of a Tree.
 * 
 * @param <Tree>
 */
public interface Composite<Tree>
 {
  void addTree(Tree subtree);

  Tree close();
}
