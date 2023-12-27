package com.xpo.ltl.shipment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementExceptionTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementExceptionTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

public class UpdateHandlingUnitsAsAdminImplTest {

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

    @Mock
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

	@InjectMocks
    private UpdateHandlingUnitsAsAdminImpl updateHandlingUnitsAsAdminImpl;

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
    public void testCreateShmHUMovement_STAGE_SPLIT() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_DOCK;
        Boolean oldSplitInd = false;
        Boolean newSplitInd = true;
        String dockName = "DN1";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(2)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(2)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.STAGE), arg.getAllValues().get(0).getMvmtTypCd());
        assertEquals(3L, arg.getAllValues().get(0).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(0).getExcpTypCd());
        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.SPLIT), arg.getAllValues().get(1).getMvmtTypCd());
        assertEquals(4L, arg.getAllValues().get(1).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(1).getExcpTypCd());
    }



    @Test
    public void testCreateShmHUMovement_UNSPLIT_STAGE() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_DOCK;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = false;
        String dockName = "DN1";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(2)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(2)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.UNSPLIT), arg.getAllValues().get(0).getMvmtTypCd());
        assertEquals(3L, arg.getAllValues().get(0).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(0).getExcpTypCd());
        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.STAGE), arg.getAllValues().get(1).getMvmtTypCd());
        assertEquals(4L, arg.getAllValues().get(1).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(1).getExcpTypCd());
    }

    @Test
    public void testCreateShmHUMovement_STAGE() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_DOCK;
        Boolean oldSplitInd = false;
        Boolean newSplitInd = false;
        String dockName = "DN1";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1))
            .createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.STAGE), arg.getValue().getMvmtTypCd());
        assertEquals(3L, arg.getValue().getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getValue().getExcpTypCd());

    }

    @Ignore
    @Test
    public void testCreateShmHUMovement_UNLOAD_STAGE() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_DOCK;
        Boolean oldSplitInd = false;
        Boolean newSplitInd = true;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.UNLOAD), arg.getAllValues().get(0).getMvmtTypCd());
        assertEquals(3L, arg.getAllValues().get(0).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(0).getExcpTypCd());
        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.SPLIT), arg.getAllValues().get(1).getMvmtTypCd());
        assertEquals(4L, arg.getAllValues().get(1).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(1).getExcpTypCd());
    }

    @Ignore
    @Test
    public void testCreateShmHUMovement_UNSPLIT_UNLOAD() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_DOCK;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = false;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.UNSPLIT), arg.getAllValues().get(0).getMvmtTypCd());
        assertEquals(3L, arg.getAllValues().get(0).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(0).getExcpTypCd());
        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.UNLOAD), arg.getAllValues().get(1).getMvmtTypCd());
        assertEquals(4L, arg.getAllValues().get(1).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(1).getExcpTypCd());
    }

    @Ignore
    @Test
    public void testCreateShmHUMovement_UNLOAD() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_DOCK;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = true;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.UNLOAD), arg.getValue().getMvmtTypCd());
        assertEquals(3L, arg.getValue().getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getValue().getExcpTypCd());
    }

    @Ignore
    @Test
    public void testCreateShmHUMovement_fromMissingToNormal_SPLIT_LOAD() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_TRAILER;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = true;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.SPLIT), arg.getAllValues().get(0).getMvmtTypCd());
        assertEquals(3L, arg.getAllValues().get(0).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(0).getExcpTypCd());
        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.LOAD), arg.getAllValues().get(1).getMvmtTypCd());
        assertEquals(4L, arg.getAllValues().get(1).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(1).getExcpTypCd());
    }

    @Ignore
    @Test
    public void testCreateShmHUMovement_splitIndHasChanged_SPLIT_LOAD() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_TRAILER;
        Boolean oldSplitInd = false;
        Boolean newSplitInd = true;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.SPLIT), arg.getAllValues().get(0).getMvmtTypCd());
        assertEquals(3L, arg.getAllValues().get(0).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(0).getExcpTypCd());
        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.LOAD), arg.getAllValues().get(1).getMvmtTypCd());
        assertEquals(4L, arg.getAllValues().get(1).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(1).getExcpTypCd());
    }

    @Ignore
    @Test
    public void testCreateShmHUMovement_UNSPLIT_LOAD() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_TRAILER;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = false;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.UNSPLIT), arg.getAllValues().get(0).getMvmtTypCd());
        assertEquals(3L, arg.getAllValues().get(0).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(0).getExcpTypCd());
        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.LOAD), arg.getAllValues().get(1).getMvmtTypCd());
        assertEquals(4L, arg.getAllValues().get(1).getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getAllValues().get(1).getExcpTypCd());
    }

    @Ignore
    @Test
    public void testCreateShmHUMovement_LOAD() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_TRAILER;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = true;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.LOAD), arg.getValue().getMvmtTypCd());
        assertEquals(3L, arg.getValue().getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getValue().getExcpTypCd());
    }

    @Test
    public void testCreateShmHUMovement_FINAL_DLVD() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.FINAL_DLVD;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = true;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.DELIVER), arg.getValue().getMvmtTypCd());
        assertEquals(3L, arg.getValue().getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getValue().getExcpTypCd());
    }

    @Test
    public void testCreateShmHUMovement_DOCK_OP_EXC() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.NOT_APPLICABLE;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.CANCELLED;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = true;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.DOCK_OPERATIONS_EXCEPTION),
            arg.getValue().getMvmtTypCd());
        assertEquals(3L, arg.getValue().getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getValue().getExcpTypCd());
    }

    @Test
    public void testCreateShmHUMovement_DELIVER_wException() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.ON_TRAILER;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.INTERIM_DLVRY;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = true;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.DELIVER),
            arg.getValue().getMvmtTypCd());
        assertEquals(3L, arg.getValue().getId().getMvmtSeqNbr());
        assertEquals(MovementExceptionTypeCdTransformer.toCode(MovementExceptionTypeCd.SHORT), arg.getValue().getExcpTypCd());
    }

    @Ignore
    @Test
    public void testCreateShmHUMovement_fromMissingToNormal_SPLIT() throws ServiceException {
        String oldMvmtCd = UpdateHandlingUnitsAsAdminImpl.MISSING_MOVEMENT_CD;
        MovementStatusCd oldMvmtStatusCd = MovementStatusCd.ON_TRAILER;
        String newMvmtCd = UpdateHandlingUnitsAsAdminImpl.NORMAL_MOVEMENT_CD;
        MovementStatusCd newMvmtStatusCd = MovementStatusCd.ON_TRAILER;
        Boolean oldSplitInd = true;
        Boolean newSplitInd = true;
        String dockName = "";

        buildMockAndExecuteCreateShmHUMovement(oldMvmtCd, oldMvmtStatusCd, newMvmtCd, newMvmtStatusCd, oldSplitInd, newSplitInd, dockName);

        ArgumentCaptor<ShmHandlingUnitMvmt> arg = ArgumentCaptor.forClass(ShmHandlingUnitMvmt.class);
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(arg.capture(), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(arg.capture(), eq(db2EntityManager));

        assertEquals(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.SPLIT), arg.getValue().getMvmtTypCd());
        assertEquals(3L, arg.getValue().getId().getMvmtSeqNbr());
        assertEquals(StringUtils.SPACE, arg.getValue().getExcpTypCd());

    }

    private void buildMockAndExecuteCreateShmHUMovement(String oldMvmtCd, MovementStatusCd oldMvmtStatusCd, String newMvmtCd,
        MovementStatusCd newMvmtStatusCd, Boolean oldSplitInd, Boolean newSplitInd, String dockName) throws ValidationException {
        AuditInfo ai = AuditInfoHelper.getAuditInfo(txnContext);

        HandlingUnit nweHU = new HandlingUnit();
        nweHU.setHandlingMovementCd(newMvmtCd);
        nweHU.setMovementStatusCd(MovementStatusCdTransformer.toCode(newMvmtStatusCd));
        nweHU.setSplitInd(newSplitInd);
        nweHU.setCurrentDockLocation(dockName);

        ShmHandlingUnitPK id1 = new ShmHandlingUnitPK();
        id1.setShpInstId(101L);
        id1.setSeqNbr(1L);
        ShmShipment mockedShipment = new ShmShipment();
        mockedShipment.setShpInstId(101L);
        ShmHandlingUnit mockedHuDB1 = new ShmHandlingUnit();
        mockedHuDB1.setMvmtStatCd(MovementStatusCdTransformer.toCode(oldMvmtStatusCd));
        mockedHuDB1.setHandlingMvmtCd(oldMvmtCd);
        mockedHuDB1.setSplitInd(BasicTransformer.toString(oldSplitInd));
        mockedHuDB1.setShmShipment(mockedShipment);
        mockedHuDB1.setId(id1);

        String requestingSicCd = "UPO";

        ShmHandlingUnitPK shmHandlingUnitPK = new ShmHandlingUnitPK();
        shmHandlingUnitPK.setShpInstId(mockedHuDB1.getId().getShpInstId());
        shmHandlingUnitPK.setSeqNbr(mockedHuDB1.getId().getSeqNbr());
        when(shmHandlingUnitMvmtSubDAO
            .getNextMvmtBySeqNbrAndShpInstId(eq(mockedHuDB1.getId().getShpInstId()), eq(mockedHuDB1.getId().getSeqNbr()), eq(entityManager)))
                .thenReturn(3L);
        updateHandlingUnitsAsAdminImpl.createShmHUMovement(nweHU, null, mockedHuDB1, requestingSicCd, ai, entityManager, txnContext);
    }

}
