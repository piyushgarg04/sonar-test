package com.xpo.ltl.shipment.service.delegates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;

@RunWith(MockitoJUnitRunner.class)
public class ShmHandlingUnitDelegateTest {

    @InjectMocks
    private ShmHandlingUnitDelegate shmHandlingUnitDelegate;

    @Test
    public void testCalculateHUPartialInd_NormalToMissing() {
        String huPro1 = "01110111111";
        String huPro2 = "01110111112";

        ShmHandlingUnit shmHu1 = new ShmHandlingUnit();
        ShmHandlingUnit shmHu2 = new ShmHandlingUnit();
        shmHu1.setChildProNbrTxt(huPro1);
        shmHu2.setChildProNbrTxt(huPro2);
        shmHu1.setHandlingMvmtCd("NORMAL");
        shmHu2.setHandlingMvmtCd("NORMAL");
        Map<String, ShmHandlingUnit> dbHuMap = new HashMap<String, ShmHandlingUnit>();
        dbHuMap.put(huPro1, shmHu1);
        dbHuMap.put(huPro2, shmHu2);

        HandlingUnit hu1 = new HandlingUnit();
        HandlingUnit hu2 = new HandlingUnit();
        hu1.setChildProNbr(huPro1);
        hu2.setChildProNbr(huPro2);
        hu1.setHandlingMovementCd("NORMAL");
        hu2.setHandlingMovementCd("MISSING");
        List<HandlingUnit> handlingUnitShipments = new ArrayList<HandlingUnit>();
        handlingUnitShipments.add(hu1);
        handlingUnitShipments.add(hu2);

        String result = shmHandlingUnitDelegate.calculateHUPartialInd(handlingUnitShipments, dbHuMap);
        Assertions.assertEquals("Y", result);

        //Change order and test again.
        hu1.setHandlingMovementCd("MISSING");
        hu2.setHandlingMovementCd("NORMAL");
        String result2 = shmHandlingUnitDelegate.calculateHUPartialInd(handlingUnitShipments, dbHuMap);
        Assertions.assertEquals("Y", result2);

    }

    @Test
    public void testCalculateHUPartialInd_MissingToNormal() {
        String huPro1 = "01110111111";
        String huPro2 = "01110111112";

        ShmHandlingUnit shmHu1 = new ShmHandlingUnit();
        ShmHandlingUnit shmHu2 = new ShmHandlingUnit();
        shmHu1.setChildProNbrTxt(huPro1);
        shmHu2.setChildProNbrTxt(huPro2);
        shmHu1.setHandlingMvmtCd("MISSING");
        shmHu2.setHandlingMvmtCd("NORMAL");
        Map<String, ShmHandlingUnit> dbHuMap = new HashMap<String, ShmHandlingUnit>();
        dbHuMap.put(huPro1, shmHu1);
        dbHuMap.put(huPro2, shmHu2);

        HandlingUnit hu1 = new HandlingUnit();
        HandlingUnit hu2 = new HandlingUnit();
        hu1.setChildProNbr(huPro1);
        hu2.setChildProNbr(huPro2);
        hu1.setHandlingMovementCd("NORMAL");
        hu2.setHandlingMovementCd("NORMAL");
        List<HandlingUnit> handlingUnitShipments = new ArrayList<HandlingUnit>();
        handlingUnitShipments.add(hu1);
        handlingUnitShipments.add(hu2);

        String result = shmHandlingUnitDelegate.calculateHUPartialInd(handlingUnitShipments, dbHuMap);
        Assertions.assertEquals("N", result);

    }

}
