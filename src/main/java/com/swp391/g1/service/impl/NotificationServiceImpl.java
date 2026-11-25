package com.swp391.g1.service.impl;

import com.swp391.g1.dao.INotificationDAO;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.NotificationDAOImpl;
import com.swp391.g1.dto.response.NotificationDTO;
import com.swp391.g1.model.Enum.ActivityNature;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.service.INotificationService;
import com.swp391.g1.util.DateTimeUtil;
import com.swp391.g1.util.StatusUtil;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp triển khai dịch vụ thông báo ({@link INotificationService}), điều phối việc gửi các loại
 * thông báo nghiệp vụ trong ứng dụng và truy vấn/cập nhật trạng thái đọc của người dùng.
 * Các tác vụ gửi thông báo được bọc try-catch để tránh làm gián đoạn luồng nghiệp vụ chính nếu gặp lỗi.
 */
public class NotificationServiceImpl implements INotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationServiceImpl.class.getName());

    private final INotificationDAO notificationDAO = new NotificationDAOImpl();

    /**
     * Gửi thông báo yêu cầu duyệt hoạt động mới tới tất cả quản trị viên (ADMIN).
     *
     * @param activityId   mã định danh hoạt động ngoại khóa đang chờ duyệt
     * @param activityName tên hoạt động hiển thị trong nội dung thông báo
     */
    @Override
    public void notifyApprovalRequest(int activityId, String activityName) {
        try {
            notificationDAO.createApprovalNotification(
                    "Yêu cầu duyệt hoạt động mới",
                    "Hoạt động \"" + activityName + "\" đang chờ duyệt.",
                    "/activities?action=detail&id=" + activityId);
        } catch (RuntimeException e) {
            // Chỉ ghi log cảnh báo để không làm thất bại thao tác tạo/gửi duyệt hoạt động chính
            LOGGER.log(Level.WARNING, "Could not send admin notification.", e);
        }
    }

    /**
     * Tạo thông báo xác nhận đăng nhập thành công cho tài khoản vừa đăng nhập.
     *
     * @param accountId mã tài khoản đăng nhập
     * @param loginAt   thời điểm đăng nhập (UTC {@link Instant})
     */
    @Override
    public void notifyLoginSuccessful(int accountId, Instant loginAt) {
        try {
            notificationDAO.createLoginNotification(accountId, loginAt);
        } catch (RuntimeException e) {
            // Ghi log kèm thời điểm đã định dạng, không chặn phiên đăng nhập nếu lưu thông báo lỗi
            LOGGER.log(Level.WARNING, "Could not save successful login notification for account "
                    + accountId + " at " + DateTimeUtil.format(loginAt) + ".", e);
        }
    }

    /**
     * Thông báo tới cán bộ phụ trách/hỗ trợ rằng hoạt động đã được duyệt.
     *
     * @param activityId   mã hoạt động ngoại khóa
     * @param activityName tên hoạt động
     */
    @Override
    public void notifyActivityApproved(int activityId, String activityName) {
        notifyActivityStatus(activityId, "ACTIVITY_APPROVED", activityName,
                "Hoạt động đã được duyệt", "đã được duyệt.");
    }

    /**
     * Gửi thông báo cho các sinh viên thuộc đối tượng mục tiêu khi một hoạt động được duyệt,
     * phân biệt nội dung giữa hoạt động đăng ký/tự nguyện và hoạt động bắt buộc.
     *
     * @param activityId   mã hoạt động ngoại khóa
     * @param activityName tên hoạt động
     * @param nature       tính chất hoạt động ({@link ActivityNature})
     * @param isPublic     {@code true} nếu hoạt động hiển thị công khai (dẫn tới trang chi tiết)
     */
    @Override
    public void notifyStudentsActivityApproved(
            int activityId, String activityName, ActivityNature nature, boolean isPublic) {
        // Bỏ qua nếu ID hoạt động không hợp lệ hoặc chưa xác định tính chất hoạt động
        if (activityId <= 0 || nature == null) {
            return;
        }
        try {
            // Chỉ dẫn thẳng tới trang chi tiết khi hoạt động công khai; ngược lại dẫn về trang danh sách
            String targetUrl = isPublic ? ("/activities?action=detail&id=" + activityId) : "/activities";
            // Nhánh hoạt động cần đăng ký hoặc tự nguyện: gửi thông báo hoạt động mới tới sinh viên đủ điều kiện
            if (nature == ActivityNature.REQUIRE_REGISTRATION || nature == ActivityNature.VOLUNTARY) {
                notificationDAO.createEligibleStudentsActivityNotification(
                        activityId,
                        false,
                        "ACTIVITY_NEW",
                        "Hoạt động ngoại khóa mới",
                        "Có hoạt động mới \"" + activityName + "\".",
                        targetUrl);
            // Nhánh hoạt động bắt buộc: bao gồm cả sinh viên đã nằm trong danh sách tham gia (includeParticipants = true)
            } else if (nature == ActivityNature.MANDATORY) {
                notificationDAO.createEligibleStudentsActivityNotification(
                        activityId,
                        true,
                        "ACTIVITY_MANDATORY_ASSIGNED",
                        "Chỉ định tham gia hoạt động mới",
                        "Bạn được chỉ định tham gia hoạt động mới \"" + activityName + "\".",
                        targetUrl);
            }
        } catch (RuntimeException e) {
            // Ghi log cảnh báo để không ảnh hưởng tới quá trình duyệt hoạt động
            LOGGER.log(Level.WARNING, "Could not send student activity approval notification.", e);
        }
    }

    /**
     * Tự động tra cứu thông tin hoạt động từ CSDL rồi gửi thông báo khi trạng thái đợt đăng ký
     * chuyển sang {@code OPENING} hoặc {@code CLOSED}.
     *
     * @param activityId mã hoạt động ngoại khóa
     * @param formId     mã biểu mẫu/đợt đăng ký
     * @param newStatus  trạng thái mới của đợt đăng ký ({@code OPENING} hoặc {@code CLOSED})
     */
    @Override
    public void notifyRegistrationFormStatusChanged(int activityId, int formId, String newStatus) {
        // Chỉ xử lý khi hoạt động hợp lệ và trạng thái mới là mở hoặc đóng đăng ký
        if (activityId <= 0 || (!"OPENING".equals(newStatus) && !"CLOSED".equals(newStatus))) {
            return;
        }
        try {
            ExtracurricularActivity activity = new ExtracurricularActivityDAOImpl().findById(activityId);
            // Không gửi thông báo nếu không tìm thấy bản ghi hoạt động tương ứng
            if (activity == null) {
                return;
            }
            // Chỉ cho phép mở trực tiếp trang chi tiết nếu hoạt động đã được duyệt và đang công khai
            boolean canOpenDetail = activity.getApprovalStatus() == ApprovalStatus.APPROVED && activity.isPublic();
            notifyRegistrationFormStatusChanged(
                    activityId, formId, activity.getName(), newStatus, canOpenDetail);
        } catch (RuntimeException e) {
            // Ghi log cảnh báo nếu tra cứu hoạt động hoặc gửi thông báo gặp lỗi
            LOGGER.log(Level.WARNING, "Could not resolve activity for registration form status notification.", e);
        }
    }

    /**
     * Gửi thông báo tới sinh viên liên quan khi đợt đăng ký của một hoạt động được mở hoặc đóng.
     *
     * @param activityId    mã hoạt động ngoại khóa
     * @param formId        mã biểu mẫu đăng ký
     * @param activityName  tên hoạt động
     * @param newStatus     trạng thái mới ({@code OPENING} hoặc {@code CLOSED})
     * @param canOpenDetail {@code true} nếu sinh viên có thể xem trực tiếp trang chi tiết hoạt động
     */
    @Override
    public void notifyRegistrationFormStatusChanged(
            int activityId, int formId, String activityName, String newStatus, boolean canOpenDetail) {
        // Bỏ qua nếu ID hoạt động không hợp lệ hoặc trạng thái không phải OPENING/CLOSED
        if (activityId <= 0 || (!"OPENING".equals(newStatus) && !"CLOSED".equals(newStatus))) {
            return;
        }
        try {
            // Chọn đường dẫn đích tùy theo việc hoạt động có cho phép mở trang chi tiết hay không
            String targetUrl = canOpenDetail ? ("/activities?action=detail&id=" + activityId) : "/activities";
            // Nhánh mở đợt đăng ký: gửi tới sinh viên đủ điều kiện tham gia hoạt động
            if ("OPENING".equals(newStatus)) {
                notificationDAO.createRegistrationFormStatusNotification(
                        activityId,
                        formId,
                        false,
                        "REGISTRATION_FORM_OPENING",
                        "Đợt đăng ký hoạt động đã mở",
                        "Đợt đăng ký cho hoạt động \"" + activityName + "\" đã mở.",
                        targetUrl);
            // Nhánh đóng đợt đăng ký: gửi tới cả sinh viên đủ điều kiện và sinh viên đã nộp đơn (chưa rút)
            } else {
                notificationDAO.createRegistrationFormStatusNotification(
                        activityId,
                        formId,
                        true,
                        "REGISTRATION_FORM_CLOSED",
                        "Đợt đăng ký hoạt động đã đóng",
                        "Đợt đăng ký cho hoạt động \"" + activityName + "\" đã đóng.",
                        targetUrl);
            }
        } catch (RuntimeException e) {
            // Ghi log cảnh báo khi gửi thông báo trạng thái đợt đăng ký thất bại
            LOGGER.log(Level.WARNING, "Could not send registration form status notification.", e);
        }
    }

    /**
     * Gửi thông báo cập nhật trạng thái điểm danh tới danh sách tài khoản sinh viên bị ảnh hưởng.
     *
     * @param activityId        mã hoạt động ngoại khóa
     * @param activityName      tên hoạt động
     * @param sessionNumber     số thứ tự buổi điểm danh
     * @param totalSessions     tổng số buổi của hoạt động (dùng quyết định có hiển thị nhãn buổi hay không)
     * @param oldStatus         mã trạng thái điểm danh cũ
     * @param newStatus         mã trạng thái điểm danh mới
     * @param studentAccountIds danh sách ID tài khoản sinh viên cần nhận thông báo
     */
    @Override
    public void notifyAttendanceStatusChanged(
            int activityId, String activityName, int sessionNumber, int totalSessions,
            String oldStatus, String newStatus, List<Integer> studentAccountIds) {
        // Bỏ qua nếu hoạt động không hợp lệ hoặc không có tài khoản sinh viên nào để gửi
        if (activityId <= 0 || studentAccountIds == null || studentAccountIds.isEmpty()) {
            return;
        }
        try {
            // Chuẩn hóa số buổi tối thiểu là 1 và chỉ gắn nhãn " buổi X" khi hoạt động có nhiều hơn 1 buổi
            int safeSession = Math.max(1, sessionNumber);
            String sessionLabel = totalSessions > 1 ? (" buổi " + safeSession) : "";
            String content = "Trạng thái điểm danh" + sessionLabel + " của bạn trong hoạt động \""
                    + activityName + "\" đã thay đổi từ \"" + StatusUtil.label(oldStatus)
                    + "\" sang \"" + StatusUtil.label(newStatus) + "\".";
            String targetUrl = "/attendance?action=detail&activityId=" + activityId + "&sessionNumber=" + safeSession;
            notificationDAO.createStudentAccountsNotification(
                    studentAccountIds,
                    "ATTENDANCE_STATUS_CHANGED",
                    "Cập nhật trạng thái điểm danh",
                    content,
                    targetUrl);
        } catch (RuntimeException e) {
            // Ghi log cảnh báo để không làm lỗi thao tác lưu điểm danh chính
            LOGGER.log(Level.WARNING, "Could not send attendance status change notification.", e);
        }
    }

    /**
     * Thông báo tới cán bộ phụ trách/hỗ trợ rằng hoạt động đã bị từ chối duyệt.
     *
     * @param activityId   mã hoạt động ngoại khóa
     * @param activityName tên hoạt động
     */
    @Override
    public void notifyActivityRejected(int activityId, String activityName) {
        notifyActivityStatus(activityId, "ACTIVITY_REJECTED", activityName,
                "Hoạt động đã bị từ chối", "đã bị từ chối.");
    }

    /**
     * Thông báo tới cán bộ phụ trách/hỗ trợ rằng quyết định duyệt hoạt động đã bị hủy
     * và hoạt động quay về trạng thái Chờ duyệt.
     *
     * @param activityId   mã hoạt động ngoại khóa
     * @param activityName tên hoạt động
     */
    @Override
    public void notifyApprovalRevoked(int activityId, String activityName) {
        notifyActivityStatus(activityId, "ACTIVITY_APPROVAL_REVOKED", activityName,
                "Đã hủy duyệt hoạt động", "đã bị hủy quyết định duyệt và chuyển về trạng thái Chờ duyệt.");
    }

    /**
     * Thông báo tới cán bộ phụ trách/hỗ trợ rằng quyết định từ chối hoạt động đã được hủy
     * và hoạt động quay về trạng thái Chờ duyệt.
     *
     * @param activityId   mã hoạt động ngoại khóa
     * @param activityName tên hoạt động
     */
    @Override
    public void notifyRejectionRevoked(int activityId, String activityName) {
        notifyActivityStatus(activityId, "ACTIVITY_REJECTION_REVOKED", activityName,
                "Đã hủy từ chối hoạt động", "đã được hủy quyết định từ chối và chuyển về trạng thái Chờ duyệt.");
    }

    /**
     * Gửi thông báo lời mời hợp tác hoạt động mới tới phía nhân sự công ty đối tác.
     *
     * @param invitationId mã lời mời hợp tác
     * @param activityName tên hoạt động ngoại khóa
     * @param companyName  tên công ty đối tác được mời
     */
    @Override
    public void notifyPartnerInvitationSent(int invitationId, String activityName, String companyName) {
        notifyPartnerInvitation(invitationId, "PARTNER_COLLABORATION_INVITATION",
                "Lời mời hợp tác hoạt động mới",
                "Công ty \"" + companyName + "\" được mời hợp tác hoạt động \"" + activityName + "\".");
    }

    /**
     * Gửi thông báo yêu cầu ngừng hợp tác hoạt động tới phía nhân sự công ty đối tác.
     *
     * @param invitationId mã lời mời hợp tác
     * @param activityName tên hoạt động ngoại khóa
     * @param companyName  tên công ty đối tác
     */
    @Override
    public void notifyPartnerTerminationRequested(int invitationId, String activityName, String companyName) {
        notifyPartnerInvitation(invitationId, "PARTNER_TERMINATION_REQUEST",
                "Yêu cầu ngừng hợp tác",
                "Công ty \"" + companyName + "\" nhận được yêu cầu ngừng hợp tác trong hoạt động \""
                        + activityName + "\".");
    }

    /**
     * Gửi thông báo cập nhật liên quan đến lời mời đối tác về cho cán bộ phụ trách chính của hoạt động.
     *
     * @param invitationId mã lời mời hợp tác
     * @param eventType    mã loại sự kiện thông báo
     * @param title        tiêu đề thông báo
     * @param content      nội dung thông báo
     */
    @Override
    public void notifyPartnerInvitationOwner(int invitationId, String eventType, String title, String content) {
        try {
            notificationDAO.createPartnerInvitationOwnerNotification(
                    invitationId, eventType, title, content, "/activities");
        } catch (RuntimeException e) {
            // Ghi log cảnh báo khi không gửi được thông báo cho cán bộ phụ trách hoạt động
            LOGGER.log(Level.WARNING, "Could not send partner invitation update notification.", e);
        }
    }

    /**
     * Hàm hỗ trợ nội bộ gửi thông báo lời mời/ngừng hợp tác tới các nhân sự đối tác liên quan,
     * gắn đường dẫn đích mặc định {@code /partner-invitations}.
     *
     * @param invitationId mã lời mời hợp tác
     * @param eventType    mã loại sự kiện thông báo
     * @param title        tiêu đề thông báo
     * @param content      nội dung thông báo
     */
    private void notifyPartnerInvitation(int invitationId, String eventType, String title, String content) {
        try {
            notificationDAO.createPartnerInvitationNotification(
                    invitationId, eventType, title, content, "/partner-invitations");
        } catch (RuntimeException e) {
            // Ghi log cảnh báo nếu tạo thông báo cho phía đối tác gặp lỗi
            LOGGER.log(Level.WARNING, "Could not send partner invitation notification.", e);
        }
    }

    /**
     * Hàm hỗ trợ nội bộ gửi thông báo thay đổi trạng thái duyệt hoạt động tới đội ngũ cán bộ của hoạt động.
     *
     * @param activityId    mã hoạt động ngoại khóa
     * @param eventType     mã loại sự kiện thông báo
     * @param activityName  tên hoạt động
     * @param title         tiêu đề thông báo
     * @param statusMessage phần mô tả trạng thái nối sau tên hoạt động
     */
    private void notifyActivityStatus(
            int activityId, String eventType, String activityName, String title, String statusMessage) {
        try {
            notificationDAO.createActivityStatusNotification(
                    activityId,
                    eventType,
                    title,
                    "Hoạt động \"" + activityName + "\" " + statusMessage,
                    "/activities?action=detail&id=" + activityId);
        } catch (RuntimeException e) {
            // Ghi log cảnh báo để không ảnh hưởng luồng cập nhật trạng thái hoạt động
            LOGGER.log(Level.WARNING, "Could not send activity status notification.", e);
        }
    }

    /**
     * Gửi thông báo tùy chỉnh tới một sinh viên cụ thể theo mã sinh viên.
     *
     * @param studentId mã sinh viên (bảng {@code student})
     * @param eventType mã loại sự kiện thông báo
     * @param title     tiêu đề thông báo
     * @param content   nội dung thông báo
     * @param targetUrl đường dẫn điều hướng khi người dùng nhấn vào thông báo
     */
    @Override
    public void notifyStudent(int studentId, String eventType, String title, String content, String targetUrl) {
        try {
            notificationDAO.createStudentNotification(studentId, eventType, title, content, targetUrl);
        } catch (RuntimeException e) {
            // Ghi log cảnh báo nếu gửi thông báo cho sinh viên gặp lỗi
            LOGGER.log(Level.WARNING, "Could not send student notification.", e);
        }
    }

    /**
     * Gửi thông báo tùy chỉnh trực tiếp tới một tài khoản theo mã tài khoản.
     *
     * @param accountId mã tài khoản (bảng {@code account})
     * @param eventType mã loại sự kiện thông báo
     * @param title     tiêu đề thông báo
     * @param content   nội dung thông báo
     * @param targetUrl đường dẫn điều hướng khi người dùng nhấn vào thông báo
     */
    @Override
    public void notifyAccount(int accountId, String eventType, String title, String content, String targetUrl) {
        try {
            notificationDAO.createAccountNotification(accountId, eventType, title, content, targetUrl);
        } catch (RuntimeException e) {
            // Ghi log cảnh báo nếu gửi thông báo tới tài khoản gặp lỗi
            LOGGER.log(Level.WARNING, "Could not send account notification.", e);
        }
    }

    /**
     * Gửi thông báo tùy chỉnh tới toàn bộ cán bộ phụ trách và cán bộ hỗ trợ của một hoạt động.
     *
     * @param activityId mã hoạt động ngoại khóa
     * @param eventType  mã loại sự kiện thông báo
     * @param title      tiêu đề thông báo
     * @param content    nội dung thông báo
     * @param targetUrl  đường dẫn điều hướng khi người dùng nhấn vào thông báo
     */
    @Override
    public void notifyActivityStaff(int activityId, String eventType, String title, String content,
                                    String targetUrl) {
        try {
            notificationDAO.createActivityStatusNotification(activityId, eventType, title, content, targetUrl);
        } catch (RuntimeException e) {
            // Ghi log cảnh báo nếu gửi thông báo tới cán bộ hoạt động gặp lỗi
            LOGGER.log(Level.WARNING, "Could not send activity staff notification.", e);
        }
    }

    /**
     * Gửi thông báo tùy chỉnh tới toàn bộ sinh viên đã nằm trong danh sách tham gia hoạt động.
     *
     * @param activityId mã hoạt động ngoại khóa
     * @param eventType  mã loại sự kiện thông báo
     * @param title      tiêu đề thông báo
     * @param content    nội dung thông báo
     * @param targetUrl  đường dẫn điều hướng khi người dùng nhấn vào thông báo
     */
    @Override
    public void notifyActivityParticipants(int activityId, String eventType, String title, String content,
                                           String targetUrl) {
        try {
            notificationDAO.createActivityParticipantsNotification(
                    activityId, eventType, title, content, targetUrl);
        } catch (RuntimeException e) {
            // Ghi log cảnh báo nếu gửi thông báo tới danh sách người tham gia gặp lỗi
            LOGGER.log(Level.WARNING, "Could not send activity participants notification.", e);
        }
    }

    /**
     * Lấy danh sách thông báo có phân trang của một tài khoản theo bộ lọc trạng thái đọc.
     *
     * @param accountId mã tài khoản người dùng
     * @param filter    bộ lọc trạng thái ({@code all}, {@code unread}, {@code read})
     * @param page      số trang (bắt đầu từ 1)
     * @param size      số bản ghi mỗi trang (tự động giới hạn trong khoảng 1..50)
     * @return danh sách {@link NotificationDTO} của trang yêu cầu
     */
    @Override
    public List<NotificationDTO> getNotifications(int accountId, String filter, int page, int size) {
        // Kẹp kích thước trang trong đoạn [1, 50] và tính vị trí offset an toàn (page >= 1)
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = (Math.max(page, 1) - 1) * safeSize;
        return notificationDAO.findByAccount(accountId, filter, offset, safeSize);
    }

    /**
     * Đếm tổng số thông báo của tài khoản thỏa mãn bộ lọc trạng thái đọc.
     *
     * @param accountId mã tài khoản người dùng
     * @param filter    bộ lọc trạng thái ({@code all}, {@code unread}, {@code read})
     * @return tổng số thông báo khớp điều kiện
     */
    @Override
    public int countNotifications(int accountId, String filter) {
        return notificationDAO.countByAccount(accountId, filter);
    }

    /**
     * Đếm số thông báo chưa đọc của một tài khoản.
     *
     * @param accountId mã tài khoản người dùng
     * @return số lượng thông báo chưa đọc
     */
    @Override
    public int countUnread(int accountId) {
        return notificationDAO.countUnread(accountId);
    }

    /**
     * Đánh dấu một thông báo cụ thể của tài khoản là đã đọc.
     *
     * @param accountId      mã tài khoản người dùng
     * @param notificationId mã thông báo cần đánh dấu
     * @return {@code true} nếu cập nhật thành công (trước đó chưa đọc), ngược lại {@code false}
     */
    @Override
    public boolean markRead(int accountId, int notificationId) {
        return notificationDAO.markRead(accountId, notificationId);
    }

    /**
     * Đánh dấu toàn bộ thông báo chưa đọc của tài khoản là đã đọc.
     *
     * @param accountId mã tài khoản người dùng
     * @return số lượng thông báo đã được chuyển sang trạng thái đã đọc
     */
    @Override
    public int markAllRead(int accountId) {
        return notificationDAO.markAllRead(accountId);
    }
}