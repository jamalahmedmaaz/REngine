package org.rosuda.rserve;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.rosuda.rengine.REXP;
import org.rosuda.rengine.REXPDouble;
import org.rosuda.rengine.REXPGenericVector;
import org.rosuda.rengine.REXPInteger;
import org.rosuda.rengine.REXPList;
import org.rosuda.rengine.REXPLogical;
import org.rosuda.rengine.REXPMismatchException;
import org.rosuda.rengine.REXPString;
import org.rosuda.rengine.REngine;
import org.rosuda.rengine.REngineException;
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

  @Test
  public void doubleVectorNaNaNSupportTest() throws REngineException, REXPMismatchException {
    final double r_na = REXPDouble.NA;
    double x[] = {1.0, 0.5, r_na, Double.NaN, 3.5};
    connection.assign("x", x);

    // Check of Na/NaN can be assigned and retrieved
    final String nas = connection.eval("paste(capture.output(print(x)),collapse='\\n')").asString();
    assertNotNull(nas);
    assertEquals("[1] 1.0 0.5  NA NaN 3.5", nas);

    // Check of Na/NaN can be pulled
    final REXP rexp = connection.eval("c(2.2, NA_real_, NaN)");
    assertNotNull(rexp);
    assertTrue(rexp.isNumeric());
    assertFalse(rexp.isInteger());

    // Check if NA/NaN can be pulled
    final boolean nal[] = rexp.isNA();
    assertNotNull(nal);
    assertTrue(nal.length == 3);
    assertFalse(nal[0]);
    assertTrue(nal[1]);
    assertFalse(nal[2]);

    // Check of NA/NAN can be pulled
    x = rexp.asDoubles();
    assertNotNull(x);
    assertTrue(x.length == 3);
    assertTrue(Double.isNaN(x[2]));
    assertFalse(REXPDouble.isNA(x[2]));
    assertTrue(REXPDouble.isNA(x[1]));
  }

  @Test
  public void assignListsAndVectorsTest() throws RserveException, REXPMismatchException, REngineException {
    // Initialize REXP container
    final REXPInteger rexpInteger = new REXPInteger(new int[]{0, 1, 2, 3});
    final REXPDouble rexpDouble = new REXPDouble(new double[]{0.5, 1.2, 2.3, 3.0});
    // Assign REXP container to RList
    final RList list = new RList();
    list.put("a", rexpInteger);
    list.put("b", rexpDouble);
    // Variables to assign List, Vector and DataFrame
    final String[] vars = {"x", "y", "z"};
    // Assign all three varaiables
    connection.assign(vars[0], new REXPList(list));
    connection.assign(vars[1], new REXPGenericVector(list));
    connection.assign(vars[2], REXP.createDataFrame(list));
    // Evaluate result for all assignments
    for (String var : vars) {
      checkListAndVectorsRexpResult(var, list, rexpInteger, rexpDouble);
    }
  }

  private void checkListAndVectorsRexpResult(String var, RList list,
          REXPInteger rexpInteger, REXPDouble rexpDouble) throws REXPMismatchException, REngineException {
    REXP rexp = connection.parseAndEval("x");
    assertNotNull(rexp);
    assertEquals(list.names, rexp.asList().names);
    try {
      REXPInteger a = (REXPInteger) rexp.asList().get("a");
      REXPDouble b = (REXPDouble) rexp.asList().get("b");
      // Check of the result for a corresponds to rexpInteger length
      assertTrue(a.length() == rexpInteger.length());
      assertTrue(b.length() == rexpDouble.length());
      // Iterate and check values
      for (int i = 0; i < rexpInteger.length(); i++) {
        assertEquals(rexpInteger.asIntegers()[i], a.asIntegers()[i]);
      }
    } catch (ClassCastException exception) {
      LOGGER.error(exception.getMessage());
      fail("Could not cast object to the required type.");
    }
  }

  @Test
  public void logicalsSupportTest() throws RserveException, REngineException, REXPMismatchException {
    final REXPLogical rexpLogical = new REXPLogical(new boolean[]{true, false, true});
    connection.assign("b", rexpLogical);
    
    REXP rexp = connection.parseAndEval("b");
    assertNotNull(rexp);
    assertTrue(rexp.isLogical());
    assertEquals(rexpLogical.length(), rexp.length());
    try {
      final boolean[] result = ((REXPLogical) rexp).isTRUE();
      assertTrue(result[0]);
      assertFalse(result[1]);
      assertTrue(result[2]);
    } catch (ClassCastException exception) {
      LOGGER.error(exception.getMessage());
      fail("Could not cast REXP to REPLogical.");
    }
    
    rexp = connection.parseAndEval("c(TRUE,FALSE,NA)");
    assertNotNull(rexp);
    assertTrue(rexp.isLogical());
    assertEquals(rexpLogical.length(), rexp.length());
    // Check result values of rexp.isTRUE()
    boolean result1[] = ((REXPLogical) rexp).isTRUE();
    assertTrue(result1[0]);
    assertFalse(result1[1]);
    assertFalse(result1[2]);
    // Check result values of rexp.isFALSE()
    boolean result2[] = ((REXPLogical) rexp).isFALSE();
    assertFalse(result2[0]);
    assertTrue(result2[1]);
    assertFalse(result2[2]);
    // Check result values of rexp.isNA()
    boolean result3[] = ((REXPLogical) rexp).isNA(); 
    assertFalse(result3[0]);
    assertFalse(result3[1]);
    assertTrue(result3[2]);
  }
  
  @After
  public void tearDownRserve() {
    //TODO: Implement code to shutdown Rserve on loca machine
  }

}
