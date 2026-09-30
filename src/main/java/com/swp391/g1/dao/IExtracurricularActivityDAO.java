package com.swp391.g1.dao;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.Enum;
import java.util.List;

public interface IExtracurricularActivityDAO extends IGenericDAO<ExtracurricularActivity, Integer> {
    List<ExtracurricularActivity> findByCode(String code);
    List<ExtracurricularActivity> findByActivityStatus(Enum.ActivityStatus status);
    List<ExtracurricularActivity> findByApprovalStatus(Enum.ApprovalStatus status);

    public List<ExtracurricularActivityResponseDTO> findAllWithDetails();
}
