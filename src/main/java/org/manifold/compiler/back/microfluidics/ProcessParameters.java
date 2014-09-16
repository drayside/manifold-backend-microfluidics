package org.manifold.compiler.back.microfluidics;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// Contains values defining the parameters 
// and limitations of the manufacturing process.
public class ProcessParameters {
  // Minimum distance between nodes (meters)
  private double minimumNodeDistance;
  public double getMinimumNodeDistance() {
    return minimumNodeDistance;
  }
  
  // Minimum channel length (meters)
  private double minimumChannelLength;
  public double getMinimumChannelLength() {
    return minimumChannelLength;
  }
  
  // Maximum chip area (meters)
  private double maximumChipSizeX;
  public double getMaximumChipSizeX() {
    return maximumChipSizeX;
  }
  private double maximumChipSizeY;
  public double getMaximumChipSizeY() {
    return maximumChipSizeY;
  }
  
  // Critical angle for channel crossings (radians)
  private double criticalCrossingAngle;
  public double getCriticalCrossingAngle() {
    return criticalCrossingAngle;
  }
  
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
