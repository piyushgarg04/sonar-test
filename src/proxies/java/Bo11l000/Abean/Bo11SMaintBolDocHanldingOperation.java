package Bo11l000.Abean;
 
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
 
public class Bo11SMaintBolDocHanldingOperation
         implements Serializable
{
    String     className = "Bo11SMaintBolDocHanldingOperation";
    private Bo11SMaintBolDocHanlding        client;
 
    private TranData tranData = new TranData(
                                             "BO11L000",
                                             "Bo11l000",
                                             "BO11",
                                             "BO11_S_MAINT_BOL_DOC_HANLDING",
                                             "Bo11SMaintBolDocHanlding",
                                             "BO11S000",
                                             "XPO BOL010 UD 8.6 BOL MANAGEMENT",
                                             "bolun",
                                              new String [] {"","","","","","","",""},
                                              new String [] {"","","","","","","",""},
                                             "bolun",
                                             "bolun",
                                             "",
                                             "",
                                             "proxy.bol.io",
                                             "com.ca.gen.odc.ITPIPTranEntry",
                                             new String [] {"", "0","Y"});
 
 
    private OutMessage out = new OutMessage();
    private InMessage in = new InMessage();
    private ITranEntry tran;
 
    public Bo11SMaintBolDocHanldingOperation(Bo11SMaintBolDocHanlding client)
    {
        this.client = client;
 
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, className, "new Bo11SMaintBolDocHanldingOperation( client )");
        }
 
    }
 
   // -------------------------------------------------------------------
   // doBo11SMaintBolDocHanldingOperation is called to issue a single request to the
   //  transaction server.
   //
   public void doBo11SMaintBolDocHanldingOperation()
               throws ProxyException, PropertyVetoException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperation", "Entering doBo11SMaintBolDocHanldingOperation routine");
    }
 
 
    // Setup the tran entry data
    tranData.setIImportView(client.importView);
    tranData.setIExportView(client.exportView);
 
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
        Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperation",
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
         Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperation",
            "Successfully performed a cooperative flow, checking results");
      }
 
      if (in.errorOccurred() == true)
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperation",
              "Apparently an error occurred, dumping it.");
 
           Trace.dump(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperation",
              "Returned error number",
              new Integer(in.getError().getNumber()).toString());
 
           Trace.dump(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperation",
              "Returned error message",
              in.getError().toString());
        }
        throw new ProxyException("doBo11SMaintBolDocHanldingOperation", in.getError().toString());
      }
      else
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperation",
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
         Trace.dump(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperation",
            "Received CSUException:", e);
      }
        throw new ProxyException("doBo11SMaintBolDocHanldingOperation", e.getClass().getName() + ": " + e.toString());
    }
  }
 
   // -------------------------------------------------------------------
   // doBo11SMaintBolDocHanldingOperationAsync is called to begin a single request to the
   //  server asynchronously.
   //
   public int doBo11SMaintBolDocHanldingOperationAsync(boolean noResponse) throws ProxyException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationAsync", "Entering doBo11SMaintBolDocHanldingOperationAsync routine");
    }
 
    int result = -1;
 
 
    // Setup the tran entry data
    tranData.setIImportView(client.importView);
 
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
        Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationAsync",
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
 
      result = CoopFlow.coopFlowPollResponse(tran, out, "doBo11SMaintBolDocHanldingOperationAsync", noResponse);
 
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationAsync",
            "Successfully started an asynchronous cooperative flow, checking results, id=" + result);
      }
 
 
      return result;
    }
    catch (CSUException e) {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.dump(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationAsync",
            "Received CSUException:", e);
      }
        throw new ProxyException("doBo11SMaintBolDocHanldingOperationAsync", e.getClass().getName() + ": " + e.toString());
    }
  }
 
   // -------------------------------------------------------------------
   // doBo11SMaintBolDocHanldingOperationGetResponse is called to retrieve the results
   //  of a particular asynchronous cooperative flow.
   //
   public boolean doBo11SMaintBolDocHanldingOperationGetResponse(int id, boolean block)
               throws ProxyException, PropertyVetoException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse", "Entering doBo11SMaintBolDocHanldingOperationGetResponse routine, id= " + id);
    }
 
 
    // Setup the tran entry data
    tranData.setIExportView(client.exportView);
 
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
        Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse",
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
           Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse",
              "GetResponse returned PENDING");
        }
        return false;
      }
 
      if (result == CoopFlow.INVALID_ID)
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse",
              " Illegal identifier given for GetResponse: " + id);
        }
        throw new ProxyException("doBo11SMaintBolDocHanldingOperationGetResponse", " Illegal asynchronous id given in get response call: " + id);
      }
 
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse",
            "Successfully performed a GetResponse on a cooperative flow, checking results");
      }
 
      if (in.errorOccurred() == true)
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse",
              "Apparently an error occurred, dumping it.");
 
           Trace.dump(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse",
              "Returned error number",
              new Integer(in.getError().getNumber()).toString());
 
           Trace.dump(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse",
              "Returned error message",
              in.getError().toString());
        }
        throw new ProxyException("doBo11SMaintBolDocHanldingOperationGetResponse", in.getError().toString());
      }
      else
      {
        if (Trace.isTracing(Trace.MASK_APPLICATION))
        {
           Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse",
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
         Trace.dump(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationGetResponse",
            "Received CSUException:", e);
      }
      throw new ProxyException("doBo11SMaintBolDocHanldingOperationGetResponse", e.getClass().getName() + ": " + e.toString());
    }
  }
 
   // -------------------------------------------------------------------
   // doBo11SMaintBolDocHanldingOperationCheckResponse is called to inquire about the
   //  results of an asynchronous cooperative flow.
   //
   public int doBo11SMaintBolDocHanldingOperationCheckResponse(int id) throws ProxyException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationCheckResponse", "Entering doBo11SMaintBolDocHanldingOperationCheckResponse routine, id= " + id);
    }
 
    try {
      return CoopFlow.coopFlowCheckStatus(id);
    }
    catch (CSUException e) {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.dump(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationCheckResponse",
            "Received CSUException:", e);
      }
      throw new ProxyException("doBo11SMaintBolDocHanldingOperationCheckResponse", e.getClass().getName() + ": " + e.toString());
    }
   }
 
   // -------------------------------------------------------------------
   // doBo11SMaintBolDocHanldingOperationIgnoreResponse is called to inquire that the
   //  indicated asynchronous request is no longer relevant and the
   //  results can be ignored.
   //
   public void doBo11SMaintBolDocHanldingOperationIgnoreResponse(int id) throws ProxyException
   {
    if (Trace.isTracing(Trace.MASK_APPLICATION))
    {
       Trace.record(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationIgnoreResponse", "Entering doBo11SMaintBolDocHanldingOperationIgnoreResponse routine, id= " + id);
    }
 
    try {
      CoopFlow.coopFlowIgnoreResponse(id);
    }
    catch (CSUException e) {
      if (Trace.isTracing(Trace.MASK_APPLICATION))
      {
         Trace.dump(Trace.MASK_APPLICATION, "doBo11SMaintBolDocHanldingOperationIgnoreResponse",
            "Received CSUException:", e);
      }
      throw new ProxyException("doBo11SMaintBolDocHanldingOperationIgnoreResponse", e.getClass().getName() + ": " + e.toString());
    }
   }
}
