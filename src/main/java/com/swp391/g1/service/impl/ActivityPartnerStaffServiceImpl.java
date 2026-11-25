package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityCollaborationInvitationDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.IActivityPartnerCompanyDAO;
import com.swp391.g1.dao.IActivityPartnerStaffDAO;
import com.swp391.g1.dao.IPartnerStaffDAO;
import com.swp391.g1.dao.impl.ActivityCollaborationInvitationDAOImpl;
import com.swp391.g1.dao.impl.ActivityPartnerCompanyDAOImpl;
import com.swp391.g1.dao.impl.ActivityPartnerStaffDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.PartnerStaffDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityPartnerCompanyDTO;
import com.swp391.g1.dto.response.ActivityPartnerStaffResponseDTO;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.PartnerStaff;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityPartnerStaffService;
import com.swp391.g1.util.DateTimeUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lớp triển khai dịch vụ quản lý phân công nhân sự của công ty đối tác tham gia hoạt động ngoại khóa.
 * Đảm bảo phạm vi hiển thị theo công ty đối tác, phân quyền theo vai trò đối tác/người phụ trách chính
 * và chỉ cho phép thay đổi phân công trước khi hoạt động bắt đầu.
 */
public class ActivityPartnerStaffServiceImpl implements IActivityPartnerStaffService {

    private final IActivityPartnerStaffDAO activityPartnerStaffDAO;
    private final IActivityCollaborationInvitationDAO invitationDAO;
    private final IExtracurricularActivityDAO activityDAO;
    private final IActivityPartnerCompanyDAO partnerCompanyDAO;
    private final IPartnerStaffDAO partnerStaffDAO;

    /**
     * Khởi tạo dịch vụ cùng các DAO liên quan tới nhân sự đối tác, công ty đối tác, lời mời và hoạt động.
     */
    public ActivityPartnerStaffServiceImpl() {
        this.activityPartnerStaffDAO = new ActivityPartnerStaffDAOImpl();
        this.invitationDAO = new ActivityCollaborationInvitationDAOImpl();
        this.activityDAO = new ExtracurricularActivityDAOImpl();
        this.partnerCompanyDAO = new ActivityPartnerCompanyDAOImpl();
        this.partnerStaffDAO = new PartnerStaffDAOImpl();
    }

    /**
     * Lấy danh sách nhân sự đối tác đã được phân công vào hoạt động, có áp dụng giới hạn hiển thị theo người dùng.
     * Nếu người dùng là nhân sự đối tác ({@code PARTNER_STAFF}), chỉ trả về nhân sự thuộc công ty của chính họ.
     *
     * @param activityId ID hoạt động
     * @param user       Người dùng hiện tại
     * @return Danh sách DTO nhân sự đối tác được phép xem
     */
    @Override
    public List<ActivityPartnerStaffResponseDTO> getPartnerStaffsByActivityId(int activityId, CurrentUser user) {
        // Trả về danh sách rỗng nếu ID hoạt động không dương
        if (activityId <= 0) {
            return List.of();
        }
        List<ActivityPartnerStaffResponseDTO> all = activityPartnerStaffDAO.findByActivityId(activityId);
        // Nếu không phải tài khoản PARTNER_STAFF (ví dụ nhân sự nhà trường/admin) thì được xem toàn bộ danh sách
        if (user == null || !"PARTNER_STAFF".equalsIgnoreCase(user.getType())) {
            return all;
        }
        // Nhân sự đối tác chỉ được xem danh sách nhân sự đã phân công thuộc công ty của chính mình
        PartnerStaff me = partnerStaffDAO.findByAccountId(user.getId());
        // Không tìm thấy hồ sơ nhân sự đối tác tương ứng với tài khoản -> trả về rỗng
        if (me == null) {
            return List.of();
        }
        // Lọc danh sách để giữ lại các nhân sự có cùng companyId với người dùng hiện tại
        return all.stream().filter(s -> s.getCompanyId() == me.getCompanyId()).collect(Collectors.toList());
    }

