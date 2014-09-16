package org.manifold.compiler.back.microfluidics;

public class CodeGenerationError extends Error {
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
