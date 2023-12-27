package com.xpo.ltl.shipment.service.dao;

import com.xpo.ltl.api.shipment.service.dao.ShmMiscLineItemDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItemPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@ApplicationScoped
public class ShmMiscLineItemSubDAO extends ShmMiscLineItemDAO<ShmMiscLineItem> {

}