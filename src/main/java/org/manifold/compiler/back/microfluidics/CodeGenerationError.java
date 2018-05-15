package org.manifold.compiler.back.microfluidics;

/*
 * Error for when a schematic does not work as expected
 */
public class CodeGenerationError extends Error {
  // Provide a serialVersionUID so that it is consistent, unlike the default
  // one that is based on the implementation of the class and can change
  private static final long serialVersionUID = 3881406564637721054L;

  private String message;

  public CodeGenerationError(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

}
