package com.schoolms.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantBinder {

    @PersistenceContext
    private EntityManager entityManager;

    public void bind(UUID schoolId) {
        TenantContext.setSchoolId(schoolId);
        TenantContext.setBypassRls(schoolId == null);
        apply();
    }

    public void bindBypass() {
        TenantContext.setBypassRls(true);
        apply();
    }

    public void apply() {
        UUID schoolId = TenantContext.getSchoolId();
        boolean bypass = TenantContext.isBypassRls();
        entityManager.createNativeQuery(
                        "SELECT set_config('app.school_id', :sid, false), set_config('app.bypass_rls', :bypass, false)")
                .setParameter("sid", schoolId == null ? "" : schoolId.toString())
                .setParameter("bypass", bypass ? "true" : "false")
                .getSingleResult();
    }
}
