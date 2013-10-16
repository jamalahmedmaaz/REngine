package org.rosuda.rserve;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.rosuda.rengine.REXP;

public class jt {

  public static void main(String[] args) {
    try {
      RConnection c = new RConnection((args.length > 0) ? args[0] : "127.0.0.1");

      BufferedReader ir = new BufferedReader(new InputStreamReader(System.in));
      String s = null;
      System.out.print("> ");
      while ((s = ir.readLine()).length() > 0) {
        if (s.equals("shutdown")) {
          System.out.println("Sending shutdown request");
          c.shutdown();
          System.out.println("Shutdown successful. Quitting console.");
          return;
        } else {
          REXP rx = c.parseAndEval(s);
          System.out.println("result(debug): " + rx.toDebugString());
        }
        System.out.print("> ");
      }

    } catch (RserveException rse) {
      System.out.println(rse);
      /*		} catch (REXPMismatchException mme) {
       System.out.println(mme);
       mme.printStackTrace(); */
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
