package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityTargetParticipantDAO;
import com.swp391.g1.dao.impl.ActivityTargetParticipantDAOImpl;
import com.swp391.g1.model.AcademicCohort;
import com.swp391.g1.model.AcademicProgram;
import com.swp391.g1.model.ActivityTargetParticipant;
import com.swp391.g1.model.Department;
import com.swp391.g1.model.MainClass;
import com.swp391.g1.model.Major;
import com.swp391.g1.service.IActivityTargetParticipantService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service triển khai nghiệp vụ quản lý đối tượng tham gia (Khoa, Niên khóa, Ngành,
 * Chương trình đào tạo, Lớp sinh hoạt) của hoạt động ngoại khóa và tự động ghi danh
 * cho các hoạt động bắt buộc.
 */
public class ActivityTargetParticipantServiceImpl implements IActivityTargetParticipantService {

    // Hàng đợi xử lý bất đồng bộ đơn luồng (daemon) để tự động ghi danh sinh viên cho hoạt động bắt buộc mà không chặn luồng duyệt.
    private static final ExecutorService AUTO_ENROLL_QUEUE = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mandatory-activity-auto-enroll-worker");
        t.setDaemon(true);
        return t;
    });

    private final IActivityTargetParticipantDAO targetDAO;

    /**
     * Khởi tạo service với đối tượng DAO thao tác dữ liệu đối tượng tham gia mặc định.
     */
    public ActivityTargetParticipantServiceImpl() {
        this.targetDAO = new ActivityTargetParticipantDAOImpl();
    }

    /**
     * Lấy danh sách các Khoa chuyên môn đang hoạt động để hiển thị trên bộ chọn đối tượng.
     *
     * @return danh sách Khoa đang hoạt động
     */
    @Override
    public List<Department> getActiveAcademicDepartments() {
        return targetDAO.findActiveAcademicDepartments();
    }

    /**
     * Lấy danh sách các Niên khóa đang hoạt động.
     *
     * @return danh sách Niên khóa đang hoạt động
     */
    @Override
    public List<AcademicCohort> getActiveCohorts() {
        return targetDAO.findActiveCohorts();
    }

    /**
     * Lấy danh sách các Ngành học đang hoạt động.
     *
     * @return danh sách Ngành học đang hoạt động
     */
    @Override
    public List<Major> getActiveMajors() {
        return targetDAO.findActiveMajors();
    }

    /**
     * Lấy danh sách các Chương trình đào tạo đang hoạt động.
     *
     * @return danh sách Chương trình đào tạo đang hoạt động
     */
    @Override
    public List<AcademicProgram> getActiveAcademicPrograms() {
        return targetDAO.findActiveAcademicPrograms();
    }

    /**
     * Lấy danh sách các Lớp sinh hoạt đang hoạt động.
     *
     * @return danh sách Lớp sinh hoạt đang hoạt động
     */
    @Override
    public List<MainClass> getActiveMainClasses() {
        return targetDAO.findActiveMainClasses();
    }

    /**
     * Lấy danh sách các dòng cấu hình đối tượng tham gia đã lưu của một hoạt động.
     *
     * @param activityId ID hoạt động ngoại khóa
     * @return danh sách cấu hình đối tượng tham gia
     */
    @Override
    public List<ActivityTargetParticipant> getTargetsByActivityId(int activityId) {
        return targetDAO.findByActivityId(activityId);
    }

    /**
     * Xây dựng và chuẩn hóa danh sách các dòng cấu hình đối tượng tham gia từ dữ liệu form chọn nhóm.
     * Áp dụng quy tắc gom nhánh phân cấp (Khoa -> Ngành -> Chương trình -> Lớp) để tránh trùng lặp,
     * đồng thời kết hợp theo phép AND với Niên khóa (chiều độc lập) và loại bỏ bản ghi trùng.
     *
     * @param targetScope        phạm vi áp dụng ("ALL" cho toàn trường hoặc "GROUP" cho nhóm cụ thể)
     * @param departmentIds      danh sách ID Khoa được chọn
     * @param cohortIds          danh sách ID Niên khóa được chọn
     * @param majorIds           danh sách ID Ngành học được chọn
     * @param academicProgramIds danh sách ID Chương trình đào tạo được chọn
     * @param mainClassIds       danh sách ID Lớp sinh hoạt được chọn
     * @return danh sách các dòng {@link ActivityTargetParticipant} đã chuẩn hóa sẵn sàng lưu DB
     */
    @Override
    public List<ActivityTargetParticipant> buildTargetRows(
            String targetScope,
            List<String> departmentIds,
            List<String> cohortIds,
            List<String> majorIds,
            List<String> academicProgramIds,
            List<String> mainClassIds) {

        // Chuẩn hóa phạm vi: nếu để trống hoặc null thì mặc định áp dụng cho toàn bộ sinh viên ("ALL").
        String scope = targetScope == null || targetScope.isBlank() ? "ALL" : targetScope.trim().toUpperCase();
        // Nếu chọn phạm vi "ALL", trả về một dòng wildcard (toàn bộ cột điều kiện đều null).
        if ("ALL".equals(scope)) {
            return List.of(new ActivityTargetParticipant(null, null, null, null, null));
        }
        // Từ chối các giá trị phạm vi không nằm trong tập hợp lệ ("ALL" hoặc "GROUP").
        if (!"GROUP".equals(scope)) {
            throw new IllegalArgumentException("Phạm vi đối tượng tham gia không hợp lệ.");
        }

        // Chuyển đổi và kiểm tra tính hợp lệ (số nguyên dương) cho các nhóm ID gửi từ form.
        Set<Integer> selectedDeptIds = parsePositiveIds(departmentIds, "Khoa không hợp lệ.");
        Set<Integer> selectedCohortIds = parsePositiveIds(cohortIds, "Niên khóa không hợp lệ.");
        Set<Integer> selectedMajorIds = parsePositiveIds(majorIds, "Ngành học không hợp lệ.");
        Set<Integer> selectedProgramIds = parsePositiveIds(academicProgramIds, "Chương trình đào tạo không hợp lệ.");
        Set<Integer> selectedClassIds = parsePositiveIds(mainClassIds, "Lớp sinh hoạt không hợp lệ.");

        // Nếu chọn "GROUP" nhưng không chọn bất kỳ tiêu chí lọc nào thì coi như áp dụng cho tất cả sinh viên.
        if (selectedDeptIds.isEmpty()
                && selectedCohortIds.isEmpty()
                && selectedMajorIds.isEmpty()
                && selectedProgramIds.isEmpty()
                && selectedClassIds.isEmpty()) {
            return List.of(new ActivityTargetParticipant(null, null, null, null, null));
        }

        // Tải sẵn danh mục Ngành, Chương trình và Lớp vào Map để tra cứu quan hệ cha-con nhanh O(1).
        Map<Integer, Major> majorById = new HashMap<>();
        // Duyệt toàn bộ Ngành đang hoạt động để lập chỉ mục theo ID.
        for (Major m : targetDAO.findActiveMajors()) {
            majorById.put(m.getId(), m);
        }
        Map<Integer, AcademicProgram> programById = new HashMap<>();
        // Duyệt toàn bộ Chương trình đào tạo đang hoạt động để lập chỉ mục theo ID.
        for (AcademicProgram p : targetDAO.findActiveAcademicPrograms()) {
            programById.put(p.getId(), p);
        }
        Map<Integer, MainClass> classById = new HashMap<>();
        // Duyệt toàn bộ Lớp sinh hoạt đang hoạt động để lập chỉ mục theo ID.
        for (MainClass mc : targetDAO.findActiveMainClasses()) {
            classById.put(mc.getId(), mc);
        }

        // Theo dõi các thực thể cấp cha đã được bao hàm trong tiêu chí cấp con chi tiết hơn để không sinh dòng thừa.
        Set<Integer> consumedDeptIds = new LinkedHashSet<>();
        Set<Integer> consumedCohortIds = new LinkedHashSet<>();
        Set<Integer> consumedMajorIds = new LinkedHashSet<>();
        Set<Integer> consumedProgramIds = new LinkedHashSet<>();

        // Dùng LinkedHashSet để giữ thứ tự sinh dòng và tự động khử các dòng cấu hình trùng khớp nhau.
        Set<ActivityTargetParticipant> result = new LinkedHashSet<>();

        // Bước 1 (cấp lá): Duyệt các Lớp sinh hoạt được chọn và truy ngược lên Chương trình, Ngành, Khoa, Niên khóa tương ứng.
        for (Integer classId : selectedClassIds) {
            MainClass mc = classById.get(classId);
            // Truy ngược chuỗi quan hệ phân cấp từ Lớp -> Chương trình -> Ngành -> Khoa và lấy Niên khóa của lớp.
            Integer progId = mc != null ? mc.getResponsibleAcademicProgramId() : null;
            AcademicProgram prog = progId != null ? programById.get(progId) : null;
            Integer majorId = prog != null ? prog.getResponsibleMajorId() : null;
            Major major = majorId != null ? majorById.get(majorId) : null;
            Integer deptId = major != null ? major.getResponsibleDepartmentId() : null;
            Integer cohortId = mc != null ? mc.getApplicableCohortId() : null;

            // Chỉ giữ lại ID cấp cha nếu người dùng thực sự có tick chọn cấp cha tương ứng trên giao diện.
            Integer matchedProg = (progId != null && selectedProgramIds.contains(progId)) ? progId : null;
            Integer matchedMajor = (majorId != null && selectedMajorIds.contains(majorId)) ? majorId : null;
            Integer matchedDept = (deptId != null && selectedDeptIds.contains(deptId)) ? deptId : null;
            Integer matchedCohort = (cohortId != null && selectedCohortIds.contains(cohortId)) ? cohortId : null;

            // Đánh dấu Chương trình đã được tiêu thụ bởi Lớp chi tiết hơn.
            if (matchedProg != null) consumedProgramIds.add(matchedProg);
            // Đánh dấu Ngành đã được tiêu thụ bởi Lớp chi tiết hơn.
            if (matchedMajor != null) consumedMajorIds.add(matchedMajor);
            // Đánh dấu Khoa đã được tiêu thụ bởi Lớp chi tiết hơn.
            if (matchedDept != null) consumedDeptIds.add(matchedDept);
            // Đánh dấu Niên khóa đã được tiêu thụ bởi Lớp chi tiết hơn.
            if (matchedCohort != null) consumedCohortIds.add(matchedCohort);

            result.add(new ActivityTargetParticipant(matchedDept, matchedCohort, matchedMajor, matchedProg, classId));
        }

        // Danh sách các nhánh học thuật (Khoa / Ngành / Chương trình) chưa gắn Lớp để kết hợp tổ hợp với Niên khóa.
        List<int[]> academicBranches = new ArrayList<>(); // [deptId, majorId, progId] (0 nghĩa là null)

        // Bước 2: Duyệt các Chương trình đào tạo được chọn nhưng chưa bị bao hàm bởi Lớp sinh hoạt nào.
        for (Integer progId : selectedProgramIds) {
            // Bỏ qua nếu Chương trình này đã xuất hiện trong dòng chi tiết theo Lớp ở bước 1.
            if (consumedProgramIds.contains(progId)) {
                continue;
            }
            AcademicProgram prog = programById.get(progId);
            // Truy ngược lên Ngành và Khoa quản lý của Chương trình.
            Integer majorId = prog != null ? prog.getResponsibleMajorId() : null;
            Major major = majorId != null ? majorById.get(majorId) : null;
            Integer deptId = major != null ? major.getResponsibleDepartmentId() : null;

            // Ghép với Ngành/Khoa nếu chúng cũng nằm trong tập người dùng chọn; dùng 0 đại diện cho null.
            int matchedMajor = (majorId != null && selectedMajorIds.contains(majorId)) ? majorId : 0;
            int matchedDept = (deptId != null && selectedDeptIds.contains(deptId)) ? deptId : 0;

            // Đánh dấu Ngành đã được tiêu thụ bởi nhánh Chương trình.
            if (matchedMajor > 0) consumedMajorIds.add(matchedMajor);
            // Đánh dấu Khoa đã được tiêu thụ bởi nhánh Chương trình.
            if (matchedDept > 0) consumedDeptIds.add(matchedDept);

            academicBranches.add(new int[]{matchedDept, matchedMajor, progId});
        }

        // Bước 3: Duyệt các Ngành học được chọn nhưng chưa bị bao hàm bởi Chương trình hoặc Lớp nào.
        for (Integer majorId : selectedMajorIds) {
            // Bỏ qua nếu Ngành đã được tiêu thụ ở các cấp chi tiết hơn.
            if (consumedMajorIds.contains(majorId)) {
                continue;
            }
            Major major = majorById.get(majorId);
            // Xác định Khoa quản lý của Ngành và kiểm tra xem Khoa đó có được chọn hay không.
            Integer deptId = major != null ? major.getResponsibleDepartmentId() : null;
            int matchedDept = (deptId != null && selectedDeptIds.contains(deptId)) ? deptId : 0;
            // Đánh dấu Khoa đã được tiêu thụ bởi nhánh Ngành.
            if (matchedDept > 0) consumedDeptIds.add(matchedDept);

            academicBranches.add(new int[]{matchedDept, majorId, 0});
        }

        // Bước 4: Duyệt các Khoa được chọn nhưng chưa bị bao hàm bởi bất kỳ Ngành, Chương trình hay Lớp nào.
        for (Integer deptId : selectedDeptIds) {
            // Bỏ qua nếu Khoa đã xuất hiện ở các nhánh chi tiết hơn.
            if (consumedDeptIds.contains(deptId)) {
                continue;
            }
            academicBranches.add(new int[]{deptId, 0, 0});
        }

        // Bước 5: Kết hợp từng nhánh học thuật với các Niên khóa được chọn (quan hệ AND giữa Niên khóa và Khoa/Ngành/CTĐT).
        for (int[] branch : academicBranches) {
            // Chuyển quy ước 0 về null trước khi tạo đối tượng ActivityTargetParticipant.
            Integer dId = branch[0] > 0 ? branch[0] : null;
            Integer mId = branch[1] > 0 ? branch[1] : null;
            Integer pId = branch[2] > 0 ? branch[2] : null;
            // Nếu không chọn Niên khóa nào, nhánh học thuật áp dụng cho mọi Niên khóa (cohortId = null).
            if (selectedCohortIds.isEmpty()) {
                result.add(new ActivityTargetParticipant(dId, null, mId, pId, null));
            } else {
                // Ngược lại, tạo tổ hợp Đề-các giữa nhánh học thuật hiện tại và từng Niên khóa được chọn.
                for (Integer cohortId : selectedCohortIds) {
                    consumedCohortIds.add(cohortId);
                    result.add(new ActivityTargetParticipant(dId, cohortId, mId, pId, null));
                }
            }
        }

        // Bước 6: Xử lý các Niên khóa còn lại chưa được ghép (ví dụ chỉ lọc theo Niên khóa hoặc Niên khóa độc lập với Lớp đã chọn).
        for (Integer cohortId : selectedCohortIds) {
            // Nếu Niên khóa chưa xuất hiện trong bất kỳ dòng nào phía trên thì thêm dòng chỉ lọc theo Niên khóa.
            if (!consumedCohortIds.contains(cohortId)) {
                result.add(new ActivityTargetParticipant(null, cohortId, null, null, null));
            }
        }

        return new ArrayList<>(result);
    }

    /**
     * Lưu thay thế toàn bộ cấu hình đối tượng tham gia cho một hoạt động ngoại khóa.
     *
     * @param activityId ID hoạt động cần cập nhật
     * @param targets    danh sách dòng đối tượng tham gia mới
     */
    @Override
    public void saveTargetsForActivity(int activityId, List<ActivityTargetParticipant> targets) {
        targetDAO.replaceTargetsForActivity(activityId, targets);
    }

    /**
     * Kiểm tra một sinh viên có thuộc phạm vi đối tượng tham gia hợp lệ của hoạt động hay không.
     *
     * @param activityId ID hoạt động
     * @param studentId  ID sinh viên
     * @return {@code true} nếu sinh viên đủ điều kiện tham gia, ngược lại {@code false}
     */
    @Override
    public boolean isStudentEligible(int activityId, int studentId) {
        return targetDAO.isStudentEligible(activityId, studentId);
    }

    /**
     * Lấy tập hợp ID tất cả các hoạt động đã duyệt mà sinh viên đủ điều kiện tham gia.
     *
     * @param studentId ID sinh viên
     * @return tập hợp ID hoạt động phù hợp với sinh viên
     */
    @Override
    public Set<Integer> getEligibleActivityIdsForStudent(int studentId) {
        return targetDAO.findEligibleActivityIdsForStudent(studentId);
    }

    /**
     * Đưa yêu cầu tự động ghi danh sinh viên đủ điều kiện cho hoạt động bắt buộc vào hàng đợi chạy nền.
     *
     * @param activityId ID hoạt động bắt buộc đã được duyệt
     */
    @Override
    public void enqueueMandatoryAutoEnrollment(int activityId) {
        // Bỏ qua nếu ID hoạt động không hợp lệ.
        if (activityId <= 0) {
            return;
        }
        // Đẩy tác vụ ghi danh vào luồng nền tuần tự để tránh nghẽn giao diện và tránh xung đột đồng thời.
        AUTO_ENROLL_QUEUE.submit(() -> {
            try {
                targetDAO.enrollEligibleStudentsForMandatoryActivity(activityId);
            } catch (Exception e) {
                // Ghi log lỗi nếu quá trình tự động ghi danh thất bại để không làm chết luồng worker.
                System.err.println("Auto-enrollment failed for mandatory activity " + activityId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Xóa các bản ghi tự động ghi danh chưa điểm danh của hoạt động (thường gọi khi hủy duyệt hoặc đổi tính chất).
     *
     * @param activityId ID hoạt động cần dọn dẹp
     */
    @Override
    public void removeUnattendedAutoEnrolledParticipants(int activityId) {
        targetDAO.removeUnattendedAutoEnrolledParticipants(activityId);
    }

    /**
     * Phân tích danh sách chuỗi ID gửi từ form thành tập hợp số nguyên dương duy nhất.
     *
     * @param rawValues    danh sách chuỗi ID thô
     * @param errorMessage thông báo lỗi nghiệp vụ nếu phát hiện giá trị không phải số nguyên dương
     * @return tập hợp ID hợp lệ giữ nguyên thứ tự chèn
     */
    private Set<Integer> parsePositiveIds(List<String> rawValues, String errorMessage) {
        Set<Integer> ids = new LinkedHashSet<>();
        // Nếu không truyền danh sách tham số thì trả về tập rỗng.
        if (rawValues == null) {
            return ids;
        }
        // Duyệt từng giá trị chuỗi để kiểm tra, ép kiểu và đưa vào tập kết quả.
        for (String raw : rawValues) {
            // Bỏ qua các phần tử rỗng hoặc chỉ chứa khoảng trắng.
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                int id = Integer.parseInt(raw.trim());
                // ID trong cơ sở dữ liệu bắt buộc phải lớn hơn 0.
                if (id <= 0) {
                    throw new IllegalArgumentException(errorMessage);
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                // Chuỗi không đúng định dạng số nguyên thì ném ngoại lệ tham số không hợp lệ.
                throw new IllegalArgumentException(errorMessage);
            }
        }
        return ids;
    }
}
