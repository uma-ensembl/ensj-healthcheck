/*
 * MyStreamHandlerTest.java
 * NetBeans JUnit based test
 *
 * Created on March 13, 2003, 9:36 AM
 */

package org.ensembl.healthcheck.test;

import junit.framework.*;
import java.util.logging.*;

import org.ensembl.healthcheck.util.*;

/**
 * @version $Revision$
 * @author glenn
 */
public class MyStreamHandlerTest extends TestCase {
  
  private MyStreamHandler msh;
  private LogRecord lr;
  
  public MyStreamHandlerTest(java.lang.String testName) {
    super(testName);
  }
  
  public static void main(java.lang.String[] args) {
    junit.textui.TestRunner.run(suite());
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(MyStreamHandlerTest.class);
    
    return suite;
  }
  
  public void setUp() {
    msh = new MyStreamHandler(System.out, new LogFormatter());
    lr = new LogRecord(Level.INFO, "message");
  }
  
  /** Test of close method, of class org.ensembl.healthcheck.util.MyStreamHandler. */
  public void testClose() {
    System.out.println("testClose");
    
    msh.close(); // ?? how to test this ??
  }
  
  /** Test of flush method, of class org.ensembl.healthcheck.util.MyStreamHandler. */
  public void testFlush() {
    System.out.println("testFlush");

    msh.flush();  // ?? how to test this ??
    
  }
  
  /** Test of publish method, of class org.ensembl.healthcheck.util.MyStreamHandler. */
  public void testPublish() {
    System.out.println("testPublish");
    
    msh.publish(lr);  // ?? how to test this ??
    
  }
   
  public void tearDown() {
  }
  
}
