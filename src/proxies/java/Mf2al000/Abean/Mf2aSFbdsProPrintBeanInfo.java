package Mf2al000.Abean;
 
import java.beans.*;
import java.lang.reflect.*;
 
public class Mf2aSFbdsProPrintBeanInfo extends SimpleBeanInfo {
 
   private final static Class rootClass = Mf2aSFbdsProPrint.class;
 
 //  Table of PropertyDescriptors that describe the base class.
 //  Do not expose "import" and "export" methods--these should be
 //  for our use only.  Initialized in the method because the 
 //  PropertyDescriptor new can raise an IntrospectionException.
 
   private PropertyDescriptor [] myProperties = {
      null,   // 0
      null,   // 1
      null,   // 2
      null,   // 3
      null,   // 4
      null,   // 5
      null,   // 6
      null,   // 7
      null,   // 8
      null,   // 9
      null,   // 10
      null,   // 11
      null,   // 12
      null,   // 13
      null,   // 14
      null,   // 15
      null,   // 16
      null,   // 17
      null,   // 18
      null,   // 19
      null,   // 20
      null,   // 21
      null,   // 22
      null,   // 23
      null,   // 24
      null,   // 25
      null,   // 26
      null,   // 27
      null,   // 28
      null,   // 29
      null,   // 30
      null,   // 31
      null,   // 32
      null,   // 33
      null,   // 34
};
 
   public BeanDescriptor getBeanDescriptor() {
      BeanDescriptor bd = 
         new BeanDescriptor(Mf2aSFbdsProPrint.class);
      bd.setDisplayName(Mf2aSFbdsProPrint.BEANNAME);
      return bd;
   }
 
   public PropertyDescriptor [] getPropertyDescriptors() {
      try {
         myProperties[0] = new PropertyDescriptor("tracing", rootClass);
         myProperties[1] = new PropertyDescriptor("serverLocation", rootClass);
         myProperties[2] = new PropertyDescriptor("servletPath", rootClass);
         myProperties[3] = new PropertyDescriptor("commandSent", rootClass);
         myProperties[4] = new PropertyDescriptor("clientId", rootClass);
         myProperties[5] = new PropertyDescriptor("clientPassword", rootClass);
         myProperties[6] = new PropertyDescriptor("nextLocation", rootClass);
         myProperties[7] = new PropertyDescriptor("exitStateSent", rootClass);
         myProperties[8] = new PropertyDescriptor("dialect", rootClass);
         myProperties[9] = new PropertyDescriptor("commandReturned", rootClass, "getCommandReturned", null);
         myProperties[10] = new PropertyDescriptor("exitStateReturned", rootClass, "getExitStateReturned", null);
         myProperties[11] = new PropertyDescriptor("exitStateType", rootClass, "getExitStateType", null);
         myProperties[12] = new PropertyDescriptor("exitStateMsg", rootClass, "getExitStateMsg", null);
         myProperties[13] = new PropertyDescriptor("comCfg", rootClass);
         myProperties[14] = new PropertyDescriptor("InDestSicCdIshs1SharedServicesString3Tx", rootClass);
         myProperties[15] = new PropertyDescriptor("InPrinterCodeIshs1SharedServicesString12Tx", rootClass);
         myProperties[16] = new PropertyDescriptor("InReportTypIshs1SharedServicesString8Tx", rootClass);
         myProperties[17] = new PropertyDescriptor("InFormTypIshs1SharedServicesString8Tx", rootClass);
         myProperties[18] = new PropertyDescriptor("InReprintIndIshs1SharedServicesFlagFg", rootClass);
         myProperties[19] = new PropertyDescriptor("InIncludeProsDestinedIndIshs1SharedServicesFlagFg", rootClass);
         myProperties[20] = new PropertyDescriptor("InIncludeProsNotDestinedIndIshs1SharedServicesFlagFg", rootClass);
         myProperties[21] = new PropertyDescriptor("InValidateIndIshs1SharedServicesFlagFg", rootClass);
         myProperties[22] = new PropertyDescriptor("InDomicileSicCdIshs1SharedServicesString3Tx", rootClass);
         myProperties[23] = new PropertyDescriptor("InCmanGatewaySicIopt1TrlrLoadInstLdDest", rootClass);
         myProperties[24] = new PropertyDescriptor("InClosedTrlrSicGroupIopt1RefUserSicGrpGrpCd", rootClass);
         myProperties[25] = new PropertyDescriptor("InIncludeClosedTrlrFlagIshs1SharedServicesFlagFg", rootClass);
         myProperties[26] = new IndexedPropertyDescriptor("InRPrintRequestKeyIshs1SharedServicesString20Tx", rootClass, null, null, "getInRPrintRequestKeyIshs1SharedServicesString20Tx", "setInRPrintRequestKeyIshs1SharedServicesString20Tx");
         myProperties[27] = new PropertyDescriptor("InControlIshs1SharedServicesLongUserid", rootClass);
         myProperties[28] = new PropertyDescriptor("OutFormTypeLoadedIrtr1PrinterFormTyp", rootClass, "getOutFormTypeLoadedIrtr1PrinterFormTyp", null);
         myProperties[29] = new PropertyDescriptor("OutValueInErrorIshs1SharedServicesString20Tx", rootClass, "getOutValueInErrorIshs1SharedServicesString20Tx", null);
         myProperties[30] = new PropertyDescriptor("OutErrorIshs1SharedServicesReasonCd", rootClass, "getOutErrorIshs1SharedServicesReasonCd", null);
         myProperties[31] = new PropertyDescriptor("OutErrorIshs1SharedServicesReturnCd", rootClass, "getOutErrorIshs1SharedServicesReturnCd", null);
         myProperties[32] = new PropertyDescriptor("OutErrorIshs1SharedServicesContextStringTx", rootClass, "getOutErrorIshs1SharedServicesContextStringTx", null);
         myProperties[33] = new PropertyDescriptor("OutErrorIshs1SharedServicesSubscriptNb", rootClass, "getOutErrorIshs1SharedServicesSubscriptNb", null);
         myProperties[34] = new PropertyDescriptor("OutErrorIshs1SharedServicesOriginServerId", rootClass, "getOutErrorIshs1SharedServicesOriginServerId", null);
         return myProperties;
      } catch (IntrospectionException e) {
         throw new Error(e.toString());
      }
   }
 
   public MethodDescriptor[] getMethodDescriptors() {
//      Method callMethod;
//      Class args[] = { };
// 
//      try {
//         callMethod = Mf2aSFbdsProPrint.class.getMethod("execute", args);
//      } catch (Exception ex) {
//               // "should never happen"
//         throw new Error("Missing method: " + ex);
//      }
// 
//               // Now create the MethodDescriptor array
//               // with visible event response methods:
//      MethodDescriptor result[] = { 
//        new MethodDescriptor(callMethod),
//      };
// 
//      return result;
        return null;
   }
 
//   public java.awt.Image getIcon(int iconKind) {
//      if (iconKind == BeanInfo.ICON_COLOR_16x16) {
//         java.awt.Image img = loadImage("Icon16.gif");
//         return img;
//      }
//      return null;
//   }
}
