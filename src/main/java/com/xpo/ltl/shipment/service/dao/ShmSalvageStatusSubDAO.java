package com.xpo.ltl.shipment.service.dao;


import javax.enterprise.context.ApplicationScoped;

import com.xpo.ltl.api.shipment.service.dao.ShmSalvageStatusDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageStatus;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmSalvageStatusSubDAO extends ShmSalvageStatusDAO<ShmSalvageStatus> {

}
