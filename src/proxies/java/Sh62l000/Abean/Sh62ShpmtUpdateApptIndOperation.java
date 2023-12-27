package Sh62l000.Abean;
 
import com.ca.gen.csu.trace.*;
import com.ca.gen.csu.exception.*;
import com.ca.gen.jprt.*;
import com.ca.gen.odc.*;
import com.ca.gen.odc.msgobj.*;
import com.ca.gen.odc.coopflow.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.Serializable;
import java.util.Vector;
import java.beans.*;
import java.util.*;
import java.math.*;
 
public class Sh62ShpmtUpdateApptIndOperation
         implements Serializable
{
    String     className = "Sh62ShpmtUpdateApptIndOperation";
    private Sh62ShpmtUpdateApptInd        client;
 
    private TranData tranData = new TranData(
                                             "SH62L000",
                                             "Sh62l000",
                                             "SH62",
                                             "SH62_SHPMT_UPDATE_APPT_IND",
                                             "Sh62ShpmtUpdateApptInd",
                                             "SH62S000",
                                             "CTS SHM040 UD 8.0 SHIPMENT",
                                             "shmud",
                                              new String [] {"","","","","","","",""},
                                              new String [] {"","","","","","","",""},
                                             "shmud",
                                             "shmud",
                                             "",
                                             "",
                                             "proxy.shm.io",
                                             "com.ca.gen.odc.ITPIPTranEntry",
                                             new String [] {"", "0","Y"});
 
 
    private OutMessage out = new OutMessage();
    private InMessage in = new InMessage();
    private ITranEntry tran;
 
    public Sh62ShpmtUpdateApptIndOperation(Sh62ShpmtUpdateApptInd client)
    {
        this.client = client;
 
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, className, "new Sh62ShpmtUpdateApptIndOperation( client )");
        }
 
    }
 
   // -------------------------------------------------------------------
   // doSh62ShpmtUpdateApptIndOperation is called to issue a single request to the
   //  transaction server.
   //
   public void doSh62ShpmtUpdateApptIndOperation()
               throws ProxyException, PropertyVetoException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperation", "Entering doSh62ShpmtUpdateApptIndOperation routine");
    }
 
 
    // Setup the tran entry data
    tranData.setIImportView(client.importView);
    tranData.setIExportView(client.exportView);
 
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
        Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperation",
            "About to perform cooperative flow");
    }
 
    try {
      out.clearMessage();
      in.clearMessage();
 
      out.setUserid(client.getClientId());
      out.setPassword(client.getClientPassword());
      out.setCommand(client.getCommandSent());
      out.setDialect(client.getDialect());
      out.setNextLocation(client.getNextLocation());
      out.setExitStateNum(client.getExitStateSent());
 
      tranData.setFileEncoding(client.getFileEncoding());
 
      tran = tranData.getTranEntry(tran, client.getComCfg(), this.getClass().getClassLoader());
 
      CoopFlow.coopFlow(tran, out, in);
 
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperation",
            "Successfully performed a cooperative flow, checking results");
      }
 
      if (in.errorOccurred() == true)
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperation",
              "Apparently an error occurred, dumping it.");
 
           Trace.dump(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperation",
              "Returned error number",
              new Integer(in.getError().getNumber()).toString());
 
           Trace.dump(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperation",
              "Returned error message",
              in.getError().toString());
        }
        throw new ProxyException("doSh62ShpmtUpdateApptIndOperation", in.getError().toString());
      }
      else
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperation",
              "Apparently no error occurred, retrieving returned data.");
        }
 
        client.setCommandReturned(in.getCommand());
        client.setExitStateReturned(in.getExitStateNum());
        client.setExitStateType(in.getExitStateType());
        client.setExitStateMsg(in.getExitStateMessage());
 
 
      }
    }
    catch (CSUException e) {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.dump(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperation",
            "Received CSUException:", e);
      }
        throw new ProxyException("doSh62ShpmtUpdateApptIndOperation", e.getClass().getName() + ": " + e.toString());
    }
  }
 
   // -------------------------------------------------------------------
   // doSh62ShpmtUpdateApptIndOperationAsync is called to begin a single request to the
   //  server asynchronously.
   //
   public int doSh62ShpmtUpdateApptIndOperationAsync(boolean noResponse) throws ProxyException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationAsync", "Entering doSh62ShpmtUpdateApptIndOperationAsync routine");
    }
 
    int result = -1;
 
 
    // Setup the tran entry data
    tranData.setIImportView(client.importView);
 
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
        Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationAsync",
            "About to perform asynchronous cooperative flow");
    }
 
    try {
      out.clearMessage();
 
      out.setUserid(client.getClientId());
      out.setPassword(client.getClientPassword());
      out.setCommand(client.getCommandSent());
      out.setDialect(client.getDialect());
      out.setNextLocation(client.getNextLocation());
      out.setExitStateNum(client.getExitStateSent());
 
      tranData.setFileEncoding(client.getFileEncoding());
 
      tran = tranData.getTranEntry(tran, client.getComCfg(), this.getClass().getClassLoader());
 
      result = CoopFlow.coopFlowPollResponse(tran, out, "doSh62ShpmtUpdateApptIndOperationAsync", noResponse);
 
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationAsync",
            "Successfully started an asynchronous cooperative flow, checking results, id=" + result);
      }
 
 
      return result;
    }
    catch (CSUException e) {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.dump(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationAsync",
            "Received CSUException:", e);
      }
        throw new ProxyException("doSh62ShpmtUpdateApptIndOperationAsync", e.getClass().getName() + ": " + e.toString());
    }
  }
 
   // -------------------------------------------------------------------
   // doSh62ShpmtUpdateApptIndOperationGetResponse is called to retrieve the results
   //  of a particular asynchronous cooperative flow.
   //
   public boolean doSh62ShpmtUpdateApptIndOperationGetResponse(int id, boolean block)
               throws ProxyException, PropertyVetoException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse", "Entering doSh62ShpmtUpdateApptIndOperationGetResponse routine, id= " + id);
    }
 
 
    // Setup the tran entry data
    tranData.setIExportView(client.exportView);
 
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
        Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse",
            "About to retrieve asynchronous results for a cooperative flow");
    }
 
    try {
      in.clearMessage();
 
      tranData.setFileEncoding(client.getFileEncoding());
 
      tran = tranData.getTranEntry(tran, client.getComCfg(), this.getClass().getClassLoader());
 
      int result = CoopFlow.coopFlowGetResponse(tran, in, id, block);
 
      if (result == CoopFlow.DATA_NOT_READY)
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse",
              "GetResponse returned PENDING");
        }
        return false;
      }
 
      if (result == CoopFlow.INVALID_ID)
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse",
              " Illegal identifier given for GetResponse: " + id);
        }
        throw new ProxyException("doSh62ShpmtUpdateApptIndOperationGetResponse", " Illegal asynchronous id given in get response call: " + id);
      }
 
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse",
            "Successfully performed a GetResponse on a cooperative flow, checking results");
      }
 
      if (in.errorOccurred() == true)
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse",
              "Apparently an error occurred, dumping it.");
 
           Trace.dump(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse",
              "Returned error number",
              new Integer(in.getError().getNumber()).toString());
 
           Trace.dump(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse",
              "Returned error message",
              in.getError().toString());
        }
        throw new ProxyException("doSh62ShpmtUpdateApptIndOperationGetResponse", in.getError().toString());
      }
      else
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse",
              "Apparently no error occurred, retrieving returned data.");
        }
 
        client.setCommandReturned(in.getCommand());
        client.setExitStateReturned(in.getExitStateNum());
        client.setExitStateType(in.getExitStateType());
        client.setExitStateMsg(in.getExitStateMessage());
 
 
       return true;
      }
    }
    catch (CSUException e) {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.dump(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationGetResponse",
            "Received CSUException:", e);
      }
      throw new ProxyException("doSh62ShpmtUpdateApptIndOperationGetResponse", e.getClass().getName() + ": " + e.toString());
    }
  }
 
   // -------------------------------------------------------------------
   // doSh62ShpmtUpdateApptIndOperationCheckResponse is called to inquire about the
   //  results of an asynchronous cooperative flow.
   //
   public int doSh62ShpmtUpdateApptIndOperationCheckResponse(int id) throws ProxyException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationCheckResponse", "Entering doSh62ShpmtUpdateApptIndOperationCheckResponse routine, id= " + id);
    }
 
    try {
      return CoopFlow.coopFlowCheckStatus(id);
    }
    catch (CSUException e) {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.dump(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationCheckResponse",
            "Received CSUException:", e);
      }
      throw new ProxyException("doSh62ShpmtUpdateApptIndOperationCheckResponse", e.getClass().getName() + ": " + e.toString());
    }
   }
 
   // -------------------------------------------------------------------
   // doSh62ShpmtUpdateApptIndOperationIgnoreResponse is called to inquire that the
   //  indicated asynchronous request is no longer relevant and the
   //  results can be ignored.
   //
   public void doSh62ShpmtUpdateApptIndOperationIgnoreResponse(int id) throws ProxyException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationIgnoreResponse", "Entering doSh62ShpmtUpdateApptIndOperationIgnoreResponse routine, id= " + id);
    }
 
    try {
      CoopFlow.coopFlowIgnoreResponse(id);
    }
    catch (CSUException e) {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.dump(Trace.MASK_APPLICATION, "doSh62ShpmtUpdateApptIndOperationIgnoreResponse",
            "Received CSUException:", e);
      }
      throw new ProxyException("doSh62ShpmtUpdateApptIndOperationIgnoreResponse", e.getClass().getName() + ": " + e.toString());
    }
   }
}
