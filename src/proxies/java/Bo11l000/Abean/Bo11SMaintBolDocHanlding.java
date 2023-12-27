package Bo11l000.Abean;
 
import java.applet.*;
import java.awt.event.*;
import java.io.*;
import java.beans.*;
import java.util.*;
import java.net.URL;
import java.math.*;
import java.text.*;
import com.ca.gen.jprt.*;
import com.ca.gen.odc.coopflow.*;
import com.ca.gen.odc.msgobj.*;
import com.ca.gen.csu.trace.*;
import com.ca.gen.vwrt.types.*;
 
/**
 * <strong>API bean documentation.</strong><p>
 *
 * This html file contains the public methods available in this bean.<br>
 * NOTE:  the links at the top of this page do not work, as they are not connected to anything. 
 * To get the images in the file to work, copy the images directory 
 * from 'jdk1.1.x/docs/api/images' to the directory where this file is located.
 */
public class Bo11SMaintBolDocHanlding  implements ActionListener, java.io.Serializable  {
 
   //  Use final String for the bean name
   public static final String BEANNAME = new String("Bo11SMaintBolDocHanlding");
 
   //  Constants for Asynchronous status
   public static final int PENDING = CoopFlow.DATA_NOT_READY;
   public static final int AVAILABLE = CoopFlow.DATA_READY;
   public static final int INVALID_ID = CoopFlow.INVALID_ID;
 
   private final SimpleDateFormat nativeTimestampFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
   private final SimpleDateFormat nativeDateFormatter = new SimpleDateFormat("yyyyMMdd");
   private static DecimalFormat decimalFormatter;
 
