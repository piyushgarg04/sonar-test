package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Stopwatch;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.GetShipmentCountByShipperResp;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dto.ShipmentCntMonthDTO;

@RequestScoped
public class GetShipmentCountImpl {

	private static final Log logger = LogFactory.getLog(GetShipmentCountImpl.class);

	@Inject
	private ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;


    /**
     * @param shipperCisCustomerNbr
     * @param txnContext
     * @param entityManager
     * @return
     * @throws ServiceException
     */
	public GetShipmentCountByShipperResp getShipmentCountByShipper(final Integer shipperCisCustomerNbr, final TransactionContext txnContext,
			final EntityManager entityManager) throws ServiceException {

		final Stopwatch sw = Stopwatch.createStarted();
		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");

		final Stopwatch sw2 = Stopwatch.createStarted();

        // by default takes 12 months of history
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, -12);
        final Date pkupDateFrom = calendar.getTime();

        calendar.add(Calendar.MONTH, 12);
        calendar.add(Calendar.DAY_OF_MONTH, -1); // last day of month
        final Date pkupDateTo = calendar.getTime();

        List<Pair<Integer, Integer>> last12YearMonthPair = build12MonthPairList();

        final List<ShipmentCntMonthDTO> shipmentCountDTOList = shipmentAsEnteredCustomerDAO
            .getShipmentCountByShipperCustNbr(shipperCisCustomerNbr, pkupDateFrom, pkupDateTo, entityManager);

        List<BigInteger> result = new ArrayList<BigInteger>();
        for (Pair<Integer, Integer> yearMonthPair : last12YearMonthPair) {
            Optional<ShipmentCntMonthDTO> dtoOpt = shipmentCountDTOList
                .stream()
                .filter(dto -> dto.getYear().equals(yearMonthPair.getLeft()) && dto.getMonth().equals(yearMonthPair.getRight()))
                .findAny();
            result.add(dtoOpt.isPresent() ? BigInteger.valueOf(dtoOpt.get().getCount()) : BigInteger.ZERO);
        }

		logger.info(
				String.format("GetShipmentCountImpl DAO call to get shipment count for shipper customer number %s in %s ms",
						shipperCisCustomerNbr, sw2.elapsed(TimeUnit.MILLISECONDS)));
		sw2.stop();


		final GetShipmentCountByShipperResp resp = new GetShipmentCountByShipperResp();
        resp.setShipmentCount(result);

		sw.stop();
		logger.info(
				String.format("GetShipmentCountImpl.getShipmentCountByShipper in %s ms", sw.elapsed(TimeUnit.MILLISECONDS)));
		return resp;
	}


    /**
     * return a list of Pair<year,month> for the las 12 months.
     *
     * @return
     */
    private List<Pair<Integer, Integer>> build12MonthPairList() {
        List<Pair<Integer, Integer>> last12YearMonthPair = new ArrayList<>();
        final Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.MONTH, -1); // start with prev. month.
        for (int i = 0; i < 12; i++) {
            Integer yearNbr = calendar2.get(Calendar.YEAR);
            Integer monthNbr = calendar2.get(Calendar.MONTH) + 1;
            last12YearMonthPair.add(Pair.of(yearNbr, monthNbr));
            calendar2.add(Calendar.MONTH, -1);
        }
        return last12YearMonthPair;
    }

}
