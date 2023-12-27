package com.xpo.ltl.shipment.service.dao.dto;

import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;


public class ShmHandlingUnitMvmtPKDTO extends ShmHandlingUnitMvmtPK {

    private static final long serialVersionUID = 6140585831279952857L;

    public ShmHandlingUnitMvmtPKDTO(long shpInstId, long seqNbr, long mvmtSeqNbr) {
        super.setShpInstId(shpInstId);
        super.setSeqNbr(seqNbr);
        super.setMvmtSeqNbr(mvmtSeqNbr);
    }

}
