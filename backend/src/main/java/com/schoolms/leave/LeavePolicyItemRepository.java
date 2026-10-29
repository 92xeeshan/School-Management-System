package com.schoolms.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeavePolicyItemRepository extends JpaRepository<LeavePolicyItem, UUID> {

    List<LeavePolicyItem> findByPolicyIdOrderByCreatedAtAsc(UUID policyId);

    void deleteByPolicyId(UUID policyId);
}
