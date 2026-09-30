package com.swp391.g1.service;

import com.swp391.g1.dto.request.ExtracurricularActivityRequestDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;

import java.util.List;

public interface IExtracurricularActivityService {
    List<ExtracurricularActivityResponseDTO> getAllActivities();
    ExtracurricularActivityResponseDTO getActivityById(int id);
    boolean createActivity(ExtracurricularActivityRequestDTO requestDTO) throws Exception;
    boolean updateActivity(ExtracurricularActivityRequestDTO requestDTO) throws Exception;
    boolean deleteActivity(int id);
    boolean submitForApproval(int id);
    boolean approveActivity(int id);
    boolean rejectActivity(int id);
}
