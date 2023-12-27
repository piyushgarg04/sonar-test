package com.xpo.ltl.shipment.service.dao;

import javax.enterprise.context.ApplicationScoped;

import com.xpo.ltl.api.shipment.service.dao.ShmRtgInfoDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmRtgInfo;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmRtgInfoSubDAO extends ShmRtgInfoDAO<ShmRtgInfo> {

}