    /**
     * Lấy tập ID các công ty đối tác của hoạt động mà người dùng hiện tại được phép chỉnh sửa phân công nhân sự
     * (yêu cầu hoạt động chưa bắt đầu và người dùng có thẩm quyền với công ty đó).
     *
     * @param activityId ID hoạt động
     * @param user       Người dùng hiện tại
     * @return Tập ID công ty đối tác được phép thao tác phân công (rỗng nếu hoạt động đã bắt đầu hoặc không có quyền)
     */
    private Set<Integer> manageableCompanyIds(int activityId, CurrentUser user) {
        // Không cho phép quản lý phân công nếu hoạt động đã bắt đầu hoặc không còn UPCOMING
        if (!isActivityBeforeStart(activityId)) {
            return Set.of();
        }
        return authorizedCompanyIds(activityId, user);
    }

    /**
     * Xác định tập ID các công ty đối tác đang hợp tác hoạt động ({@code isActive}) mà người dùng có quyền quản lý:
     * phải là tài khoản {@code PARTNER_STAFF} thuộc đúng công ty đó, đồng thời là lãnh đạo/thư ký theo {@link AccessPolicy}
     * hoặc là nhân sự đối tác phụ trách chính của lời mời hợp tác.
     *
     * @param activityId ID hoạt động
     * @param user       Người dùng hiện tại
     * @return Tập ID công ty đối tác mà người dùng có thẩm quyền
     */
    private Set<Integer> authorizedCompanyIds(int activityId, CurrentUser user) {
        // Chỉ áp dụng quyền quản lý nhân sự đối tác cho tài khoản loại PARTNER_STAFF với activityId hợp lệ
        if (activityId <= 0 || user == null || !"PARTNER_STAFF".equalsIgnoreCase(user.getType())) {
            return Set.of();
        }
        PartnerStaff me = partnerStaffDAO.findByAccountId(user.getId());
        // Trả về tập rỗng nếu tài khoản không gắn với hồ sơ PartnerStaff
        if (me == null) {
            return Set.of();
        }
        boolean leader = AccessPolicy.canAssignPartnerStaffToActivity(user);
        // Duyệt danh sách công ty đối tác của hoạt động: giữ các hợp tác đang active, thuộc cùng công ty,
        // và người dùng có quyền leader hoặc chính là partnerStaff phụ trách chính
        return partnerCompanyDAO.findByActivityId(activityId).stream()
                .filter(ActivityPartnerCompanyDTO::isActive)
                .filter(i -> i.getPartnerCompanyId() == me.getCompanyId())
                .filter(i -> leader || i.getPartnerStaffId() == me.getId())
                .map(i -> i.getPartnerCompanyId())
                .collect(Collectors.toSet());
    }

    /**
     * Thu thập tập ID các nhân sự đối tác phụ trách chính của các công ty đối tác đang hợp tác trong hoạt động.
     *
     * @param activityId ID hoạt động
     * @return Tập ID nhân sự đối tác phụ trách chính
     */
    private Set<Integer> mainStaffIds(int activityId) {
        // Duyệt các công ty đối tác của hoạt động, chỉ lấy các hợp tác đang active và trích xuất partnerStaffId phụ trách chính
        return partnerCompanyDAO.findByActivityId(activityId).stream()
                .filter(ActivityPartnerCompanyDTO::isActive)
                .map(i -> i.getPartnerStaffId())
                .collect(Collectors.toSet());
    }

