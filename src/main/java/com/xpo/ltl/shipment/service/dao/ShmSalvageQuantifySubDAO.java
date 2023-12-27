package com.xpo.ltl.shipment.service.dao;


import javax.enterprise.context.ApplicationScoped;

import com.xpo.ltl.api.shipment.service.dao.ShmSalvageQuantifyDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageQuantify;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmSalvageQuantifySubDAO extends ShmSalvageQuantifyDAO<ShmSalvageQuantify> {

}
