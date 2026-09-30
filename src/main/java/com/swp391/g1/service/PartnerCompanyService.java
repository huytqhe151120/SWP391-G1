package com.swp391.g1.service;

import com.swp391.g1.dao.PartnerCompanyDAO;
import com.swp391.g1.model.PartnerCompany;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class PartnerCompanyService {

    private PartnerCompanyDAO partnerDAO;
    private boolean sampleMode;
    private final List<PartnerCompany> samplePartners = new java.util.ArrayList<>();

    public PartnerCompanyService() {
        try {
            partnerDAO = new PartnerCompanyDAO();
        } catch (IllegalStateException exception) {
            sampleMode = true;
            samplePartners.addAll(createSamplePartners());
        }
    }

    public List<PartnerCompany> getAll() throws SQLException {
        if (sampleMode) {
            return samplePartners;
        }
        try {
            return partnerDAO.getAll();
        } catch (SQLException exception) {
            enableSampleMode();
            return samplePartners;
        }
    }

    public PartnerCompany getById(int id) throws SQLException {
        if (id <= 0) {
            return null;
        }
        if (sampleMode) {
            return findSamplePartner(id);
        }
        try {
            return partnerDAO.getById(id);
        } catch (SQLException exception) {
            enableSampleMode();
            return findSamplePartner(id);
        }
    }

    public boolean create(PartnerCompany partner) throws SQLException {
        if (!isValid(partner)) {
            return false;
        }
        if (partner.getCreatedAt() == null) {
            partner.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }
        if (sampleMode) {
            addSamplePartner(partner);
            return true;
        }
        try {
            return partnerDAO.insert(partner);
        } catch (SQLException exception) {
            enableSampleMode();
            addSamplePartner(partner);
            return true;
        }
    }

    public boolean update(PartnerCompany partner) throws SQLException {
        if (partner == null || partner.getId() <= 0 || !isValid(partner)) {
            return false;
        }
        if (sampleMode) {
            return replaceSamplePartner(partner);
        }
        try {
            return partnerDAO.update(partner);
        } catch (SQLException exception) {
            enableSampleMode();
            return replaceSamplePartner(partner);
        }
    }

    public boolean delete(int id) throws SQLException {
        if (id <= 0) {
            return false;
        }
        if (sampleMode) {
            return removeSamplePartner(id);
        }
        try {
            return partnerDAO.delete(id);
        } catch (SQLException exception) {
            enableSampleMode();
            return removeSamplePartner(id);
        }
    }

    private void enableSampleMode() {
        sampleMode = true;
        if (samplePartners.isEmpty()) {
            samplePartners.addAll(createSamplePartners());
        }
    }

    private List<PartnerCompany> createSamplePartners() {
        List<PartnerCompany> partners = new java.util.ArrayList<>();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        PartnerCompany first = new PartnerCompany("FPT Software", "contact@fpt.com", "0900000001",
                "Da Nang", "ACTIVE", now);
        first.setId(1);
        PartnerCompany second = new PartnerCompany("VNG Corporation", "contact@vng.com", "0900000002",
                "Ho Chi Minh City", "ACTIVE", now);
        second.setId(2);
        partners.add(first);
        partners.add(second);
        return partners;
    }

    private PartnerCompany findSamplePartner(int id) {
        return samplePartners.stream().filter(partner -> partner.getId() == id).findFirst().orElse(null);
    }

    private void addSamplePartner(PartnerCompany partner) {
        partner.setId(samplePartners.stream().mapToInt(PartnerCompany::getId).max().orElse(0) + 1);
        samplePartners.add(partner);
    }

    private boolean replaceSamplePartner(PartnerCompany partner) {
        for (int index = 0; index < samplePartners.size(); index++) {
            if (samplePartners.get(index).getId() == partner.getId()) {
                samplePartners.set(index, partner);
                return true;
            }
        }
        return false;
    }

    private boolean removeSamplePartner(int id) {
        return samplePartners.removeIf(partner -> partner.getId() == id);
    }

    private boolean isValid(PartnerCompany partner) {
        return partner != null
                && hasText(partner.getName())
                && hasText(partner.getEmail())
                && hasText(partner.getPhone())
                && hasText(partner.getStatus());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}