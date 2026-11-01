# Quy tắc nghiệp vụ form đăng ký hoạt động

Tài liệu này mô tả quy tắc tạo và quản lý form đăng ký hoạt động, tiếp nhận đơn sinh viên, giới hạn sức chứa và xét duyệt đơn.

## Cài đặt cơ sở dữ liệu

Trước khi sử dụng form đăng ký, custom form fields, thông tin hồ sơ được lưu và trạng thái đơn bị từ chối, áp dụng các script sau vào SQL Server:

- `src/main/resources/db/registration-form-fields.sql`
- `src/main/resources/db/registration-form-status.sql`
- `src/main/resources/db/registration-form-activity-limit.sql`
- `src/main/resources/db/registration-application-status.sql`

## Tạo và quản lý form

- Mỗi hoạt động chỉ có tối đa một form đăng ký.
- Chỉ hoạt động đã được duyệt và còn ở trạng thái `UPCOMING` mới được tạo form hoặc mở form đăng ký.
- Form có ba trạng thái: `DRAFT` (bản nháp), `OPENING` (đang mở đăng ký) và `CLOSED` (đã đóng).
- Form `DRAFT` được lên lịch với thời điểm mở trong tương lai sẽ tự chuyển sang `OPENING` khi đến hạn. Form đang `OPENING` tự đóng khi hết hạn đăng ký hoặc khi hoạt động bắt đầu, bị hủy hay không còn đủ điều kiện. Bộ lập lịch đồng bộ khi ứng dụng khởi động và mỗi phút.
- Không thể mở form trước thời điểm mở hoặc sau thời điểm đóng. Form đã đóng có thể được mở lại nếu hoạt động vẫn đủ điều kiện và hạn đăng ký chưa qua.
- Khi sửa form, không thể chỉnh sửa form đã có đơn đăng ký. Việc giữ nguyên thời điểm mở cũ đã qua được chấp nhận để chỉnh sửa các thông tin khác; không thể đặt thời điểm mở mới trong quá khứ hoặc lưu bản nháp với thời điểm mở đã qua.

## Gửi đơn và sức chứa

- Sinh viên chỉ gửi được đơn khi form thực sự đang `OPENING`, thời gian đăng ký còn hiệu lực, hoạt động đã được duyệt và đang `UPCOMING`, và hoạt động còn chỗ.
- `max_participants` là giới hạn tùy chọn: giá trị rỗng nghĩa là không giới hạn; nếu có giá trị thì phải lớn hơn 0.
- Các đơn `SUBMITTED` (chờ xử lý) và `APPROVED` (đã duyệt) đều giữ một chỗ. Đơn `REJECTED` (đã từ chối) không giữ chỗ.
- Đơn chỉ được tính là gửi thành công sau khi giao dịch lưu đơn và các câu trả lời hoàn tất. Thời điểm `submitted_at` được lưu trong cơ sở dữ liệu lúc gửi thành công; nếu nhiều đơn có cùng thời điểm, ID đơn dùng để xác định thứ tự.
- Khi số đơn đang giữ chỗ đạt `max_participants`, hệ thống ngừng nhận đơn mới và không hiển thị form đó trong danh sách đăng ký dành cho sinh viên. Khi đơn bị từ chối, chỗ được giải phóng và form vẫn mở thì sinh viên khác có thể đăng ký.
- Kiểm tra sức chứa được thực hiện ở backend/database, không chỉ ở giao diện. Các thao tác đăng ký đồng thời được tuần tự hóa trên hoạt động để đơn được ghi nhận trước giữ chỗ trước và không vượt quá giới hạn.
- Không tự động từ chối đơn chờ xử lý khi hoạt động đủ chỗ hoặc khi form đóng. Đóng form chỉ ngăn đơn mới; các đơn hiện có vẫn được giữ nguyên và phải được xét xử lý.
- Không thể giảm sức chứa xuống thấp hơn tổng số đơn `SUBMITTED` và `APPROVED` đang giữ chỗ. Nếu cần giảm thấp hơn, phải xử lý các đơn vượt mức trước; việc giảm giới hạn thông thường không tự ý từ chối hoặc xóa đơn.

## Xét duyệt đơn

- Trạng thái đơn gồm `SUBMITTED`, `APPROVED` và `REJECTED`. Nhân viên có thể duyệt hoặc từ chối đơn đang `SUBMITTED`; duyệt giữ nguyên chỗ đã có, từ chối giải phóng chỗ.
- Các đơn chờ được sắp xếp theo `submitted_at` tăng dần, sau đó theo ID tăng dần. Backend chỉ cho phép duyệt hoặc từ chối đơn chờ sớm nhất của form; có đơn chờ cũ hơn thì thao tác trên đơn mới hơn bị từ chối, bất kể thứ tự hiển thị ở giao diện.
- Hủy duyệt chuyển `APPROVED` về `SUBMITTED`, không giải phóng chỗ. Hủy từ chối chuyển `REJECTED` về `SUBMITTED` và chỉ được phép nếu còn chỗ.
- Luồng ngoại lệ cho phép ưu tiên hoặc xử lý ngoài thứ tự gửi hiện chưa được triển khai. Nếu nhà trường cần ngoại lệ, cần có quy định riêng cùng thao tác ghi nhận người thực hiện, lý do và thời điểm; không được bỏ qua thứ tự một cách âm thầm.

