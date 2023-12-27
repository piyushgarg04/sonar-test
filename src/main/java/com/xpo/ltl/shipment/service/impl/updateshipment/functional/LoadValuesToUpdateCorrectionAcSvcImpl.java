package com.xpo.ltl.shipment.service.impl.updateshipment.functional;

import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvcPK;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAcSvc;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LoadValuesToUpdateCorrectionAcSvcImpl extends AbstractAcSvc implements LoadValuesToUpdate<ShmAcSvc, ShmAcSvc> {

	private static void setValues(ShmAcSvc shmAcSvc, ShmAcSvc acSvcFound) {
		if (Objects.nonNull(acSvcFound.getAmt())) {
			shmAcSvc.setAmt(acSvcFound.getAmt());
		}
		if (Objects.nonNull(acSvcFound.getTrfRt())) {
			shmAcSvc.setTrfRt(acSvcFound.getTrfRt());
		}
		if (Objects.nonNull(acSvcFound.getMinChrgInd())) {
			shmAcSvc.setMinChrgInd(acSvcFound.getMinChrgInd());
		}
		if (StringUtils.isNotEmpty(acSvcFound.getDescTxt())) {
			shmAcSvc.setDescTxt(acSvcFound.getDescTxt());
		}

	}

	@Override
	public List<ShmAcSvc> load(
			List<ShmAcSvc> shmAcSvcToInsert, List<ShmAcSvc> shmAcSvcUpdated, boolean fromDelete, String racfId, String tranCode) {

		List<ShmAcSvc> result = new ArrayList<>();

		shmAcSvcToInsert.forEach(shmAcSvc -> {
			Optional<ShmAcSvc> optionalShmAcSvc = shmAcSvcUpdated
					.stream()
					.filter(shmAcSvcToUpdate -> shmAcSvcToUpdate.getId().getSeqNbr() == shmAcSvc.getId().getSeqNbr()
							&& shmAcSvcToUpdate.getId().getShpInstId() == shmAcSvc.getId().getShpInstId())
					.findAny();
			ShmAcSvc shmAcSvcToCheck = new ShmAcSvc();

			copyFields(shmAcSvc, shmAcSvcToCheck);
			if (optionalShmAcSvc.isPresent() && !fromDelete) {
				ShmAcSvc acSvcFound = optionalShmAcSvc.get();
				setValues(shmAcSvc, acSvcFound);

				List<String> diff = this.compareShmAcSvc(shmAcSvc, shmAcSvcToCheck);

				if (CollectionUtils.isNotEmpty(diff)) {
					shmAcSvc.setLstUpdtTmst(new Timestamp(new Date().getTime()));
					shmAcSvc.setLstUpdtTranCd(tranCode);
					shmAcSvc.setLstUpdtUid(racfId);
					result.add(shmAcSvc);
				}
			} else {

				ShmAcSvcPK id = new ShmAcSvcPK();
				copyFields(shmAcSvc.getId(), id);
				shmAcSvcToCheck.setId(id);
				result.add(shmAcSvcToCheck);
			}
		});
		return result;
	}
}
