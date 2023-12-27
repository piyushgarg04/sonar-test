package Bo11l000.Abean;
 
import java.beans.*;
import java.lang.reflect.*;
 
public class Bo11SMaintBolDocHanldingBeanInfo extends SimpleBeanInfo {
 
   private final static Class rootClass = Bo11SMaintBolDocHanlding.class;
 
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
};
 
   public BeanDescriptor getBeanDescriptor() {
      BeanDescriptor bd = 
         new BeanDescriptor(Bo11SMaintBolDocHanlding.class);
      bd.setDisplayName(Bo11SMaintBolDocHanlding.BEANNAME);
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
         myProperties[14] = new PropertyDescriptor("InIbol1DocBolInstId", rootClass);
         myProperties[15] = new PropertyDescriptor("InIbol1DocProNbrTxt", rootClass);
         myProperties[16] = new PropertyDescriptor("InIbol1DocCustSuppliedProNbrTxt", rootClass);
         myProperties[17] = new IndexedPropertyDescriptor("InGrpIbol1DocHandlingUnitChildProNbrTxt", rootClass, null, null, "getInGrpIbol1DocHandlingUnitChildProNbrTxt", "setInGrpIbol1DocHandlingUnitChildProNbrTxt");
         myProperties[18] = new PropertyDescriptor("InControlIshs1SharedServicesLongUserid", rootClass);
         myProperties[19] = new PropertyDescriptor("InControlIshs1SharedServicesUserId", rootClass);
         myProperties[20] = new PropertyDescriptor("InControlIshs1SharedServicesDateDt", rootClass);
         myProperties[21] = new PropertyDescriptor("InControlIshs1SharedServicesTimestampTs", rootClass);
         myProperties[22] = new PropertyDescriptor("InControlIshs1SharedServicesTranCd", rootClass);
         myProperties[23] = new PropertyDescriptor("InControlIshs1SharedServicesVersionCheckTs", rootClass);
         myProperties[24] = new IndexedPropertyDescriptor("OutGrpDocHandlingUnitIbol1DocHandlingUnitChildProNbrTxt", rootClass, null, null, "getOutGrpDocHandlingUnitIbol1DocHandlingUnitChildProNbrTxt", null);
         myProperties[25] = new IndexedPropertyDescriptor("OutGrpDocHandlingUnitIbol1DocHandlingUnitBolInstId", rootClass, null, null, "getOutGrpDocHandlingUnitIbol1DocHandlingUnitBolInstId", null);
         myProperties[26] = new IndexedPropertyDescriptor("OutGrpDocHandlingUnitIbol1DocHandlingUnitSeqNbr", rootClass, null, null, "getOutGrpDocHandlingUnitIbol1DocHandlingUnitSeqNbr", null);
         myProperties[27] = new IndexedPropertyDescriptor("OutGrpDocHandlingUnitIbol1DocHandlingUnitCreateDt", rootClass, null, null, "getOutGrpDocHandlingUnitIbol1DocHandlingUnitCreateDt", null);
         myProperties[28] = new PropertyDescriptor("OutErrorIshs1SharedServicesDataStoreStatusCd", rootClass, "getOutErrorIshs1SharedServicesDataStoreStatusCd", null);
         myProperties[29] = new PropertyDescriptor("OutErrorIshs1SharedServicesOriginServerId", rootClass, "getOutErrorIshs1SharedServicesOriginServerId", null);
         myProperties[30] = new PropertyDescriptor("OutErrorIshs1SharedServicesContextStringTx", rootClass, "getOutErrorIshs1SharedServicesContextStringTx", null);
         myProperties[31] = new PropertyDescriptor("OutErrorIshs1SharedServicesReturnCd", rootClass, "getOutErrorIshs1SharedServicesReturnCd", null);
         myProperties[32] = new PropertyDescriptor("OutErrorIshs1SharedServicesReasonCd", rootClass, "getOutErrorIshs1SharedServicesReasonCd", null);
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
//         callMethod = Bo11SMaintBolDocHanlding.class.getMethod("execute", args);
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