    /**
     * Lấy danh sách nhân sự đối tác khả dụng có thể phân công thêm vào hoạt động bởi người dùng hiện tại.
     * Chỉ gồm nhân sự đang {@code ACTIVE} thuộc các công ty mà người dùng có quyền quản lý,
     * loại trừ nhân sự đã phân công và nhân sự phụ trách chính.
     *
     * @param activityId ID hoạt động
     * @param user       Người dùng hiện tại
     * @return Danh sách {@link PartnerStaff} khả dụng
     */
    @Override
    public List<PartnerStaff> getAvailablePartnerStaffsForActivity(int activityId, CurrentUser user) {
        Set<Integer> companyIds = manageableCompanyIds(activityId, user);
        // Trả về rỗng nếu người dùng không quản lý công ty đối tác nào (hoặc hoạt động đã bắt đầu)
        if (companyIds.isEmpty()) {
            return List.of();
        }
        // Gom tập ID nhân sự đối tác đã được phân công vào hoạt động
        Set<Integer> excluded = new HashSet<>(activityPartnerStaffDAO.findByActivityId(activityId).stream()
                .map(ActivityPartnerStaffResponseDTO::getPartnerStaffId)
                .collect(Collectors.toSet()));
        // Loại trừ thêm cả các nhân sự đối tác phụ trách chính
        excluded.addAll(mainStaffIds(activityId));

        // Trải phẳng danh sách nhân sự của từng công ty đối tác được quản lý, chỉ giữ người ACTIVE và chưa nằm trong tập excluded
        return companyIds.stream()
                .flatMap(companyId -> partnerStaffDAO.findByCompany(companyId).stream())
                .filter(s -> s.getStatus() == Enum.CommonStatus.ACTIVE)
                .filter(s -> !excluded.contains(s.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra người dùng hiện tại có vai trò quản lý nhân sự đối tác của hoạt động hay không (không xét thời gian).
     *
     * @param activityId ID hoạt động
     * @param user       Người dùng hiện tại
     * @return {@code true} nếu người dùng quản lý ít nhất một công ty đối tác trong hoạt động
     */
    @Override
    public boolean canManagePartnerStaff(int activityId, CurrentUser user) {
        return !authorizedCompanyIds(activityId, user).isEmpty();
    }

    /**
     * Kiểm tra người dùng hiện tại có thể thêm/gỡ nhân sự đối tác ngay lúc này hay không (đủ quyền và trước giờ bắt đầu).
     *
     * @param activityId ID hoạt động
     * @param user       Người dùng hiện tại
     * @return {@code true} nếu được phép phân công nhân sự đối tác
     */
    @Override
    public boolean canAssignPartnerStaff(int activityId, CurrentUser user) {
        return !manageableCompanyIds(activityId, user).isEmpty();
    }

    /**
     * Phân công một nhân sự đối tác vào hoạt động sau khi kiểm tra thời gian, phân quyền,
     * trạng thái hoạt động của nhân sự, không phải phụ trách chính, đúng công ty và chưa trùng phân công.
     *
     * @param activityId     ID hoạt động
     * @param partnerStaffId ID nhân sự đối tác cần phân công
     * @param user           Người dùng thực hiện thao tác
     * @return {@code true} nếu thêm phân công thành công
     */
    @Override
    public boolean assignPartnerStaff(int activityId, int partnerStaffId, CurrentUser user) {
        // Kiểm tra ID hoạt động và ID nhân sự đối tác phải hợp lệ
        if (activityId <= 0 || partnerStaffId <= 0) {
            throw new IllegalArgumentException("Thông tin phân công không hợp lệ.");
        }
        // Ràng buộc chỉ được thay đổi phân công trước khi hoạt động bắt đầu
        requireActivityBeforeStart(activityId);
        Set<Integer> companyIds = manageableCompanyIds(activityId, user);
        // Chặn thao tác nếu người dùng không có quyền phân công cho công ty đối tác nào của hoạt động
        if (companyIds.isEmpty()) {
            throw new SecurityException("Bạn không có quyền phân công nhân sự đối tác vào hoạt động này.");
        }
        PartnerStaff staff = partnerStaffDAO.findById(partnerStaffId);
        // Đảm bảo nhân sự đối tác tồn tại
        if (staff == null) {
            throw new IllegalArgumentException("Không tìm thấy nhân sự đối tác được chọn.");
        }
        // Chỉ cho phép phân công nhân sự đối tác đang hoạt động (ACTIVE)
        if (staff.getStatus() != Enum.CommonStatus.ACTIVE) {
            throw new IllegalArgumentException("Nhân sự đối tác được chọn không còn hoạt động.");
        }
        // Không cho phép thêm nhân sự đối tác phụ trách chính vào danh sách nhân sự hỗ trợ
        if (mainStaffIds(activityId).contains(partnerStaffId)) {
            throw new IllegalArgumentException(
                    "Nhân sự đối tác phụ trách chính không được thêm vào danh sách nhân sự đối tác của hoạt động.");
        }
        // Đảm bảo nhân sự được chọn thuộc công ty đối tác nằm trong phạm vi quản lý của người dùng
        if (!companyIds.contains(staff.getCompanyId())) {
            throw new IllegalArgumentException("Nhân sự đối tác phải thuộc công ty đối tác của bạn trong hoạt động.");
        }
        // Tránh phân công trùng lặp nhân sự đối tác đã có trong hoạt động
        if (activityPartnerStaffDAO.exists(activityId, partnerStaffId)) {
            throw new IllegalArgumentException("Nhân sự đối tác này đã được phân công vào hoạt động.");
        }
        return activityPartnerStaffDAO.insert(activityId, partnerStaffId);
    }

    /**
     * Gỡ phân công một nhân sự đối tác khỏi hoạt động trước khi hoạt động bắt đầu.
     *
     * @param activityId     ID hoạt động
     * @param partnerStaffId ID nhân sự đối tác cần gỡ
     * @param user           Người dùng thực hiện thao tác
     * @return {@code true} nếu gỡ thành công, {@code false} nếu tham số không hợp lệ hoặc không thuộc quyền quản lý
     */
    @Override
    public boolean removePartnerStaff(int activityId, int partnerStaffId, CurrentUser user) {
        // Từ chối khi ID không hợp lệ
        if (activityId <= 0 || partnerStaffId <= 0) {
            return false;
        }
        // Ràng buộc chỉ được gỡ trước khi hoạt động bắt đầu
        requireActivityBeforeStart(activityId);
        PartnerStaff staff = partnerStaffDAO.findById(partnerStaffId);
        // Kiểm tra nhân sự tồn tại và thuộc công ty đối tác mà người dùng có quyền quản lý
        if (staff == null || !manageableCompanyIds(activityId, user).contains(staff.getCompanyId())) {
            return false;
        }
        return activityPartnerStaffDAO.delete(activityId, partnerStaffId);
    }

    /**
     * Kiểm tra hoạt động đang ở trạng thái {@link ActivityStatus#UPCOMING} và thời điểm hiện tại (giờ Việt Nam)
     * vẫn nằm trước thời điểm bắt đầu của hoạt động.
     *
     * @param activityId ID hoạt động cần kiểm tra
     * @return {@code true} nếu hoạt động chưa bắt đầu
     */
    private boolean isActivityBeforeStart(int activityId) {
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        return activity != null
                && activity.getActivityStatus() == ActivityStatus.UPCOMING
                && activity.getStartTime() != null
                && DateTimeUtil.now().isBefore(activity.getStartTime());
    }

    /**
     * Đảm bảo hoạt động chưa bắt đầu; nếu không sẽ ném {@link IllegalArgumentException}.
     *
     * @param activityId ID hoạt động cần kiểm tra
     */
    private void requireActivityBeforeStart(int activityId) {
        // Chặn thay đổi phân công nhân sự đối tác nếu hoạt động đã bắt đầu hoặc không còn UPCOMING
        if (!isActivityBeforeStart(activityId)) {
            throw new IllegalArgumentException(
                    "Chỉ được thay đổi phân công nhân sự đối tác trước khi hoạt động bắt đầu.");
        }
    }
}
