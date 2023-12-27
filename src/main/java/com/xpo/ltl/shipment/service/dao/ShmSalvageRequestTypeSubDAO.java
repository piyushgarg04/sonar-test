package com.xpo.ltl.shipment.service.dao;


import javax.enterprise.context.ApplicationScoped;

import com.xpo.ltl.api.shipment.service.dao.ShmSalvageRequestTypeDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequestType;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmSalvageRequestTypeSubDAO extends ShmSalvageRequestTypeDAO<ShmSalvageRequestType> {

}
