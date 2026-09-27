package com.swp391.g1.dao;
import com.swp391.g1.model.PartnerCompany;
import com.swp391.g1.model.Enum;
import java.util.List;

public interface IPartnerCompanyDAO extends IGenericDAO {
    PartnerCompany findByCode(String code);
    List<PartnerCompany> findByCommonStatus(Enum.CommonStatus status);
}
