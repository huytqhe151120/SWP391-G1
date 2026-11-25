package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityCollaborationInvitationDAO;
import com.swp391.g1.dao.IActivityPartnerCompanyDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.IPartnerCompanyDAO;
import com.swp391.g1.dao.IPartnerStaffDAO;
import com.swp391.g1.dao.impl.ActivityCollaborationInvitationDAOImpl;
import com.swp391.g1.dao.impl.ActivityPartnerCompanyDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.PartnerCompanyDAOImpl;
import com.swp391.g1.dao.impl.PartnerStaffDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityCollaborationInvitationDTO;
import com.swp391.g1.dto.response.ActivityPartnerCompanyDTO;
import com.swp391.g1.model.Enum.CommonStatus;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.PartnerCompany;
import com.swp391.g1.model.PartnerStaff;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityCollaborationInvitationService;
import com.swp391.g1.service.INotificationService;
import com.swp391.g1.util.DateTimeUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ActivityCollaborationInvitationServiceImpl implements IActivityCollaborationInvitationService {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final IActivityCollaborationInvitationDAO invitationDAO = new ActivityCollaborationInvitationDAOImpl();
    private final IActivityPartnerCompanyDAO partnerCompanyLinkDAO = new ActivityPartnerCompanyDAOImpl();
    private final IExtracurricularActivityDAO activityDAO = new ExtracurricularActivityDAOImpl();
    private final IPartnerCompanyDAO partnerCompanyDAO = new PartnerCompanyDAOImpl();
    private final IPartnerStaffDAO partnerStaffDAO = new PartnerStaffDAOImpl();
    private final INotificationService notificationService = new NotificationServiceImpl();

    /**
     * Lấy danh sách tất cả lời mời phối hợp của một hoạt động và gán cờ quyền phản hồi/hủy phản hồi
     * tương ứng với người dùng hiện tại.
     *
     * @param activityId mã định danh của hoạt động ngoại khóa
     * @param user       người dùng đang đăng nhập
     * @return danh sách DTO lời mời phối hợp (rỗng nếu activityId không hợp lệ)
     */
    @Override
    public List<ActivityCollaborationInvitationDTO> getInvitations(int activityId, CurrentUser user) {
        // Trả về danh sách rỗng nếu ID hoạt động không hợp lệ
        if (activityId <= 0) {
            return List.of();
        }
        // Tự động hủy các lời mời đang chờ thuộc những hoạt động đã bắt đầu, kết thúc hoặc bị hủy
        invitationDAO.cancelPendingForInactiveActivities();
        List<ActivityCollaborationInvitationDTO> invitations = invitationDAO.findByActivityId(activityId);
        // Nếu người dùng là nhân sự đối tác thì tra cứu hồ sơ PartnerStaff tương ứng để kiểm tra quyền
        PartnerStaff me = isPartnerStaffUser(user) ? partnerStaffDAO.findByAccountId(user.getId()) : null;
        Set<Integer> activeCompanyIds = activeCompanyIds(activityId);
        // Duyệt từng lời mời để tính toán cờ cho phép phản hồi và hủy phản hồi cho giao diện
        for (ActivityCollaborationInvitationDTO invitation : invitations) {
            invitation.setCanRespond(canRespond(invitation, user, me));
            invitation.setCanRevoke(canRevoke(invitation, user, me, activeCompanyIds));
        }
        return invitations;
    }

    /**
     * Lấy danh sách lời mời phối hợp trong phạm vi được phép xem của người dùng hiện tại
     * (nhân sự đối tác chỉ thấy lời mời của công ty mình; quản trị/nhân viên trường thấy tất cả).
     *
     * @param user người dùng đang đăng nhập
     * @return danh sách DTO lời mời kèm cờ quyền thao tác
     */
    @Override
    public List<ActivityCollaborationInvitationDTO> getInvitationsForUser(CurrentUser user) {
        // Trả về rỗng nếu chưa đăng nhập
        if (user == null) {
            return List.of();
        }
        // Nếu là tài khoản nhân sự đối tác thì lấy hồ sơ nhân sự gắn với account
        PartnerStaff me = isPartnerStaffUser(user) ? partnerStaffDAO.findByAccountId(user.getId()) : null;
        // Nếu là nhân sự đối tác nhưng chưa liên kết hồ sơ PartnerStaff thì không hiển thị lời mời nào
        if (isPartnerStaffUser(user) && me == null) {
            return List.of();
        }
        // Đồng bộ hủy các lời mời chờ thuộc những hoạt động không còn ở trạng thái UPCOMING
        invitationDAO.cancelPendingForInactiveActivities();
        // Lọc theo công ty của nhân sự đối tác, hoặc truyền null để lấy toàn bộ cho tài khoản nhà trường
        List<ActivityCollaborationInvitationDTO> invitations =
                invitationDAO.findByPartnerCompanyId(me == null ? null : me.getCompanyId());
        // Bộ nhớ đệm danh sách công ty đang hợp tác theo từng activityId để tránh truy vấn lặp lại
        java.util.Map<Integer, Set<Integer>> activeByActivity = new java.util.HashMap<>();
        // Duyệt từng lời mời để gán quyền phản hồi và hủy phản hồi
        for (ActivityCollaborationInvitationDTO invitation : invitations) {
            Set<Integer> activeCompanyIds = activeByActivity.computeIfAbsent(
                    invitation.getActivityId(), this::activeCompanyIds);
            invitation.setCanRespond(canRespond(invitation, user, me));
            invitation.setCanRevoke(canRevoke(invitation, user, me, activeCompanyIds));
        }
        return invitations;
    }

    /**
     * Lấy tập ID các hoạt động mà người dùng đối tác được phép nhìn thấy
     * (trả về null nếu không phải nhân sự đối tác, nghĩa là không giới hạn theo phạm vi đối tác).
     *
     * @param user người dùng đang đăng nhập
     * @return tập ID hoạt động được phép xem, hoặc null nếu không áp dụng lọc theo đối tác
     */
    @Override
    public java.util.Set<Integer> getVisibleActivityIds(CurrentUser user) {
        // Không giới hạn tập ID đối với các vai trò không phải nhân sự đối tác
        if (!isPartnerStaffUser(user)) {
            return null;
        }
        PartnerStaff me = partnerStaffDAO.findByAccountId(user.getId());
        // Trả về tập rỗng nếu chưa có hồ sơ nhân sự; ngược lại truy vấn các hoạt động liên quan đến công ty
        return me == null ? Set.of() : invitationDAO.findVisibleActivityIds(me.getCompanyId());
    }

    /**
     * Lấy tập ID các hoạt động mà nhân sự đối tác được phép truy cập phạm vi điểm danh.
     *
     * @param user người dùng đang đăng nhập
     * @return tập ID hoạt động được phép điểm danh, tập rỗng nếu không đủ quyền, hoặc null nếu không phải đối tác
     */
    @Override
    public java.util.Set<Integer> getAttendanceActivityIds(CurrentUser user) {
        // Không áp dụng phạm vi điểm danh đối tác cho người dùng nội bộ trường
        if (!isPartnerStaffUser(user)) {
            return null;
        }
        // Kiểm tra vai trò có được phép sử dụng phạm vi điểm danh của đối tác hay không
        if (!AccessPolicy.canUsePartnerAttendanceScope(user)) {
            return Set.of();
        }
        PartnerStaff me = partnerStaffDAO.findByAccountId(user.getId());
        // Từ chối nếu không tìm thấy hồ sơ nhân sự đối tác hoặc nhân sự không còn ACTIVE
        if (me == null || me.getStatus() != CommonStatus.ACTIVE) {
            return Set.of();
        }
        // Quản lý/thư ký công ty được xem toàn bộ hoạt động của công ty; nhân sự thường chỉ xem hoạt động được phân công
        boolean companyWide = AccessPolicy.canAssignPartnerStaffToActivity(user);
        return invitationDAO.findAttendanceActivityIds(me.getCompanyId(), me.getId(), companyWide);
    }

    /**
     * Lấy danh sách các công ty đối tác đã được duyệt/gắn với một hoạt động.
     *
     * @param activityId mã định danh hoạt động
     * @return danh sách liên kết công ty đối tác của hoạt động
     */
    @Override
    public List<ActivityPartnerCompanyDTO> getApprovedPartners(int activityId) {
        // Trả về danh sách rỗng nếu ID hoạt động không hợp lệ
        return activityId <= 0 ? List.of() : partnerCompanyLinkDAO.findByActivityId(activityId);
    }

    /**
     * Tạo và gửi lời mời hợp tác từ nhà trường đến một công ty đối tác cho hoạt động sắp diễn ra.
     *
     * @param activityId       ID hoạt động ngoại khóa
     * @param partnerCompanyId chuỗi ID công ty đối tác được chọn
     * @param partnerStaffId   chuỗi ID nhân sự đối tác phụ trách chính
     * @param message          lời nhắn đính kèm lời mời
     * @param user             người thực hiện gửi lời mời
     */
    @Override
    public void invite(int activityId, String partnerCompanyId, String partnerStaffId, String message,
                       CurrentUser user) {
        // Kiểm tra quyền quản lý hoạt động trước khi cho phép gửi lời mời đối tác
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new SecurityException("Bạn không có quyền mời đối tác tham gia hoạt động.");
        }

        // Xác thực hoạt động tồn tại, đang UPCOMING, chưa bắt đầu và đã được phê duyệt
        ExtracurricularActivity activity = requireUpcomingActivity(activityId);
        // Chỉ cho phép gửi lời mời khi hoạt động đã ở trạng thái duyệt APPROVED
        if (activity.getApprovalStatus() != com.swp391.g1.model.Enum.ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Chỉ có thể tạo lời mời cho hoạt động đã được duyệt.");
        }
        int companyId = parseId(partnerCompanyId, "Vui lòng chọn công ty đối tác.");
        int staffId = parseId(partnerStaffId, "Vui lòng chọn nhân sự đối tác phụ trách chính.");
        String note = requireMessage(message);

        // Kiểm tra công ty đối tác tồn tại và đang hoạt động
        PartnerCompany company = partnerCompanyDAO.findById(companyId);
        if (company == null || company.getStatus() != CommonStatus.ACTIVE) {
            throw new IllegalArgumentException("Công ty đối tác không tồn tại hoặc không còn hoạt động.");
        }
        // Kiểm tra nhân sự đối tác tồn tại, đang hoạt động và thuộc đúng công ty đã chọn
        PartnerStaff staff = partnerStaffDAO.findById(staffId);
        if (staff == null || staff.getStatus() != CommonStatus.ACTIVE || staff.getCompanyId() != companyId) {
            throw new IllegalArgumentException(
                    "Nhân sự đối tác phải đang hoạt động và thuộc công ty đối tác đã chọn.");
        }
        // Không cho phép gửi trùng lời mời nếu công ty đang có lời mời chờ phản hồi hoặc đang hợp tác ACTIVE
        if (invitationDAO.existsOpenInvitation(activityId, companyId)) {
            throw new IllegalArgumentException(
                    "Công ty đối tác này đã được mời hoặc đang hợp tác trong hoạt động.");
        }
        // Lưu lời mời hợp tác vào CSDL với điều kiện nguyên tử kiểm tra lại trạng thái UPCOMING
        int invitationId = invitationDAO.insert(
                activityId,
                companyId,
                staffId,
                String.valueOf(user.getId()),
                IActivityCollaborationInvitationDAO.TYPE_COLLABORATION,
                note);
        // Báo lỗi nếu không chèn được bản ghi (ví dụ hoạt động vừa chuyển trạng thái hoặc đã quá giờ bắt đầu)
        if (invitationId <= 0) {
            throw new IllegalArgumentException(
                    "Không thể gửi lời mời. Hoạt động có thể đã bắt đầu "
                            + "hoặc không còn ở trạng thái UPCOMING.");
        }
        // Gửi thông báo đến phía đối tác về lời mời hợp tác mới
        notificationService.notifyPartnerInvitationSent(invitationId, activity.getName(), company.getName());
    }

    /**
     * Gửi yêu cầu ngừng hợp tác đến một công ty đối tác đang tham gia hoạt động sắp diễn ra.
     *
     * @param activityId               ID hoạt động ngoại khóa
     * @param activityPartnerCompanyId ID bản ghi liên kết giữa hoạt động và công ty đối tác
     * @param message                  lý do/lời nhắn yêu cầu ngừng hợp tác
     * @param user                     người thực hiện gửi yêu cầu
     */
    @Override
    public void requestTermination(int activityId, int activityPartnerCompanyId, String message, CurrentUser user) {
        // Kiểm tra quyền quản lý hoạt động trước khi gửi yêu cầu ngừng hợp tác
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new SecurityException("Bạn không có quyền gửi lời mời ngừng hợp tác.");
        }
        ExtracurricularActivity activity = requireUpcomingActivity(activityId);
        String note = requireMessage(message);
        // Tra cứu bản ghi liên kết đối tác - hoạt động nếu ID hợp lệ
        ActivityPartnerCompanyDTO link = activityPartnerCompanyId <= 0
                ? null : partnerCompanyLinkDAO.findById(activityPartnerCompanyId);
        // Kiểm tra liên kết tồn tại và thuộc đúng hoạt động đang thao tác
        if (link == null || link.getActivityId() != activityId) {
            throw new IllegalArgumentException("Không tìm thấy đối tác của hoạt động.");
        }
        // Chỉ gửi yêu cầu ngừng hợp tác cho đối tác đang ở trạng thái hợp tác ACTIVE
        if (!link.isActive()) {
            throw new IllegalArgumentException("Đối tác này hiện không trong trạng thái hợp tác.");
        }
        // Chặn tạo thêm yêu cầu mới nếu đối tác này đang có một lời mời khác chờ phản hồi
        if (invitationDAO.existsPendingInvitation(activityId, link.getPartnerCompanyId())) {
            throw new IllegalArgumentException("Đối tác này đang có lời mời chờ phản hồi.");
        }

        // Lưu lời mời loại TERMINATION vào CSDL
        int invitationId = invitationDAO.insert(
                activityId,
                link.getPartnerCompanyId(),
                link.getPartnerStaffId(),
                String.valueOf(user.getId()),
                IActivityCollaborationInvitationDAO.TYPE_TERMINATION,
                note);
        // Báo lỗi nếu CSDL từ chối chèn do hoạt động đã bắt đầu hoặc không còn UPCOMING
        if (invitationId <= 0) {
            throw new IllegalArgumentException(
                    "Không thể gửi yêu cầu ngừng hợp tác. Hoạt động có thể đã bắt đầu "
                            + "hoặc không còn ở trạng thái UPCOMING."
            );
        }
        // Gửi thông báo đến phía đối tác về yêu cầu ngừng hợp tác
        notificationService.notifyPartnerTerminationRequested(
                invitationId, activity.getName(), link.getPartnerCompanyName());
    }

    /**
     * Phản hồi (chấp nhận hoặc từ chối) một lời mời hợp tác hoặc yêu cầu ngừng hợp tác đang chờ xử lý.
     *
     * @param invitationId ID lời mời cần phản hồi
     * @param accept       true nếu chấp nhận, false nếu từ chối
     * @param user         người dùng phía đối tác thực hiện phản hồi
     */
    @Override
    public void respond(int invitationId, boolean accept, CurrentUser user) {
        ActivityCollaborationInvitationDTO invitation = requirePending(invitationId);

        // Kiểm tra hoạt động còn cho phép phản hồi lời mời không.
        requireUpcomingActivity(invitation.getActivityId());

        // Lấy hồ sơ nhân sự đối tác nếu tài khoản hiện tại thuộc loại PARTNER_STAFF
        PartnerStaff me = isPartnerStaffUser(user)
                ? partnerStaffDAO.findByAccountId(user.getId())
                : null;

        // Xác thực người dùng có quyền phản hồi lời mời này (người được mời hoặc quản lý/thư ký công ty)
        if (!canRespond(invitation, user, me)) {
            throw new SecurityException(
                    "Bạn không có quyền phản hồi lời mời này."
            );
        }

        // Thực hiện cập nhật trạng thái lời mời sang APPROVED hoặc REJECTED trong CSDL
        boolean updated = accept
                ? invitationDAO.accept(invitationId)
                : invitationDAO.reject(invitationId);
        // Báo lỗi nếu không cập nhật được do lời mời đã bị xử lý song song hoặc hoạt động vừa bắt đầu
        if (!updated) {
            throw new IllegalArgumentException(
                    "Không thể phản hồi lời mời vì lời mời đã được xử lý hoặc hoạt động đã bắt đầu.");
        }
        // Dựng nội dung thông báo phù hợp theo loại lời mời (hợp tác hay ngừng hợp tác) và kết quả phản hồi
        String action = accept ? "đã chấp nhận" : "đã từ chối";
        boolean termination = isTermination(invitation);
        String invitationLabel = termination ? "yêu cầu ngừng hợp tác" : "lời mời hợp tác";
        String eventType = accept ? "PARTNER_INVITATION_ACCEPTED" : "PARTNER_INVITATION_REJECTED";
        // Gửi thông báo kết quả phản hồi về cho người tạo lời mời phía nhà trường
        notificationService.notifyPartnerInvitationOwner(
                invitationId,
                eventType,
                termination
                        ? (accept ? "Đối tác đã chấp nhận yêu cầu ngừng hợp tác"
                                : "Đối tác đã từ chối yêu cầu ngừng hợp tác")
                        : (accept ? "Đối tác đã chấp nhận lời mời" : "Đối tác đã từ chối lời mời"),
                "Công ty \"" + invitation.getPartnerCompanyName() + "\" " + action + " "
                        + invitationLabel + " cho hoạt động \"" + invitation.getActivityName() + "\".");
    }

    /**
     * Hủy một lời mời đang ở trạng thái chờ phản hồi (dành cho cán bộ quản lý hoạt động).
     *
     * @param invitationId ID lời mời cần hủy
     * @param user         người dùng thực hiện hủy
     */
    @Override
    public void cancel(int invitationId, CurrentUser user) {
        // Kiểm tra quyền quản lý hoạt động trước khi hủy lời mời
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new SecurityException("Bạn không có quyền hủy lời mời này.");
        }
        requirePending(invitationId);
        // Thực hiện chuyển trạng thái lời mời sang CANCELLED; báo lỗi nếu thao tác thất bại
        if (!invitationDAO.cancel(invitationId)) {
            throw new IllegalArgumentException(
                    "Không thể hủy lời mời vì lời mời đã được xử lý hoặc hoạt động đã bắt đầu.");
        }
    }

    /**
     * Thu hồi (hủy) phản hồi trước đó (đã chấp nhận hoặc đã từ chối) để đưa lời mời trở lại trạng thái PENDING.
     *
     * @param invitationId ID lời mời cần hủy phản hồi
     * @param user         người dùng phía đối tác thực hiện thu hồi
     */
    @Override
    public void revoke(int invitationId, CurrentUser user) {
        // Tra cứu lời mời nếu ID hợp lệ
        ActivityCollaborationInvitationDTO invitation = invitationId <= 0 ? null : invitationDAO.findById(invitationId);
        // Báo lỗi nếu không tìm thấy lời mời
        if (invitation == null) {
            throw new IllegalArgumentException("Không tìm thấy lời mời.");
        }
        // Lấy thông tin nhân sự đối tác và danh sách công ty đang hợp tác ACTIVE của hoạt động
        PartnerStaff me = isPartnerStaffUser(user) ? partnerStaffDAO.findByAccountId(user.getId()) : null;
        Set<Integer> activeCompanyIds = activeCompanyIds(invitation.getActivityId());
        // Kiểm tra điều kiện và quyền được phép hủy phản hồi lời mời này
        if (!canRevoke(invitation, user, me, activeCompanyIds)) {
            throw new SecurityException("Bạn không thể hủy phản hồi của lời mời này.");
        }
        boolean approved = IActivityCollaborationInvitationDAO.APPROVED.equalsIgnoreCase(invitation.getApprovalStatus());
        // Không cho thu hồi nếu công ty này đang có lời mời khác ở trạng thái PENDING trong cùng hoạt động
        if (invitationDAO.existsPendingInvitation(invitation.getActivityId(), invitation.getPartnerCompanyId())) {
            throw new IllegalArgumentException("Công ty đối tác này đang có lời mời khác chờ phản hồi trong hoạt động.");
        }
        // Nếu là lời mời hợp tác đã bị từ chối nhưng công ty hiện đã hợp tác ACTIVE qua lời mời khác thì không cho thu hồi
        if (!approved && !isTermination(invitation)
                && activeCompanyIds.contains(invitation.getPartnerCompanyId())) {
            throw new IllegalArgumentException("Công ty đối tác này đang hợp tác trong hoạt động.");
        }
        // Gọi DAO thu hồi chấp nhận hoặc thu hồi từ chối tùy trạng thái hiện tại của lời mời
        boolean updated = approved ? invitationDAO.revokeApproval(invitationId) : invitationDAO.revokeRejection(invitationId);
        // Báo lỗi nếu lời mời đã bị thay đổi trước đó hoặc hoạt động đã bắt đầu
        if (!updated) {
            throw new IllegalArgumentException(
                    "Không thể hủy phản hồi vì lời mời đã thay đổi hoặc hoạt động đã bắt đầu.");
        }
        // Dựng tiêu đề, nội dung thông báo theo loại lời mời và trạng thái phản hồi vừa hủy rồi gửi cho người tạo lời mời
        String invitationLabel = isTermination(invitation) ? "yêu cầu ngừng hợp tác" : "lời mời hợp tác";
        boolean termination = isTermination(invitation);
        notificationService.notifyPartnerInvitationOwner(
                invitationId,
                approved ? "PARTNER_INVITATION_APPROVAL_REVOKED" : "PARTNER_INVITATION_REJECTION_REVOKED",
                termination
                        ? (approved ? "Đối tác đã hủy chấp nhận yêu cầu ngừng hợp tác"
                                : "Đối tác đã hủy từ chối yêu cầu ngừng hợp tác")
                        : (approved ? "Đối tác đã hủy chấp nhận lời mời" : "Đối tác đã hủy từ chối lời mời"),
                "Công ty \"" + invitation.getPartnerCompanyName() + "\" đã hủy phản hồi "
                        + (approved ? "chấp nhận " : "từ chối ") + invitationLabel
                        + " cho hoạt động \"" + invitation.getActivityName()
                        + "\". Lời mời đã trở về trạng thái chờ phản hồi.");
    }

    /**
     * Lấy tập ID các công ty đối tác hiện đang hợp tác ở trạng thái ACTIVE trong một hoạt động.
     *
     * @param activityId ID hoạt động ngoại khóa
     * @return tập ID công ty đối tác đang ACTIVE
     */
    private Set<Integer> activeCompanyIds(int activityId) {
        // Duyệt danh sách đối tác của hoạt động bằng stream, lọc các bản ghi đang ACTIVE và trích xuất ID công ty
        return partnerCompanyLinkDAO.findByActivityId(activityId).stream()
                .filter(ActivityPartnerCompanyDTO::isActive)
                .map(ActivityPartnerCompanyDTO::getPartnerCompanyId)
                .collect(Collectors.toSet());
    }

    /**
     * Kiểm tra lời mời tồn tại và đang ở trạng thái PENDING sau khi đã đồng bộ hủy lời mời của hoạt động không còn mở.
     *
     * @param invitationId ID lời mời cần kiểm tra
     * @return DTO lời mời hợp lệ đang ở trạng thái PENDING
     */
    private ActivityCollaborationInvitationDTO requirePending(int invitationId) {
        invitationDAO.cancelPendingForInactiveActivities();
        // Tra cứu thông tin lời mời nếu ID hợp lệ
        ActivityCollaborationInvitationDTO invitation = invitationId <= 0 ? null : invitationDAO.findById(invitationId);
        // Báo lỗi nếu lời mời không tồn tại
        if (invitation == null) {
            throw new IllegalArgumentException("Không tìm thấy lời mời.");
        }
        // Báo lỗi nếu lời mời không còn ở trạng thái chờ phản hồi PENDING
        if (!IActivityCollaborationInvitationDAO.PENDING.equalsIgnoreCase(invitation.getApprovalStatus())) {
            throw new IllegalArgumentException("Lời mời đã được xử lý trước đó.");
        }
        return invitation;
    }

    /**
     * Kiểm tra xem lời mời có phải là yêu cầu ngừng hợp tác (TERMINATION) hay không.
     *
     * @param invitation DTO lời mời cần kiểm tra
     * @return true nếu là yêu cầu ngừng hợp tác, ngược lại false
     */
    private boolean isTermination(ActivityCollaborationInvitationDTO invitation) {
        return IActivityCollaborationInvitationDAO.TYPE_TERMINATION.equalsIgnoreCase(invitation.getInvitationType());
    }

    /**
     * Kiểm tra người dùng hiện tại có thuộc loại tài khoản nhân sự đối tác (PARTNER_STAFF) hay không.
     *
     * @param user người dùng hiện tại
     * @return true nếu là tài khoản PARTNER_STAFF, ngược lại false
     */
    private boolean isPartnerStaffUser(CurrentUser user) {
        return user != null && "PARTNER_STAFF".equalsIgnoreCase(user.getType());
    }

    /**
     * Kiểm tra người dùng có phải là người được quyền phản hồi lời mời hay không
     * (nhân sự phụ trách chính được mời, hoặc lãnh đạo/thư ký của công ty đối tác đó).
     *
     * @param invitation DTO lời mời cần kiểm tra
     * @param user       tài khoản người dùng hiện tại
     * @param me         hồ sơ nhân sự đối tác tương ứng với tài khoản
     * @return true nếu người dùng có tư cách phản hồi lời mời
     */
    private boolean isResponder(ActivityCollaborationInvitationDTO invitation, CurrentUser user, PartnerStaff me) {
        // Không có hồ sơ nhân sự đối tác thì không thể phản hồi
        if (me == null) {
            return false;
        }
        // Cho phép nếu nhân sự hiện tại chính là nhân sự phụ trách chính ghi trên lời mời
        if (invitation.getPartnerStaffId() != null && invitation.getPartnerStaffId() == me.getId()) {
            return true;
        }
        // Hoặc là người có quyền phân công nhân sự của đúng công ty đối tác nhận lời mời
        return AccessPolicy.canAssignPartnerStaffToActivity(user)
                && me.getCompanyId() == invitation.getPartnerCompanyId();
    }

    /**
     * Xác định người dùng hiện tại có thể phản hồi (chấp nhận/từ chối) lời mời hay không.
     *
     * @param invitation DTO lời mời
     * @param user       tài khoản người dùng hiện tại
     * @param me         hồ sơ nhân sự đối tác tương ứng
     * @return true nếu lời mời đang PENDING và người dùng là người có quyền phản hồi
     */
    private boolean canRespond(ActivityCollaborationInvitationDTO invitation, CurrentUser user, PartnerStaff me) {
        return IActivityCollaborationInvitationDAO.PENDING.equalsIgnoreCase(invitation.getApprovalStatus())
                && isResponder(invitation, user, me);
    }

    /**
     * Kiểm tra người phản hồi có thể thu hồi quyết định trước đó hay không:
     * - Cho phép hủy chấp nhận lời mời hợp tác khi trạng thái hợp tác vẫn đang ACTIVE.
     * - Cho phép hủy từ chối lời mời (riêng yêu cầu ngừng hợp tác bị từ chối chỉ được hủy khi công ty vẫn đang hợp tác ACTIVE).
     * - Không cho phép hủy yêu cầu ngừng hợp tác đã được chấp nhận.
     *
     * @param invitation       DTO lời mời cần kiểm tra
     * @param user             tài khoản người dùng hiện tại
     * @param me               hồ sơ nhân sự đối tác tương ứng
     * @param activeCompanyIds tập ID công ty đang hợp tác ACTIVE trong hoạt động
     * @return true nếu được phép hủy phản hồi lời mời
     */
    private boolean canRevoke(ActivityCollaborationInvitationDTO invitation, CurrentUser user, PartnerStaff me,
                              Set<Integer> activeCompanyIds) {
        boolean termination = isTermination(invitation);
        // Trường hợp thu hồi chấp nhận: chỉ áp dụng cho lời mời hợp tác (không phải ngừng hợp tác) đang ở trạng thái hợp tác ACTIVE
        boolean approved = !termination
                && IActivityCollaborationInvitationDAO.APPROVED.equalsIgnoreCase(invitation.getApprovalStatus())
                && "ACTIVE".equalsIgnoreCase(invitation.getCollaborationStatus());
        // Trường hợp thu hồi từ chối: áp dụng cho lời mời đã REJECTED (nếu là ngừng hợp tác thì công ty phải vẫn đang ACTIVE)
        boolean rejected = IActivityCollaborationInvitationDAO.REJECTED.equalsIgnoreCase(invitation.getApprovalStatus())
                && (!termination || activeCompanyIds.contains(invitation.getPartnerCompanyId()));
        return (approved || rejected) && isResponder(invitation, user, me);
    }

    /**
     * Kiểm tra và chuẩn hóa lời nhắn gửi kèm lời mời (không được trống và không vượt quá giới hạn ký tự).
     *
     * @param message nội dung lời nhắn đầu vào
     * @return nội dung lời nhắn đã cắt khoảng trắng hai đầu
     */
    private String requireMessage(String message) {
        String note = message == null ? "" : message.trim();
        // Bắt buộc người gửi phải nhập nội dung lời nhắn
        if (note.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lời nhắn gửi đối tác.");
        }
        // Kiểm tra giới hạn độ dài tối đa của lời nhắn
        if (note.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Lời nhắn không được vượt quá " + MAX_MESSAGE_LENGTH + " ký tự.");
        }
        return note;
    }

    /**
     * Phân tích chuỗi ID thành số nguyên dương, ném IllegalArgumentException với thông báo tùy chỉnh nếu không hợp lệ.
     *
     * @param value        chuỗi ID cần chuyển đổi
     * @param errorMessage thông báo lỗi khi giá trị không hợp lệ
     * @return ID nguyên dương sau khi phân tích
     */
    private int parseId(String value, String errorMessage) {
        try {
            int id = Integer.parseInt(value == null ? "" : value.trim());
            // Từ chối các giá trị nhỏ hơn hoặc bằng 0
            if (id <= 0) {
                throw new IllegalArgumentException(errorMessage);
            }
            return id;
        } catch (NumberFormatException e) {
            // Chuyển đổi lỗi định dạng số thành lỗi nghiệp vụ thân thiện với người dùng
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    /**
     * Kiểm tra và trả về hoạt động ngoại khóa hợp lệ cho các thao tác lời mời:
     * chỉ cho phép khi hoạt động tồn tại, đang ở trạng thái UPCOMING và chưa đến thời điểm bắt đầu.
     *
     * @param activityId ID hoạt động cần kiểm tra
     * @return thực thể hoạt động ngoại khóa hợp lệ
     */
    private ExtracurricularActivity requireUpcomingActivity(int activityId) {
        // Kiểm tra tính hợp lệ cơ bản của ID hoạt động
        if (activityId <= 0) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }

        ExtracurricularActivity activity = activityDAO.findById(activityId);

        // Báo lỗi nếu hoạt động không tồn tại trong CSDL
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }

        // Chỉ cho phép thao tác lời mời khi hoạt động đang ở trạng thái UPCOMING
        if (activity.getActivityStatus() == null
                || !"UPCOMING".equalsIgnoreCase(
                activity.getActivityStatus().toString())) {
            throw new IllegalArgumentException(
                    "Chỉ được thao tác lời mời khi hoạt động đang ở trạng thái UPCOMING."
            );
        }

        LocalDateTime startTime = activity.getStartTime();
        LocalDateTime now = DateTimeUtil.now();

        // Không cho phép thao tác lời mời nếu chưa có thời gian bắt đầu hoặc hiện tại đã chạm/vượt quá thời gian bắt đầu
        if (startTime == null || !now.isBefore(startTime)) {
            throw new IllegalArgumentException(
                    "Không thể thao tác lời mời sau khi hoạt động bắt đầu."
            );
        }
        return activity;
    }
}
