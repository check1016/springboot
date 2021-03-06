package com.yq.service.impl;

import com.ecwid.consul.v1.ConsulClient;
import com.orbitz.consul.*;
import com.orbitz.consul.model.health.ServiceHealth;
import com.yq.config.ConsulConfig;
import com.yq.config.SpringContextHolder;
import com.yq.service.IConsulService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;


@Service
@Slf4j
public class OrbitzConsulServiceImpl implements IConsulService {

    @Autowired
    private ConsulConfig consulConfig;

    @Autowired
    private ApplicationContext context;

    private Consul consul = null;

    public OrbitzConsulServiceImpl() {
        log.info("consulConfig={}", consulConfig);
        log.info("context={}", context);
        if (consulConfig == null && context != null) {
            consulConfig = (ConsulConfig) context.getBean("consulConfig");
        }
        else if (consulConfig == null) {
            consulConfig = (ConsulConfig) SpringContextHolder.getBean("consulConfig");
            log.info("consulConfig={} gotten by SpringContextHolder.", consulConfig);
        }

        String url = null;
        if (consulConfig == null) {
            url = "http:/127.0.0.1:8500";
        }
        else {
            url = "http://" + consulConfig.getConsulIP() + ":" + consulConfig.getConsulPort();
        }

        consul = Consul.builder().withUrl(url).build();
    }

    @Override
    public void registerService(String serviceName, String serviceId) {

        AgentClient agentClient = consul.agentClient();

        try {
            //"本应用就在"http://127.0.0.1:" +  + "/health"，并且启用actuator，可以作为consul的监控服务检查
            agentClient.register(8080, URI.create("http://127.0.0.1:8080/health").toURL(),
                    10L, serviceName, serviceId, null, null);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            agentClient.pass(serviceId);
        } catch (NotRegisteredException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deRegisterService(String serviceId) {
        AgentClient agentClient = consul.agentClient();
        agentClient.deregister(serviceId);
    }

    @Override
    public List<ServiceHealth> findServiceHealthy(String servicename){
        HealthClient healthClient = consul.healthClient();
        return healthClient.getHealthyServiceInstances(servicename).getResponse();
    }

    @Override
    public void storeKV(String key, String value){
        KeyValueClient kvClient = consul.keyValueClient();
        kvClient.putValue(key, value);
    }

    @Override
    public String getKV(String key){
        KeyValueClient kvClient = consul.keyValueClient();
        return kvClient.getValueAsString(key).get();
    }

    @Override
    public List<String> findRaftPeers(){
        StatusClient statusClient = consul.statusClient();
        return statusClient.getPeers();
    }

    @Override
    public String findRaftLeader(){
        StatusClient statusClient = consul.statusClient();
        return statusClient.getLeader();
    }
}