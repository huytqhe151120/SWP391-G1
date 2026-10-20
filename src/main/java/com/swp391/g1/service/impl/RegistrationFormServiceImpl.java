package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IRegistrationFormDAO;
import com.swp391.g1.dao.impl.RegistrationFormDAOImpl;
import com.swp391.g1.model.Enum.RegistrationFormStatus;
import com.swp391.g1.model.RegistrationForm;
import com.swp391.g1.service.IRegistrationFormService;

import java.util.List;

public class RegistrationFormServiceImpl implements IRegistrationFormService {
    private final IRegistrationFormDAO dao = new RegistrationFormDAOImpl();

    public List<RegistrationForm> getAll() {
        return dao.findAll();
    }

    public List<RegistrationForm> getOpenForms() {
        return dao.findOpenForms();
    }

    public RegistrationForm getById(int id) {
        return id > 0 ? dao.findById(id) : null;
    }

    public String save(RegistrationForm f) {
        if (f.getActivityId() <= 0) return "Vui lòng chọn hoạt động.";
        if (f.getTitle() == null || f.getTitle().isBlank()) return "Vui lòng nhập tiêu đề form.";
        if (f.getTitle().trim().length() > 200) return "Tiêu đề không được vượt quá 200 ký tự.";
        if (f.getDescription() != null && f.getDescription().length() > 4000)
            return "Mô tả không được vượt quá 4.000 ký tự.";
        if (f.getOpenAt() != null && f.getCloseAt() != null && !f.getCloseAt().isAfter(f.getOpenAt()))
            return "Thời gian đóng phải sau thời gian mở.";
        f.setTitle(f.getTitle().trim());
        f.setDescription(f.getDescription() == null ? "" : f.getDescription().trim());
        if (f.getStatus() == null) return "Vui lòng chọn trạng thái form.";
        try {
            RegistrationFormStatus.valueOf(f.getStatus());
        } catch (IllegalArgumentException e) {
            return "Trạng thái form không hợp lệ.";
        }
        if (f.getId() > 0)
            return dao.update(f) ? null : "Không thể sửa form đã có sinh viên đăng ký hoặc form không tồn tại.";
        return dao.insert(f) ? null : "Không thể tạo form đăng ký.";
    }

    public String updateStatus(int id, String status) {
        if (id <= 0) return "Form đăng ký không hợp lệ.";
        if (status == null) return "Trạng thái form không hợp lệ.";
        try {
            RegistrationFormStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return "Trạng thái form không hợp lệ.";
        }
        return dao.updateStatus(id, status) ? null : "Không tìm thấy form đăng ký.";
    }

    public String delete(int id) {
        return dao.delete(id) ? null : "Không thể xóa form đã có đăng ký. Hãy chuyển form sang bản nháp thay vì xóa.";
    }

    public String register(int formId, int studentId, String note) {
        if (formId <= 0 || studentId <= 0) return "Thông tin đăng ký không hợp lệ.";
        if (note != null && note.length() > 2000) return "Ghi chú không được vượt quá 2.000 ký tự.";
        if (dao.registerStudent(formId, studentId, note == null ? "" : note.trim())) return null;
        return "Không thể đăng ký: form đã đóng, hoạt động chưa được duyệt hoặc bạn đã đăng ký trước đó.";
    }
}
