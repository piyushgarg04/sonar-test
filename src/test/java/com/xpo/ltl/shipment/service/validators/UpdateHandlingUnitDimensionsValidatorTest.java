package com.xpo.ltl.shipment.service.validators;

import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitDimensionsRqst;

class UpdateHandlingUnitDimensionsValidatorTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

    private UpdateHandlingUnitDimensionsValidator validator;

    private static final String PRO_NBR = "208-966063";

    @BeforeEach
    public void setUp() {

        MockitoAnnotations.initMocks(this);
        this.validator = new UpdateHandlingUnitDimensionsValidator();
    }

    @Test
    void testRequestIsRequired() {

        ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> validator.validate(null, PRO_NBR,
                        txnContext, entityManager));

        Assertions.assertTrue(ex.getMessage().contains("The request is required"));
    }

    @Test
    void testTrackingProNbrIsRequired() {

        UpdateHandlingUnitDimensionsRqst rqst = new UpdateHandlingUnitDimensionsRqst();

        ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> validator.validate(rqst, null, txnContext, entityManager));

        Assertions.assertTrue(ex.getMessage().contains("Pro number must be entered"));
    }

    @Test
    void testTransactionContextIsRequired() {

        UpdateHandlingUnitDimensionsRqst rqst = new UpdateHandlingUnitDimensionsRqst();

        ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> validator.validate(rqst, PRO_NBR, null, entityManager));

        Assertions.assertTrue(ex.getMessage().contains("The TransactionContext is required"));
    }

    @Test
    void testEntityManagerIsRequired() {

        UpdateHandlingUnitDimensionsRqst rqst = new UpdateHandlingUnitDimensionsRqst();

        ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> validator.validate(rqst, PRO_NBR, txnContext, null));

        Assertions.assertTrue(ex.getMessage().contains("The EntityManager is required"));
    }

    @Test
    void testDimensionsWidthOrHeightExceeded() {

        UpdateHandlingUnitDimensionsRqst rq1 = buildUpdateHUDimensionsRqst(200d, 10d, 10d);
        UpdateHandlingUnitDimensionsRqst rq2 = buildUpdateHUDimensionsRqst(10d, 200d, 10d);

        Stream.of(rq1, rq2).forEach(r -> {

            ValidationException ex = Assertions.assertThrows(ValidationException.class,
                    () -> validator.validate(r, PRO_NBR, txnContext, entityManager));

            Assertions.assertTrue(ex.getMessage()
                    .contains("The shipment Width and Height can not be greater than 103 inches"));
        });
    }

    @Test
    void testDimensionsLengthExceeded() {

        UpdateHandlingUnitDimensionsRqst rq = buildUpdateHUDimensionsRqst(10d, 10d, 700d);

        ValidationException ex = Assertions.assertThrows(ValidationException.class,
                () -> validator.validate(rq, PRO_NBR, txnContext, entityManager));

        Assertions.assertTrue(ex.getMessage()
                .contains("The shipment Length can not be greater than 636 inches"));
    }

    private UpdateHandlingUnitDimensionsRqst buildUpdateHUDimensionsRqst(
            Double w, Double h, Double l) {

        UpdateHandlingUnitDimensionsRqst rq = new UpdateHandlingUnitDimensionsRqst();
        rq.setWidthNbr(w);
        rq.setHeightNbr(h);
        rq.setLengthNbr(l);

        return rq;
    }

}