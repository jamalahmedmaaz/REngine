/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rosuda.rserve;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.rosuda.rengine.REXPMismatchException;
import org.rosuda.rengine.REXPString;
import org.rosuda.rengine.REngine;
import org.rosuda.rengine.RList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cemmersb
 */
public class RserveTest {

  /**
   * Provides some detailed output on test execution.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RserveTest.class);
  /**
   * Connection object to establish communication to Rserve.
   */
  private RConnection connection = null;
  /**
   * Backend agnostic object providing an abstraction to RConnection.
   */
  private REngine engine = null;

  @Before
  public void startUpRserve() throws RserveException {
    //TODO: Implement code to start Rserve on local machine
    connection = new RConnection();
    engine = (REngine) connection;
  }

  @Test
  public void versionStringTest() throws RserveException, REXPMismatchException {
    final String versionString = connection.eval("R.version$version.string").asString();
    LOGGER.debug(versionString);
    assertNotNull(versionString);
    assertTrue(versionString.contains("R version"));
  }

  @Test
  public void stringAndListRetrievalTest() throws RserveException, REXPMismatchException {
    final RList list = connection.eval("{d=data.frame(\"huhu\",c(11:20)); lapply(d,as.character)}").asList();
    LOGGER.debug(list.toString());
    assertNotNull(list);
    for (Object object : list) {
      if (object instanceof REXPString) {
        REXPString rexpString = (REXPString) object;
        // Check if 10 elements have been received within the REXPString object
        assertNotNull(rexpString);
        assertEquals(10, rexpString.length());
        // Check the value of the objects
        if (object.equals(list.firstElement())) {
          String[] value = rexpString.asStrings();
          for (String string : value) {
            assertNotNull(string);
            assertEquals("huhu", string);
          }
        } else if (object.equals(list.lastElement())) {
          String[] numbers = rexpString.asStrings();
          for (String string : numbers) {
            assertNotNull(string);
            assertTrue(11 <= Integer.parseInt(string) 
                    && Integer.parseInt(string) <= 20);
          }
        } else {
          // Fail if there are more than first and last element as result
          fail("There are more elements than expected within the RList object.");
        }
      } else {
        // Fail if the response is other than REXPString
        fail("Could not find object of instance REXPString.");
      }
    }
  }

  @After
  public void tearDownRserve() {
    //TODO: Implement code to shutdown Rserve on loca machine
  }

}
