/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rosuda.rserve;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.rosuda.rengine.REXP;
import org.rosuda.rengine.REXPMismatchException;
import org.rosuda.rengine.REngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cemmersb
 */
public class RservePlotTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(RservePlotTest.class);
  /**
   * Connection to RServe
   */
  private RConnection connection;
  /**
   * Output device format
   */
  private String device = null;
  /**
   * File name the plot is written to
   */
  private String fileName = null;
  /**
   * File where the output is written to
   */
  private File file = null;
  
  @Before
  public void setUp() throws RserveException, REngineException, REXPMismatchException {
    connection = new RConnection("127.0.0.1");
    device = configureDevice();
    fileName = configureFileName();
    evaluateEnvironment();

  }

  private String configureDevice() throws REngineException, REXPMismatchException {
    String dev;
    final REXP rexp = connection.parseAndEval("suppressWarnings(require('Cairo',quietly=TRUE))");
    // Check that the command returned a value
    assertNotNull(rexp);
    // Set the output device either to CairoJPEG or jpeg
    if (0 < rexp.asInteger()) {
      dev = "CairoJPEG";
    } else {
      dev = "jpeg";
    }
    LOGGER.debug("Using output device: {}", dev);
    return dev;
  }

  private String configureFileName() {
    final StringBuilder stringBuilder = new StringBuilder()
            .append("test-")
            .append((new Date()).getTime())
            .append(".jpg");
    return stringBuilder.toString();
  }

  private void evaluateEnvironment() throws REngineException, REXPMismatchException {
    final REXP rexp = connection.parseAndEval("try(" + device + "('" + fileName + "',quality=90))");
    assertNotNull(rexp);

    if (rexp.inherits("try-error")) {
      LOGGER.error("Can not open {} graphics device: {}", device, rexp.asString());
      final REXP error = connection.eval("if (exists('last.warning') && length(last.warning)>0) "
              + "names(last.warning)[1] else 0");
      if (error.isString()) {
        LOGGER.error("R exception: {}", error.asString());
      }
    }
  }
  
  public String writePlot(byte[] image) {
    final String fileLocation = RservePlotTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    try {
      final File localFile = new File(fileLocation + "../" + fileName);
      final FileOutputStream outputStream = new FileOutputStream(localFile);
      outputStream.write(image);
      outputStream.flush();
      outputStream.close();
    } catch (FileNotFoundException ex) {
      LOGGER.error("Could locate file: {}", ex);
    } catch (IOException ex) {
      LOGGER.error("Could not write file: {}", ex);
    }
    return fileLocation;
  }
  
  @Test
  public void plotTest() throws REngineException, REXPMismatchException {
    // Plotting iris data
    connection.parseAndEval("data(iris);\n"
            + "attach(iris);\n"
            + "plot(Sepal.Length, Petal.Length, col=unclass(Species));\n"
            + "dev.off()");
    // Due to efficiency there is now I/O API in REngine. However it is possble to
    // use R directly for accessing the plot's binaries
    final REXP rexp = connection.parseAndEval("r=readBin('" + fileName + "','raw',1024*1024);\n"
            + "unlink('test.jpg');\n"
            + "r");
    final byte[] image = rexp.asBytes();
    assertNotNull(image);
    
    file = new File(writePlot(image));
    assertTrue(file.exists());
  }
  
  @After
  public void tearDown() {
    connection.close();
  }
}