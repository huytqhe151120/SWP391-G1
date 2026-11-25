package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IStudentGradeDAO;
import com.swp391.g1.dao.impl.StudentGradeDAOImpl;
import com.swp391.g1.dto.response.StudentActivityGradeDTO;
import com.swp391.g1.dto.response.StudentGradeSummaryDTO;
import com.swp391.g1.service.IStudentGradeService;

import java.util.List;

/**
 * Triển khai tầng Service cho nghiệp vụ tra cứu điểm tổng hợp và điểm chi tiết hoạt động của sinh viên.
 * Ủy quyền trực tiếp việc đồng bộ và truy vấn dữ liệu xuống {@link IStudentGradeDAO}.
 */
public class StudentGradeServiceImpl implements IStudentGradeService {

    private final IStudentGradeDAO studentGradeDAO;

    /**
     * Khởi tạo service với triển khai DAO mặc định {@link StudentGradeDAOImpl}.
     */
    public StudentGradeServiceImpl() {
        this.studentGradeDAO = new StudentGradeDAOImpl();
    }

    /**
     * Lấy danh sách tổng hợp điểm hoạt động của tất cả sinh viên cộng dồn trên toàn bộ học kỳ.
     *
     * @return danh sách DTO tổng hợp điểm sinh viên (truyền {@code semesterId = 0} để lấy tất cả học kỳ)
     */
    @Override
    public List<StudentGradeSummaryDTO> getAllStudentGradeSummaries() {
        return studentGradeDAO.findAllStudentGradeSummaries(0);
    }

    /**
     * Lấy danh sách tổng hợp điểm hoạt động của tất cả sinh viên theo một học kỳ cụ thể.
     *
     * @param semesterId ID học kỳ cần lọc ({@code <= 0} để tổng hợp trên tất cả học kỳ)
     * @return danh sách DTO tổng hợp điểm sinh viên
     */
    @Override
    public List<StudentGradeSummaryDTO> getAllStudentGradeSummaries(int semesterId) {
        return studentGradeDAO.findAllStudentGradeSummaries(semesterId);
    }

    /**
     * Lấy thông tin tổng hợp điểm của một sinh viên cộng dồn trên tất cả học kỳ.
     *
     * @param studentId ID sinh viên cần tra cứu
     * @return DTO tổng hợp điểm hoặc {@code null} nếu không tìm thấy sinh viên
     */
    @Override
    public StudentGradeSummaryDTO getStudentGradeSummaryById(int studentId) {
        return studentGradeDAO.findStudentGradeSummaryById(studentId, 0);
    }

    /**
     * Lấy thông tin tổng hợp điểm của một sinh viên theo học kỳ chỉ định.
     *
     * @param studentId  ID sinh viên cần tra cứu
     * @param semesterId ID học kỳ ({@code <= 0} để tổng hợp tất cả học kỳ)
     * @return DTO tổng hợp điểm hoặc {@code null} nếu không tìm thấy sinh viên
     */
    @Override
    public StudentGradeSummaryDTO getStudentGradeSummaryById(int studentId, int semesterId) {
        return studentGradeDAO.findStudentGradeSummaryById(studentId, semesterId);
    }

    /**
     * Lấy lịch sử điểm tất cả các hoạt động mà sinh viên đã tham gia (không giới hạn học kỳ).
     *
     * @param studentId ID sinh viên cần tra cứu
     * @return danh sách DTO điểm chi tiết theo từng hoạt động
     */
    @Override
    public List<StudentActivityGradeDTO> getActivityGradesByStudentId(int studentId) {
        return studentGradeDAO.findActivityGradesByStudentId(studentId, 0);
    }

    /**
     * Lấy lịch sử điểm các hoạt động mà sinh viên tham gia trong một học kỳ cụ thể.
     *
     * @param studentId  ID sinh viên cần tra cứu
     * @param semesterId ID học kỳ ({@code <= 0} để lấy tất cả học kỳ)
     * @return danh sách DTO điểm chi tiết theo từng hoạt động
     */
    @Override
    public List<StudentActivityGradeDTO> getActivityGradesByStudentId(int studentId, int semesterId) {
        return studentGradeDAO.findActivityGradesByStudentId(studentId, semesterId);
    }

    /**
     * Lấy bảng phân tích điểm chi tiết của một sinh viên trong một hoạt động cụ thể.
     *
     * @param studentId  ID sinh viên
     * @param activityId ID hoạt động ngoại khóa
     * @return DTO chi tiết điểm của sinh viên trong hoạt động hoặc {@code null} nếu không tồn tại
     */
    @Override
    public StudentActivityGradeDTO getActivityGradeDetail(int studentId, int activityId) {
        return studentGradeDAO.findActivityGradeDetail(studentId, activityId);
    }
}
