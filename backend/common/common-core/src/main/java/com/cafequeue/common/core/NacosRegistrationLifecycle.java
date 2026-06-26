package com.cafequeue.common.core;

import java.net.InetAddress;
import java.util.Properties;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class NacosRegistrationLifecycle implements DisposableBean {
    private final boolean enabled;
    private final String serverAddr;
    private final String serviceName;
    private final String groupName;
    private final String registerIp;
    private final int port;
    private NamingService namingService;
    private Instance instance;

    public NacosRegistrationLifecycle(
            @Value("${cafequeue.nacos.enabled:true}") boolean enabled,
            @Value("${spring.cloud.nacos.discovery.server-addr:${NACOS_SERVER_ADDR:127.0.0.1:8848}}") String serverAddr,
            @Value("${spring.application.name}") String serviceName,
            @Value("${spring.cloud.nacos.discovery.group:DEFAULT_GROUP}") String groupName,
            @Value("${NACOS_REGISTER_IP:}") String registerIp,
            @Value("${server.port}") int port
    ) {
        this.enabled = enabled;
        this.serverAddr = serverAddr;
        this.serviceName = serviceName;
        this.groupName = groupName;
        this.registerIp = registerIp;
        this.port = port;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() throws Exception {
        if (!enabled) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverAddr);
        namingService = NacosFactory.createNamingService(properties);

        instance = new Instance();
        instance.setIp(resolveIp());
        instance.setPort(port);
        instance.setEphemeral(true);
        instance.setHealthy(true);
        instance.addMetadata("framework", "spring-boot-4");
        instance.addMetadata("registeredBy", "CafeQueueManualRegistrar");

        namingService.registerInstance(serviceName, groupName, instance);
    }

    @Override
    public void destroy() throws Exception {
        if (namingService != null && instance != null) {
            namingService.deregisterInstance(serviceName, groupName, instance.getIp(), instance.getPort());
        }
    }

    private String resolveIp() throws Exception {
        if (registerIp != null && !registerIp.isBlank()) {
            return registerIp;
        }
        return InetAddress.getLocalHost().getHostAddress();
    }
}
