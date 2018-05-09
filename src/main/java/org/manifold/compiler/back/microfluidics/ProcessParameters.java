package org.manifold.compiler.back.microfluidics;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Contains values defining the parameters 
 * and limitations of the manufacturing process.
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class ProcessParameters {
  
  private final double minimumNodeDistance;
  /**
   * Get the minimum distance between nodes
   * 
   * @return Minimum distance between nodes (meters)
   */
  public double getMinimumNodeDistance() {
    return minimumNodeDistance;
  }
  
  private final double minimumChannelLength;
  /**
   * Get the minimum channel length
   * 
   * @return Minimum length of channel (meters)
   */
  public double getMinimumChannelLength() {
    return minimumChannelLength;
  }
  
  private final double maximumChipSizeX;
  /**
   * Get the X dimension of the maximum size of the microfluidic chip
   * 
   * @return X side of the maximum size of chip (meters)
   */
  public double getMaximumChipSizeX() {
    return maximumChipSizeX;
  }

  private final double maximumChipSizeY;
  /**
   * Get the Y dimension of the maximum size of the microfluidic chip
   * 
   * @return Y side of the maximum size of chip (meters)
   */
  public double getMaximumChipSizeY() {
    return maximumChipSizeY;
  }
  
  // 
  private final double criticalCrossingAngle;
  /**
   * Get the critical angle for channel crossings that will cause
   * 
   * @return Critical angle for channel crossings (radians)
   */
  public double getCriticalCrossingAngle() {
    return criticalCrossingAngle;
  }
  
  /**
   * Set the parameters for a microfluidic chip
   * @param minimumNodeDistance  Minimum distance between nodes (meters)
   * @param minimumChannelLength  Minimum length of channel (meters)
   * @param maximumChipSizeX  X side of the maximum size of chip (meters)
   * @param maximumChipSizeY  Y side of the maximum size of chip (meters)
   * @param criticalCrossingAngle  Critical angle for channel crossings (radians)
   */
  private ProcessParameters(
      double minimumNodeDistance, double minimumChannelLength,
      double maximumChipSizeX, double maximumChipSizeY,
      double criticalCrossingAngle) {
    this.minimumNodeDistance = minimumNodeDistance;
    this.minimumChannelLength = minimumChannelLength;
    this.maximumChipSizeX = maximumChipSizeX;
    this.maximumChipSizeY = maximumChipSizeY;
    this.criticalCrossingAngle = criticalCrossingAngle;
  }
  
  /**
   * Dimensions of a test chip 
   * 
   * @return Dimensions of the chip
   */
  public static ProcessParameters loadTestData() {
    // TODO refactor test cases that use this to load from another source
    return new ProcessParameters(
        0.001, 0.00001,
        0.05, 0.05,
        0.0872664626);
  }
  
  /**
   * Gets a number from JSON files at location defined by key, used to get chip dimension values
   * 
   * @param input  JSON file object
   * @param key  Location of number in JSON
   * @return number as a Double
   */
  private static double readJsonDouble(JsonObject input, String key) {
    JsonElement d = input.get(key);
    if (d == null) {
      throw new IllegalArgumentException("required parameter '" + key + "'"
          + " not found in provided JSON file");
    }
    try {
      return d.getAsDouble();
    } catch (ClassCastException | IllegalStateException e) {
      throw new IllegalArgumentException("parameter '" + key + "'" 
          + " must be a double-precision value");
    }
  }
  
  /**
   * Gets a number from the command line, used to get chip dimension values
   * 
   * @param cli  CommandLine object
   * @param key  Location of the number in the 
   * @return number as a Double
   */
  private static double readCLIDouble(CommandLine cli, String key) {
    String valString = cli.getOptionValue(key);
    if (valString == null) {
      throw new IllegalArgumentException("required option '" + key + "'"
          + " was not specified");
    }
    try {
      return Double.parseDouble(valString);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("option '" + key + "'" 
          + " must be a double-precision value");
    }
  }
  
  @SuppressWarnings("static-access")
  private static void createDoubleOption(Options opts, String key, 
      String description) {
    Option dOpt = OptionBuilder.withArgName("double")
        .hasArg()
        .withDescription(description)
        .create(key);
    opts.addOption(dOpt);
  }
  
  /**
   * Creates options for initializing process parameters
   * 
   * @param opts  A default Options object
   */
  public static void createOptions(Options opts) {
    createDoubleOption(opts, "bProcessMinimumNodeDistance",
        "minimum distance between nodes (meters)");
    createDoubleOption(opts, "bProcessMinimumChannelLength",
        "minimum channel length (meters)");
    createDoubleOption(opts, "bProcessMaximumChipSizeX",
        "chip dimension in X-direction (meters)");
    createDoubleOption(opts, "bProcessMaximumChipSizeY",
        "chip dimension in Y-direction (meters)");
    createDoubleOption(opts, "bProcessCriticalCrossingAngle",
        "minimum crossing angle for channels (radians)");
  }

  /**
   * Initializes process parameters with values from JSON file located at path,
   * Searches for parameters with keys minimumNodeDistance, minimumChannelLength, maximumChipSizeX, maximumChipSizeY,
   * and criticalCrossingAngle
   * 
   * @param path  Path to the JSON file to be read
   * @return ProcessParameters object constructed with values read from JSON file
   * @throws IOException  if JSON files cannot be read
   */
  public static ProcessParameters loadFromFile(String path) throws IOException {
    Path p = Paths.get(path);
    Charset charset = Charset.forName("UTF-8");
    BufferedReader reader = Files.newBufferedReader(p, charset);
    JsonObject input = new JsonParser().parse(reader).getAsJsonObject();
    return new ProcessParameters(
        readJsonDouble(input, "minimumNodeDistance"),
        readJsonDouble(input, "minimumChannelLength"),
        readJsonDouble(input, "maximumChipSizeX"),
        readJsonDouble(input, "maximumChipSizeY"),
        readJsonDouble(input, "criticalCrossingAngle")
    );
  }
  
  /**
   * Initializes process parameters with values from command line
   * Searches for parameters with keys minimumNodeDistance, minimumChannelLength, maximumChipSizeX, maximumChipSizeY,
   * and criticalCrossingAngle
   * 
   * @param cli  CommandLine object containing parameter values
   * @return ProcessParameters object constructed with values read from command line
   * @throws IllegalArgumentException if command line cannot be read
   */
  public static ProcessParameters loadFromCommandLine(CommandLine cli) 
      throws IllegalArgumentException {
    return new ProcessParameters(
        readCLIDouble(cli, "bProcessMinimumNodeDistance"),
        readCLIDouble(cli, "bProcessMinimumChannelLength"),
        readCLIDouble(cli, "bProcessMaximumChipSizeX"),
        readCLIDouble(cli, "bProcessMaximumChipSizeY"),
        readCLIDouble(cli, "bProcessCriticalCrossingAngle")
    );
  }
}