## Hủy đăng ký

- Sinh viên có thể hủy đăng ký của mình nếu hoạt động còn ở trạng thái `UPCOMING` và chưa đến 24 giờ trước thời điểm bắt đầu hoạt động.
- Khi hủy đăng ký, trạng thái đơn được chuyển thành:
  - `WITHDRAWN` (Đã rút đơn): Nếu đơn đang ở trạng thái `SUBMITTED` hoặc hoạt động là đăng ký nhanh (không cần form). Sinh viên có thể đăng ký lại hoạt động này.
  - `CANCELLED` (Đã hủy): Nếu đơn đã được duyệt (`APPROVED`) và hoạt động yêu cầu form. Sinh viên **không thể** đăng ký lại hoạt động này.
- Đơn đã bị từ chối (`REJECTED`) không thể hủy.
- Đơn đã hủy (`WITHDRAWN` hoặc `CANCELLED`) không thể hủy lần nữa.

## Đăng ký nhanh (Quick Registration)

- Đối với hoạt động không yêu cầu form đăng ký (`registration_form_required = 0`), hệ thống tự động tạo một form đăng ký nhanh mặc định khi sinh viên đăng ký lần đầu.
- Đăng ký nhanh không yêu cầu sinh viên điền form, chỉ cần ghi chú tùy chọn (tối đa 2000 ký tự).
- Khi đăng ký nhanh, đơn được tạo với trạng thái `APPROVED` ngay lập tức (không cần chờ duyệt).
- Nếu sinh viên đã có đơn đăng ký nhanh bị hủy (`WITHDRAWN`), hệ thống sẽ kích hoạt lại đơn cũ thay vì tạo đơn mới.
- Nếu sinh viên đã có đơn đăng ký nhanh bị hủy (`CANCELLED`), không thể đăng ký lại hoạt động này.

## Re-registration (Đăng ký lại)

- Sinh viên có thể đăng ký lại hoạt động nếu:
  - Đơn trước đó ở trạng thái `WITHDRAWN` (đã rút đơn).
  - Form vẫn đang `OPENING` và hoạt động còn chỗ.
- Sinh viên **không thể** đăng ký lại hoạt động nếu:
  - Đơn trước đó ở trạng thái `CANCELLED` (đã hủy sau khi được duyệt).
  - Đơn trước đó ở trạng thái `APPROVED` hoặc `SUBMITTED` (chưa hủy).
- Logic kiểm tra re-registration được áp dụng cho cả đăng ký thường và đăng ký nhanh.

## Đồng bộ trạng thái form

- Hệ thống có bộ lập lịch tự động đồng bộ trạng thái form mỗi phút và khi ứng dụng khởi động.
- Form tự động chuyển trạng thái:
  - `DRAFT` → `OPENING`: Khi đến thời điểm mở đăng ký.
  - `OPENING` → `CLOSED`: Khi hết thời điểm đóng đăng ký hoặc khi hoạt động bắt đầu, bị hủy, hoặc không còn đủ điều kiện.
- Form có thể được mở lại từ `CLOSED` nếu hoạt động vẫn đủ điều kiện và hạn đăng ký chưa qua.

## Xóa form

- Chỉ có thể xóa form khi không còn đơn đăng ký nào (trạng thái `SUBMITTED`, `APPROVED`, `REJECTED`, `WITHDRAWN`, hoặc `CANCELLED`).
- Form đăng ký nhanh do hệ thống tự động quản lý không thể xóa thủ công.
- Nếu cần xóa form đã có đơn, nên chuyển form sang trạng thái `DRAFT` thay vì xóa.

## Điều kiện không thể sửa hoạt động

- Không thể sửa hoạt động khi hoạt động đang ở trạng thái `ONGOING` (đang diễn ra) hoặc `COMPLETED` (đã hoàn thành).
- Không thể thay đổi tính chất hoạt động (`nature`) hoặc yêu cầu form đăng ký (`registration_form_required`) khi đã có sinh viên đăng ký.
- Không thể giảm số lượng tối đa (`max_participants`) xuống thấp hơn tổng số đơn `SUBMITTED` và `APPROVED` đang giữ chỗ.

## Trạng thái hoạt động và form

- Chỉ hoạt động đã được duyệt (`APPROVED`) và còn ở trạng thái `UPCOMING` mới được tạo form hoặc mở form đăng ký.
- Form chỉ có thể được tạo hoặc mở khi hoạt động còn đủ điều kiện (chưa bắt đầu, chưa bị hủy, còn trong thời gian đăng ký).
- Khi hoạt động chuyển sang trạng thái `ONGOING` hoặc bị hủy, form liên quan sẽ tự động đóng.
