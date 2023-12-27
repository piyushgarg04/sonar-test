package com.xpo.ltl.shipment.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcpPK;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.ChildProNbrReplacement;
import com.xpo.ltl.api.shipment.v2.ChildShipmentId;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.ReplaceChildProsRqst;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmXdockExcpSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmHandlingUnitDelegate;
import com.xpo.ltl.shipment.service.util.DB2DefaultValueUtil;

public class ReplaceChildProsImplTest {

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

	@Mock
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Mock
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Mock
    private ShmXdockExcpSubDAO shmXdockExcpSubDAO;

    @Mock
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;
    
    @InjectMocks
    private ReplaceChildProsImpl replaceChildProsImpl;

    @Mock
    private ShmHandlingUnitDelegate shmHandlingUnitDelegate;

	@InjectMocks
    private UpdateHandlingUnitsImpl updateHandlingUnitsImpl;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(txnContext.getSrcApplicationId()).thenReturn("JUNIT");

		final User user = new User();
		user.setUserId("JUNIT");
		user.setEmployeeId("JUNIT");
		when(txnContext.getUser()).thenReturn(user);

		when(txnContext.getTransactionTimestamp())
				.thenReturn(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));

		when(txnContext.getCorrelationId()).thenReturn("0");
	}

	@Test
    public void testReplaceChildPros_NoRequest() {
        NullPointerException e = Assertions
            .assertThrows(NullPointerException.class,
                () -> replaceChildProsImpl
                    .replaceChildProsWithNewPros(null, txnContext, entityManager));
                
        Assertions.assertTrue(e.getMessage().contains("ReplaceChildPros Request is required."));

    }
	
    @Test
    public void testWhenTransactionContextIsNull() {
        ReplaceChildProsRqst rqst = new ReplaceChildProsRqst();
        
        try {
            replaceChildProsImpl.replaceChildProsWithNewPros(rqst, null, entityManager);
        } catch (Exception e) {
            assertEquals("The TransactionContext is required.", e.getMessage());
        }
    }

 
    @Test
    public void testWhenEntityManagerIsNull() {
        ReplaceChildProsRqst rqst = new ReplaceChildProsRqst();
        try {
            replaceChildProsImpl.replaceChildProsWithNewPros(rqst, txnContext, null);
        } catch (Exception e) {
            assertEquals("The EntityManager is required.", e.getMessage());
        }
    }
    
    @Test
    public void testReplaceChildProsWithNewPros() throws ServiceException {

        ReplaceChildProsRqst rqst = new ReplaceChildProsRqst();
        
        List<ChildProNbrReplacement> childProNbrReplacements = new ArrayList<>();        
        ChildProNbrReplacement ChildPro = new ChildProNbrReplacement();        
        ChildPro.setCurrentChildProNbr("06481101712"); 
        ChildPro.setNewChildProNbr("06480154231");   
        childProNbrReplacements.add(ChildPro);
        rqst.setChildProNbrReplacements(childProNbrReplacements);
        
        
        ShmHandlingUnit shmHandlingUnit = new ShmHandlingUnit();
        shmHandlingUnit.setChildProNbrTxt(ChildPro.getCurrentChildProNbr());
        shmHandlingUnit.setParentProNbrTxt("06470756465");
        shmHandlingUnit.setMvmtStatCd("1");
        shmHandlingUnit.setCurrentSicCd("NAA");
        shmHandlingUnit.setHandlingMvmtCd("NORMAL");
        ShmHandlingUnitPK id1 = new ShmHandlingUnitPK();
        id1.setShpInstId(101L);
        id1.setSeqNbr(1L);
        shmHandlingUnit.setId(id1);
        
        List<ShmHandlingUnitMvmt> shmHandlingUnitMvmts = new ArrayList<>();
        
        ShmHandlingUnitMvmtPK id = new ShmHandlingUnitMvmtPK();
        id.setShpInstId(shmHandlingUnit.getId().getShpInstId());
        id.setSeqNbr(shmHandlingUnit.getId().getSeqNbr());
        id.setMvmtSeqNbr(1L);

        ShmHandlingUnitMvmt shmHandlingUnitMvmt = new ShmHandlingUnitMvmt();
        shmHandlingUnitMvmt.setId(id);
        shmHandlingUnitMvmt.setShmHandlingUnit(shmHandlingUnit);
        shmHandlingUnitMvmt.setCrteUid("U2345");
        shmHandlingUnitMvmt.setSplitAuthorizeBy(StringUtils.SPACE);
        shmHandlingUnitMvmt.setSplitAuthorizeTmst(
                 DB2DefaultValueUtil.LOW_TMST);
        Timestamp currentTmst = new Timestamp(System.currentTimeMillis());
        shmHandlingUnitMvmt.setCrteTmst(currentTmst);
        shmHandlingUnitMvmt.setMvmtTmst(currentTmst);
        shmHandlingUnitMvmt
            .setMvmtTypCd(true 
                ? HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.REPLACE)
                    : HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.REPLACE));
        shmHandlingUnitMvmt.setArchiveCntlCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanInd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanReason(StringUtils.SPACE);
        shmHandlingUnitMvmt.setDmgdCatgCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setDockInstId(BigDecimal.ZERO);
        shmHandlingUnitMvmt.setExcpTypCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setMvmtRptgSicCd(StringUtils.isNotBlank(shmHandlingUnit.getCurrentSicCd()) ? shmHandlingUnit.getCurrentSicCd() : StringUtils.SPACE);
        shmHandlingUnitMvmt.setRfsdRsnCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setRmrkTxt(StringUtils.SPACE);
        shmHandlingUnitMvmt.setScanTmst(DB2DefaultValueUtil.LOW_TMST);
        shmHandlingUnitMvmt.setTrlrInstId(BigDecimal.ZERO);
        shmHandlingUnitMvmt.setUndlvdRsnCd(StringUtils.SPACE);
        
        shmHandlingUnitMvmts.add(shmHandlingUnitMvmt);
        
        shmHandlingUnit.setShmHandlingUnitMvmts(shmHandlingUnitMvmts);
        
        when(shmHandlingUnitSubDAO.findByChildProNumberList(anySet(), eq(entityManager)))
        .thenReturn(Arrays.asList(shmHandlingUnit));

        replaceChildProsImpl.replaceChildProsWithNewPros(rqst, txnContext,entityManager);
        
        verify(shmHandlingUnitSubDAO, times(1))
        .persist(eq(shmHandlingUnit), eq(entityManager));

        verify(shmHandlingUnitSubDAO, times(1))
            .updateDB2ShmHandlingUnit(eq(shmHandlingUnit), eq(shmHandlingUnit.getLstUpdtTmst()), eq(txnContext),eq(db2EntityManager));
    }
}
