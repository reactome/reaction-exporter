package org.reactome.server.tools.reaction.exporter;

/**
 * @author Piotr Gawron
 *
 */
public class InvalidArgumentException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public InvalidArgumentException(String message) {
    super(message);
  }
}
