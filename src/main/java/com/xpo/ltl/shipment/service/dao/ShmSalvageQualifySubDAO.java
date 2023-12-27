package com.xpo.ltl.shipment.service.dao;


import javax.enterprise.context.ApplicationScoped;

import com.xpo.ltl.api.shipment.service.dao.ShmSalvageQualifyDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageQualify;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmSalvageQualifySubDAO extends ShmSalvageQualifyDAO<ShmSalvageQualify> {

}