   public Bo11SMaintBolDocHanlding() {
      super();
      DecimalFormatSymbols symbols = new DecimalFormatSymbols();
      symbols.setDecimalSeparator('.');
      symbols.setGroupingSeparator(',');
      decimalFormatter = new DecimalFormat("###################.###################", symbols);
      nativeDateFormatter.setLenient(false);
      nativeTimestampFormatter.setLenient(false);
      Trace.initialize(null);
   }
   /**
    * Sets the traceflag to tell the bean to output diagnostic messages on stdout.
    *
    * @param traceFlag 1 to turn tracing on, 0 to turn tracing off.
    */
   public void setTracing(int traceFlag) {
      if (traceFlag == 0)
         Trace.setMask(Trace.MASK_NONE);
      else
         Trace.setMask(Trace.MASK_ALL);
   }
   /**
    * Gets the current state of tracing.
    *
    * @return traceFlag value
    */
   public int  getTracing() {
      if (Trace.getMask() == Trace.MASK_NONE)
         return 0;
      else
         return 1;
   }
   protected void traceOut(String x)  {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, BEANNAME, x);
      }
   }
 
 
   private Bo11SMaintBolDocHanldingOperation oper = null;
 
   /**
    * Calls the transaction/procedure step on the server.
    *
    * @exception java.beans.PropertyVetoException
    * Final property checks can throw this exception.
    */
   public void execute()  throws PropertyVetoException {
      try  {
         if (oper == null) {
            oper = new Bo11SMaintBolDocHanldingOperation(this);
            addCompletionListener(new operListener(this));
            addExceptionListener(new operListener(this));
         }
 

 
         oper.doBo11SMaintBolDocHanldingOperation();
         notifyCompletionListeners();
      }
      catch (PropertyVetoException ePve)  {
         throw ePve;
      }
      catch (ProxyException e)  {
         notifyExceptionListeners(e.toString());
         return;
      }
   }
 
 
   private class operListener implements ActionListener, java.io.Serializable  {
      private Bo11SMaintBolDocHanlding rP;
      operListener(Bo11SMaintBolDocHanlding r)  {
         rP = r;
      }
      public void actionPerformed(ActionEvent aEvent)  {
         if (Trace.isTracing(Trace.MASK_APPLICATION))
         {
            Trace.record(Trace.MASK_APPLICATION, "Bo11SMaintBolDocHanlding", "Listener heard that Bo11SMaintBolDocHanldingOperation completed with " + 
               aEvent.getActionCommand());
         }
      }
   }
 
   private Vector completionListeners = new Vector();
   /**
    * Adds an object to the list of listeners that are called when execute has completed.
    *
    * @param l a class object that implements the ActionListener interface.  See the test UI 
    * for an example.
    */
   public synchronized void addCompletionListener(ActionListener l)  {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, BEANNAME, "addCompletionListener registered");
      }
      completionListeners.addElement(l);     //add listeners
   }
   /**
    * Removes the object from the list of listeners that are called after completion of execute.
    *
    * @param l the class object that was registered as a CompletionListener.  See the test UI 
    * for an example.
    */
   public synchronized void removeCompletionListener(ActionListener l)  {
      completionListeners.removeElement(l);  //remove listeners
   }
   private void notifyCompletionListeners()  {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, BEANNAME, "notifying listeners of completion of operation Bo11SMaintBolDocHanlding ()\n");
      }
      Vector targets;
      ActionEvent actionEvt = null;
      synchronized (this)  {
         targets = (Vector) completionListeners.clone();
      }
      actionEvt = new ActionEvent(this, 0, "Completion.Bo11SMaintBolDocHanlding");
      for (int i = 0; targets.size() > i; i++)  {
         ActionListener target = (ActionListener)targets.elementAt(i);
         target.actionPerformed(actionEvt);
      }
   }
 
   private Vector exceptionListeners = new Vector();
   /**
    * Adds an object to the list of listeners that are called when an exception occurs.
    *
    * @param l a class object that implements the ActionListener interface.  See the test UI 
    * for an example.
    */
   public synchronized void addExceptionListener(ActionListener l)  {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, BEANNAME, "addExceptionListener registered");
      }
      exceptionListeners.addElement(l);     //add listeners
   }
   /**
    * Removes the object from the list of listeners that are called when an exception occurs.
    *
    * @param l the class object that was registered as an ExceptionListener.  See the test UI 
    * for an example.
    */
   public synchronized void removeExceptionListener(ActionListener l)  {
      exceptionListeners.removeElement(l);  //remove listeners
   }
   private void notifyExceptionListeners(String s)  {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, BEANNAME, "notifying listeners of exception of operation Bo11SMaintBolDocHanlding ()\n");
      }
      Vector targets;
      ActionEvent actionEvt = null;
      String failCommand = "Exception.Bo11SMaintBolDocHanlding";
      synchronized (this)  {
         targets = (Vector) exceptionListeners.clone();
      }
      if (s.length() > 0)
          failCommand = failCommand.concat(s);
      actionEvt = new ActionEvent(this, 0, failCommand);
      for (int i = 0; targets.size() > i; i++)  {
         ActionListener target = (ActionListener)targets.elementAt(i);
         target.actionPerformed(actionEvt);
      }
   }
 
   /**
    * Called by the sender of Listener events.
    */
   public void actionPerformed(ActionEvent e) {
      String command = e.getActionCommand();
 
      if (command.equals("execute"))  {
         try {
            execute();
         } catch (PropertyVetoException pve) {}
      } else {
         if (Trace.isTracing(Trace.MASK_APPLICATION))
         {
            Trace.record(Trace.MASK_APPLICATION, BEANNAME, "ActionEvent " + e.toString());
         }
      }
   }
 
   //these are the standard properties that are passed into the Server Dialog 
   //all of these are checked when loaded by the operation into the srvdialog class
 
   private String commandSent = "";
   /**
    * Sets the command sent property to be sent to the server where
    * the procedure step's executable code is installed. This property should only be
    * set if the procedure step uses case of command construct.
    *
    * @param s a string representing the command name
    */
   public void setCommandSent(String s) {
      if (s == null) commandSent = "";
      else commandSent = s;
   }
   /**
    * Gets the command sent property to be sent to the server where
    * the procedure step's executable code is installed.
    *
    * @return a string representing the command name
    */
   public String getCommandSent() {
      return commandSent;
   }
 
   private String clientId = "";
   /**
    * Sets the client user id property which will be sent to
    * the server where the procedure step's executable code is installed. A client
    * user id is usually accompanied by a client user password, which can be set
    * with the clientPassword property.  Security is not enabled until the security
    * user exit is modified to enable it.
    *
    * @param s a string representing the client user id
    */
   public void setClientId(String s) {
      if (s == null) clientId = "";
      else clientId = s;
   }
   /**
    * Gets the client user id property which will be sent to
    * the server where the procedure step's executable code is installed. A client
    * user id is usually accompanied by a client user password, which can also be set
    * with the clientPassword property. Security is not enabled until the security
    * user exit is modified to enable it.
    *
    * @return a string representing the client user id
    */
   public String getClientId() {
      return clientId;
   }
 
   private String clientPassword = "";
   /**
    * Sets the client password property which will be sent to
    * the server where the procedure step's executable code is installed. A client
    * password usually accompanies a client user id, which can be set
    * with the clientId property. Security is not enabled until the security
    * user exit is modified to enable it.
    *
    * @param s a string representing the client password
    */
   public void setClientPassword(String s) {
      if (s == null) clientPassword = "";
      else  clientPassword = s;
   }
   /**
    * Gets the client password property which will be sent to
    * the server where the procedure step's executable code is installed. A client
    * password usually accompanies a client user id, which can be set
    * with the clientId property. Security is not enabled until the security
    * user exit is modified to enable it.
    *
    * @return a string representing the client password
    */
   public String getClientPassword() {
      return clientPassword;
   }
 
   private String nextLocation = "";
   /**
    * Sets the location name (NEXTLOC) property that may be
    * used by ODC user exit flow dlls.
    *
    * @param s a string representing the NEXTLOC value
    */
   public void setNextLocation(String s) {
      if (s == null) nextLocation = "";
      else nextLocation = s;
   }
   /**
    * Gets the location name (NEXTLOC) property that may be
    * used by ODC user exit flow dlls.
    *
    * @return a string representing the NEXTLOC value
    */
   public String getNextLocation() {
      return nextLocation;
   }
 
   private int exitStateSent = 0;
   /**
    * Sets the exit state property which will be sent to server procedure step.
    *
    * @param n an integer representing the exit state value
    */
   public void setExitStateSent(int n) {
      exitStateSent = n;
   }
   /**
    * Gets the exit state property which will be sent to server procedure step.
    *
    * @return an integer representing the exit state value
    */
   public int getExitStateSent() {
      return exitStateSent;
   }
 
   private String dialect = "DEFAULT";
   /**
    * Sets the dialect property.  It has the default value of "DEFAULT".
    *
    * @param s a string representing the dialect value
    */
   public void setDialect(String s) {
      if (s == null) dialect = "DEFAULT";
      else dialect = s;
   }
   /**
    * Gets the dialect property.  It has the default value of "DEFAULT".
    *
    * @return a string representing the dialect value
    */
   public String getDialect() {
      return dialect;
   }
 
   private String commandReturned;
   protected void setCommandReturned(String s) {
      commandReturned = s;
   }
   /**
    * Retrieves the command returned property, if any,
    * after the server procedure step has been executed.
    *
    * @return a string representing the command returned value
    */
   public String getCommandReturned() {
      return commandReturned;
   }
 
   private int exitStateReturned;
   protected void setExitStateReturned(int n) {
      exitStateReturned = n;
   }
   /**
    * Retrieves the exit state returned property, if any,
    * after the server procedure step has been executed.
    *
    * @return a string representing the exit state returned value
    */
   public int getExitStateReturned() {
      return exitStateReturned;
   }
 
   private int exitStateType;
   protected void setExitStateType(int n) {
      exitStateType = n;
   }
   /**
    * Gets the exit state type based upon the server procedure step exit state. 
    *
    * @return a string representing the exit state type value
    */
   public int getExitStateType() {
      return exitStateType;
   }
 
   private String exitStateMsg = "";
   protected void setExitStateMsg(String s) {
      exitStateMsg = s;
   }
   /**
    * Gets the current status text message, if
    * one exists. A status message is associated with a status code, and can
    * be returned by a Gen exit state.
    *
    * @return a string representing the exit state message value
    */
   public String getExitStateMsg() {
      return exitStateMsg;
   }
 
   private String comCfg = "";
   /**
    * Sets the value to be used for communications instead of the information in commcfg.properties.
    * For details on this information, refer to the commcfg.properties file provided.
    *
    * @param s a string containing the communications command value
    */
   public void setComCfg(String s) {
      if (s == null) comCfg = "";
      else  comCfg = s;
   }
   /**
    * Gets the value to be used for communications instead of the information in commcfg.properties.
    * For details on this information, refer to the commcfg.properties file provided.
    *
    * @return a string containing the communications command value
    */
   public String getComCfg() {
      return comCfg;
   }
 
   private URL serverLocation;
   /**
    * Sets the URL used to locate the servlet.  Set this property by calling
    * myObject.setServerLocation( getDocumentBase()) from your applet
    * or, force a server connection by using<br>
    * <code>try {new URL("http://localhost:80");} catch(MalformedURLException e) {}</code>
    * 
    * @param newServerLoc a URL containing the base URL for the servlet
    */
   public void setServerLocation(URL newServerLoc) {
      serverLocation = newServerLoc;
   }
   /**
    * Gets the URL used to locate the servlet.
    * 
    * @return a URL containing the base URL for the servlet
    */
   public URL getServerLocation() {
      return serverLocation;
   }
 
   private String servletPath = "";
   /**
    * Sets the URL path to be used to locate the servlet.  Set this property by calling
    * myObject.setServletPath( ... ) from your applet.
    * If the servletPath is left blank, then a default path
    * of "servlet" will be used.
    * 
    * @param newServletPath a String containing the URL path for the servlet
    */
   public void setServletPath(String newServletPath) {
      if (newServletPath == null) servletPath = "";
      else servletPath = newServletPath;
   }
   /**
    * Gets the URL path that will be used to locate the servlet.
    * 
    * @return a String containing the URL path for the servlet
    */
   public String getServletPath() {
      return servletPath;
   }
 
   private String fileEncoding = "";
   /**
    * Sets the file encoding to be used for converting to/from UNICODE.
    *
    * @param s a string containing the file encoding value
    */
   public void setFileEncoding(String s) {
      if (s == null) fileEncoding = "";
      else  fileEncoding = s;
   }
   /**
    * Gets the file encoding that will be used for converting to/from UNICODE.
    *
    * @return a string the file encoding value
    */
   public String getFileEncoding() {
      return fileEncoding;
   }
 
   //  Property interfaces
   //     (names include full predicate viewname)
   //  get...  for each output predicate view
   //  set...  for each input predicate view
   //  both for input-output views
   //  export (set and fire pcs) for input-output and output views
 
   // support notifying bound properties when a attribute value changes
   // see pcs changes below
   protected transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
   /**
    * Adds an object to the list of listeners that are called when a property value changes.
    *
    * @param l a class object that implements the PropertyChangeListener interface.  See the test UI 
    * for an example.
    */
   public void addPropertyChangeListener (PropertyChangeListener l)
                           { pcs.addPropertyChangeListener (l);    }
   /**
    * Removes the object from the list of listeners that are called when a property value changes.
    *
    * @param l the class object that was registered as a PropertyChangeListener.  See the test UI 
    * for an example.
    */
   public void removePropertyChangeListener (PropertyChangeListener l)
                           { pcs.removePropertyChangeListener (l);    }
 
   /**
    * This method clears all import and export attribute properties. The
    * default value is used if one is specified in the model otherwise 0 is used
    * for numeric, date and time attributes, an empty string is used for string attributes
    * and "00000000000000000000" is used for timestamp attributes. For attributes in repeating
    * groups, the clear method sets the repeat count to 0. In addition to clearing
    * attribute properties, the <code>clear</code> method also clears the system properties
    * commandReturned, exitStateReturned, and exitStateMsg.
    *
    * @exception java.beans.PropertyVetoException
    * This is needed to cover the setXXXX calls used in the function.
    */
   public void clear()  throws PropertyVetoException  {
      setCommandReturned("");
      setExitStateReturned(0);
      setExitStateMsg("");
 
      importView.reset();
      exportView.reset();
      importView.InGroup_MA = IntAttr.valueOf(InGroupMax);
      exportView.OutGroupDocHandlingUnits_MA = IntAttr.getDefaultValue();
   }

   proxy.bol.io.BO11S000_IA importView = proxy.bol.io.BO11S000_IA.getInstance();
   proxy.bol.io.BO11S000_OA exportView = proxy.bol.io.BO11S000_OA.getInstance();
   public double getInIbol1DocBolInstId() {
      return importView.InIbol1DocBolInstId;
   }
   public void setInIbol1DocBolInstId(double s)
      throws PropertyVetoException {
      int decimals = 0;
      boolean decimal_found = false;
      String tempDataStr = decimalFormatter.format(s);
      for (int i=tempDataStr.length(); i>0; i--) {
         if (tempDataStr.charAt(i-1) == '.') {
            decimal_found = true;
            break;
         }
         decimals++;
      }
      if (decimal_found == true && decimals > 0) {
         throw new PropertyVetoException("InIbol1DocBolInstId has more than 0 fractional digits.",
               new PropertyChangeEvent (this, "InIbol1DocBolInstId", null, null));
      }
      if (java.lang.Math.abs(s) >= 1000000000000000.0) {
         throw new PropertyVetoException("InIbol1DocBolInstId has more than 15 integral digits.",
               new PropertyChangeEvent (this, "InIbol1DocBolInstId", null, null));
      }
      importView.InIbol1DocBolInstId = DoubleAttr.valueOf(s);
   }
   public void setAsStringInIbol1DocBolInstId(String s)
      throws PropertyVetoException {
      if (s == null || s.length() == 0) {
          throw new PropertyVetoException("InIbol1DocBolInstId is not a valid numeric value: " + s,
                                          new PropertyChangeEvent (this, "InIbol1DocBolInstId", null, null));
      }
      try {
          setInIbol1DocBolInstId(new Double(s).doubleValue() );
      } catch (NumberFormatException e) {
          throw new PropertyVetoException("InIbol1DocBolInstId is not a valid numeric value: " + s,
                                          new PropertyChangeEvent (this, "InIbol1DocBolInstId", null, null));
      }
   }
 
   public String getInIbol1DocProNbrTxt() {
      return FixedStringAttr.valueOf(importView.InIbol1DocProNbrTxt, 11);
   }
   public void setInIbol1DocProNbrTxt(String s)
      throws PropertyVetoException {
      if (s.length() > 11) {
         throw new PropertyVetoException("InIbol1DocProNbrTxt must be <= 11 characters.",
               new PropertyChangeEvent (this, "InIbol1DocProNbrTxt", null, null));
      }
      importView.InIbol1DocProNbrTxt = FixedStringAttr.valueOf(s, (short)11);
   }
 
   public String getInIbol1DocCustSuppliedProNbrTxt() {
      return FixedStringAttr.valueOf(importView.InIbol1DocCustSuppliedProNbrTxt, 11);
   }
   public void setInIbol1DocCustSuppliedProNbrTxt(String s)
      throws PropertyVetoException {
      if (s.length() > 11) {
         throw new PropertyVetoException("InIbol1DocCustSuppliedProNbrTxt must be <= 11 characters.",
               new PropertyChangeEvent (this, "InIbol1DocCustSuppliedProNbrTxt", null, null));
      }
      importView.InIbol1DocCustSuppliedProNbrTxt = FixedStringAttr.valueOf(s, (short)11);
   }
 
   public final int InGroupMax = 200;
   public short getInGroupCount() {
      return (short)(importView.InGroup_MA);
   };
 
   public void setInGroupCount(short s) throws PropertyVetoException {
      if (s < 0 || s > InGroupMax) {
         throw new PropertyVetoException("InGroupCount value is not a valid value. (0 to 200)",
               new PropertyChangeEvent (this, "InGroupCount", null, null));
      } else {
         importView.InGroup_MA = IntAttr.valueOf((int)s);
      }
   }
 
   public String getInGrpIbol1DocHandlingUnitChildProNbrTxt(int index) throws ArrayIndexOutOfBoundsException {
      if (199 < index || index < 0) {
         throw new ArrayIndexOutOfBoundsException("index range must be from 0 to 199, not: " + index);
      }
      return FixedStringAttr.valueOf(importView.InGrpIbol1DocHandlingUnitChildProNbrTxt[index], 11);
   }
   public void setInGrpIbol1DocHandlingUnitChildProNbrTxt(int index, String s)
      throws ArrayIndexOutOfBoundsException, PropertyVetoException {
      if (199 < index || index < 0) {
         throw new ArrayIndexOutOfBoundsException("index range must be from 0 to 199, not: " + index);
      }
      if (s.length() > 11) {
         throw new PropertyVetoException("InGrpIbol1DocHandlingUnitChildProNbrTxt must be <= 11 characters.",
               new PropertyChangeEvent (this, "InGrpIbol1DocHandlingUnitChildProNbrTxt", null, null));
      }
      importView.InGrpIbol1DocHandlingUnitChildProNbrTxt[index] = FixedStringAttr.valueOf(s, (short)11);
   }
 
   public String getInControlIshs1SharedServicesLongUserid() {
      return FixedStringAttr.valueOf(importView.InControlIshs1SharedServicesLongUserid, 15);
   }
   public void setInControlIshs1SharedServicesLongUserid(String s)
      throws PropertyVetoException {
      if (s.length() > 15) {
         throw new PropertyVetoException("InControlIshs1SharedServicesLongUserid must be <= 15 characters.",
               new PropertyChangeEvent (this, "InControlIshs1SharedServicesLongUserid", null, null));
      }
      importView.InControlIshs1SharedServicesLongUserid = FixedStringAttr.valueOf(s, (short)15);
   }
 
   public String getInControlIshs1SharedServicesUserId() {
      return FixedStringAttr.valueOf(importView.InControlIshs1SharedServicesUserId, 8);
   }
   public void setInControlIshs1SharedServicesUserId(String s)
      throws PropertyVetoException {
      if (s.length() > 8) {
         throw new PropertyVetoException("InControlIshs1SharedServicesUserId must be <= 8 characters.",
               new PropertyChangeEvent (this, "InControlIshs1SharedServicesUserId", null, null));
      }
      importView.InControlIshs1SharedServicesUserId = FixedStringAttr.valueOf(s, (short)8);
   }
 
   public Calendar getInControlIshs1SharedServicesDateDt() {
      return DateAttr.toCalendar(importView.InControlIshs1SharedServicesDateDt);
   }
   public int getAsIntInControlIshs1SharedServicesDateDt() {
      return DateAttr.toInt(importView.InControlIshs1SharedServicesDateDt);
   }
   public void setInControlIshs1SharedServicesDateDt(Calendar s)
      throws PropertyVetoException {
      importView.InControlIshs1SharedServicesDateDt = DateAttr.valueOf(s);
   }
   public void setAsStringInControlIshs1SharedServicesDateDt(String s)
      throws PropertyVetoException {
      if (s == null || s.length() == 0) {
         setInControlIshs1SharedServicesDateDt((Calendar)null);
      } else {
         Calendar tempCalendar = Calendar.getInstance();
         try {
            tempCalendar.setTime(nativeDateFormatter.parse(s.length() > 8 ? s.substring(0, 8) : s));
            setInControlIshs1SharedServicesDateDt(tempCalendar);
         } catch (ParseException e) {
            throw new PropertyVetoException("InControlIshs1SharedServicesDateDt has an invalid format (yyyyMMdd).",
                  new PropertyChangeEvent (this, "InControlIshs1SharedServicesDateDt", null, null));
         }
      }
   }
   public void setAsIntInControlIshs1SharedServicesDateDt(int s)
      throws PropertyVetoException {
      String temp = Integer.toString(s);
      if (temp.length() < 8)
      {
         temp = "00000000".substring(temp.length()) + temp;
      }
      setAsStringInControlIshs1SharedServicesDateDt(temp);
   }
 
   public Calendar getInControlIshs1SharedServicesTimestampTs() {
      return TimestampAttr.toCalendar(importView.InControlIshs1SharedServicesTimestampTs);
   }
   public String getAsStringInControlIshs1SharedServicesTimestampTs() {
      return TimestampAttr.toString(importView.InControlIshs1SharedServicesTimestampTs);
   }
   public void setInControlIshs1SharedServicesTimestampTs(Calendar s)
      throws PropertyVetoException {
      importView.InControlIshs1SharedServicesTimestampTs = TimestampAttr.valueOf(s);
   }
   public void setAsStringInControlIshs1SharedServicesTimestampTs(String s)
      throws PropertyVetoException {
      if (s == null || s.length() == 0 || s.matches("[0]*")) {
         setInControlIshs1SharedServicesTimestampTs((Calendar)null);
      } else {
         Calendar tempCalendar = Calendar.getInstance();
         try {
            tempCalendar.setTime(nativeTimestampFormatter.parse(s.length() > 17 ? s.substring(0, 17) : s));
            importView.InControlIshs1SharedServicesTimestampTs = TimestampAttr.valueOf(s);
         } catch (ParseException e) {
            throw new PropertyVetoException("InControlIshs1SharedServicesTimestampTs has an invalid format (yyyyMMddHHmmssSSSSSS).",
                  new PropertyChangeEvent (this, "InControlIshs1SharedServicesTimestampTs", null, null));
         }
      }
   }
 
   public String getInControlIshs1SharedServicesTranCd() {
      return FixedStringAttr.valueOf(importView.InControlIshs1SharedServicesTranCd, 8);
   }
   public void setInControlIshs1SharedServicesTranCd(String s)
      throws PropertyVetoException {
      if (s != null) {
          s = s.toUpperCase();
      }
      if (s.length() > 8) {
         throw new PropertyVetoException("InControlIshs1SharedServicesTranCd must be <= 8 characters.",
               new PropertyChangeEvent (this, "InControlIshs1SharedServicesTranCd", null, null));
      }
      importView.InControlIshs1SharedServicesTranCd = FixedStringAttr.valueOf(s, (short)8);
   }
 
   public Calendar getInControlIshs1SharedServicesVersionCheckTs() {
      return TimestampAttr.toCalendar(importView.InControlIshs1SharedServicesVersionCheckTs);
   }
   public String getAsStringInControlIshs1SharedServicesVersionCheckTs() {
      return TimestampAttr.toString(importView.InControlIshs1SharedServicesVersionCheckTs);
   }
   public void setInControlIshs1SharedServicesVersionCheckTs(Calendar s)
      throws PropertyVetoException {
      importView.InControlIshs1SharedServicesVersionCheckTs = TimestampAttr.valueOf(s);
   }
   public void setAsStringInControlIshs1SharedServicesVersionCheckTs(String s)
      throws PropertyVetoException {
      if (s == null || s.length() == 0 || s.matches("[0]*")) {
         setInControlIshs1SharedServicesVersionCheckTs((Calendar)null);
      } else {
         Calendar tempCalendar = Calendar.getInstance();
         try {
            tempCalendar.setTime(nativeTimestampFormatter.parse(s.length() > 17 ? s.substring(0, 17) : s));
            importView.InControlIshs1SharedServicesVersionCheckTs = TimestampAttr.valueOf(s);
         } catch (ParseException e) {
            throw new PropertyVetoException("InControlIshs1SharedServicesVersionCheckTs has an invalid format (yyyyMMddHHmmssSSSSSS).",
                  new PropertyChangeEvent (this, "InControlIshs1SharedServicesVersionCheckTs", null, null));
         }
      }
   }
 
   public final int OutGroupDocHandlingUnitsMax = 200;
   public short getOutGroupDocHandlingUnitsCount() {
      return (short)(exportView.OutGroupDocHandlingUnits_MA);
   };
 
   public String getOutGrpDocHandlingUnitIbol1DocHandlingUnitChildProNbrTxt(int index) throws ArrayIndexOutOfBoundsException {
      if (199 < index || index < 0) {
         throw new ArrayIndexOutOfBoundsException("index range must be from 0 to 199, not: " + index);
      }
      return FixedStringAttr.valueOf(exportView.OutGrpDocHandlingUnitIbol1DocHandlingUnitChildProNbrTxt[index], 11);
   }
 
   public double getOutGrpDocHandlingUnitIbol1DocHandlingUnitBolInstId(int index) throws ArrayIndexOutOfBoundsException {
      if (199 < index || index < 0) {
         throw new ArrayIndexOutOfBoundsException("index range must be from 0 to 199, not: " + index);
      }
      return exportView.OutGrpDocHandlingUnitIbol1DocHandlingUnitBolInstId[index];
   }
 
   public short getOutGrpDocHandlingUnitIbol1DocHandlingUnitSeqNbr(int index) throws ArrayIndexOutOfBoundsException {
      if (199 < index || index < 0) {
         throw new ArrayIndexOutOfBoundsException("index range must be from 0 to 199, not: " + index);
      }
      return exportView.OutGrpDocHandlingUnitIbol1DocHandlingUnitSeqNbr[index];
   }
 
   public Calendar getOutGrpDocHandlingUnitIbol1DocHandlingUnitCreateDt(int index) throws ArrayIndexOutOfBoundsException {
      if (199 < index || index < 0) {
         throw new ArrayIndexOutOfBoundsException("index range must be from 0 to 199, not: " + index);
      }
      return DateAttr.toCalendar(exportView.OutGrpDocHandlingUnitIbol1DocHandlingUnitCreateDt[index]);
   }
   public int getAsIntOutGrpDocHandlingUnitIbol1DocHandlingUnitCreateDt(int index) throws ArrayIndexOutOfBoundsException {
      if (199 < index || index < 0) {
         throw new ArrayIndexOutOfBoundsException("index range must be from 0 to 199, not: " + index);
      }
      return DateAttr.toInt(exportView.OutGrpDocHandlingUnitIbol1DocHandlingUnitCreateDt[index]);
   }
 
   public String getOutErrorIshs1SharedServicesDataStoreStatusCd() {
      return FixedStringAttr.valueOf(exportView.OutErrorIshs1SharedServicesDataStoreStatusCd, 1);
   }
 
   public double getOutErrorIshs1SharedServicesOriginServerId() {
      return exportView.OutErrorIshs1SharedServicesOriginServerId;
   }
 
   public String getOutErrorIshs1SharedServicesContextStringTx() {
      return StringAttr.valueOf(exportView.OutErrorIshs1SharedServicesContextStringTx);
   }
 
   public int getOutErrorIshs1SharedServicesReturnCd() {
      return exportView.OutErrorIshs1SharedServicesReturnCd;
   }
 
   public int getOutErrorIshs1SharedServicesReasonCd() {
      return exportView.OutErrorIshs1SharedServicesReasonCd;
   }
 
};
