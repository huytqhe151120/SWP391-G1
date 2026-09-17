package com.swp391.g1.service.impl;

import com.swp391.g1.entity.ExtracurricularActivity;
import com.swp391.g1.repository.ExtracurricularActivityRepository;
import com.swp391.g1.service.ExtracurricularActivityService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExtracurricularActivityServiceImpl
        implements ExtracurricularActivityService {

    private final ExtracurricularActivityRepository repository;

    public ExtracurricularActivityServiceImpl(
            ExtracurricularActivityRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public ExtracurricularActivity create(ExtracurricularActivity activity) {
        return repository.save(activity);
    }

    @Override
    public List<ExtracurricularActivity> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<ExtracurricularActivity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public ExtracurricularActivity update(
            Long id,
            ExtracurricularActivity activity
    ) {
        ExtracurricularActivity existing = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                "Extracurricular activity not found: " + id
            ));

        existing.setSemesterId(activity.getSemesterId());
        existing.setActivityType(activity.getActivityType());
        existing.setCode(activity.getCode());
        existing.setName(activity.getName());
        existing.setResponsibleDepartmentId(activity.getResponsibleDepartmentId());
        existing.setResponsibleStaffId(activity.getResponsibleStaffId());
        existing.setPartnerId(activity.getPartnerId());
        existing.setPartnerStaffId(activity.getPartnerStaffId());
        existing.setBonusPoint(activity.getBonusPoint());
        existing.setPenaltyPoint(activity.getPenaltyPoint());
        existing.setAddress(activity.getAddress());
        existing.setDescription(activity.getDescription());
        existing.setActivityStatus(activity.getActivityStatus());
        existing.setApprovalStatus(activity.getApprovalStatus());

        return repository.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Extracurricular activity not found: " + id
            );
        }

        repository.deleteById(id);
    }
}
