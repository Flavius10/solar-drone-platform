package com.solar.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


@Service
public class SseNotificationService {

    private record Registration(SseEmitter emitter, Set<Long> accessibleFarmIds) {
    }

    private final List<Registration> registrations = new CopyOnWriteArrayList<>();
    private final FarmAccessService farmAccessService;

    public SseNotificationService(FarmAccessService farmAccessService) {
        this.farmAccessService = farmAccessService;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); 
        Set<Long> accessibleFarmIds = farmAccessService.getAccessibleFarmIds();
        Registration registration = new Registration(emitter, accessibleFarmIds);
        registrations.add(registration);

        emitter.onCompletion(() -> registrations.remove(registration));
        emitter.onTimeout(() -> registrations.remove(registration));
        emitter.onError(e -> registrations.remove(registration));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            registrations.remove(registration);
        }

        return emitter;
    }

    public void broadcast(String eventName, Object data, Long farmId) {
        for (Registration registration : registrations) {
            boolean visible = farmId == null
                    || registration.accessibleFarmIds() == null
                    || registration.accessibleFarmIds().contains(farmId);
            if (!visible) continue;

            try {
                registration.emitter().send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                registrations.remove(registration);
            }
        }
    }

    
    
    @Scheduled(fixedRate = 25000)
    public void heartbeat() {
        for (Registration registration : registrations) {
            try {
                registration.emitter().send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                registrations.remove(registration);
            }
        }
    }
}
