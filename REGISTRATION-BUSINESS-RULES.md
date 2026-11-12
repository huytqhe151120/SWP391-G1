# Registration Business Rules

Tài liệu mô tả các quy tắc nghiệp vụ đăng ký hoạt động và những khác biệt đã
phát hiện khi đối chiếu mã nguồn. Các mục 1–10 là quy tắc được yêu cầu; mục 11
ghi rõ những điểm code hiện tại chưa đáp ứng hoặc có hành vi khác. Chưa kiểm
chứng các quy tắc bằng thao tác trên hệ thống và cơ sở dữ liệu đang chạy.

## 1. Tạo và quản lý form

- Mỗi hoạt động có tối đa một form đăng ký.
- Chỉ hoạt động đã được duyệt và ở trạng thái `UPCOMING` mới được tạo hoặc mở form.
- Form có ba trạng thái: `DRAFT` (bản nháp), `OPENING` (đang mở đăng ký) và `CLOSED` (đã đóng).
- Thời gian mở và đóng là bắt buộc; thời gian đóng phải sau thời gian mở. Hạn đóng phải cách thời điểm bắt đầu hoạt động ít nhất 24 giờ, kể cả đúng mốc 24 giờ.
- Form `DRAFT` được lên lịch mở trong tương lai tự chuyển sang `OPENING` khi đến thời điểm mở. Form `OPENING` tự đóng khi hết hạn đăng ký hoặc khi hoạt động bắt đầu, bị hủy hay không còn đủ điều kiện. Bộ lập lịch đồng bộ khi ứng dụng khởi động và mỗi phút.
- Không thể mở form trước thời điểm mở hoặc sau thời điểm đóng. Form đã đóng có thể được mở lại nếu hoạt động vẫn đủ điều kiện và hạn đăng ký chưa qua.
- Không thể chỉnh sửa form đã có đơn đăng ký. Khi sửa form chưa có đơn, được giữ nguyên thời điểm mở cũ đã qua để chỉnh sửa thông tin khác; không thể đặt thời điểm mở mới trong quá khứ hoặc lưu bản nháp với thời điểm mở đã qua.
- Được tạo form `DRAFT` trước thời điểm mở nếu hoạt động đủ điều kiện; chỉ được mở nhận đăng ký khi đã đến thời điểm mở và chưa đến thời điểm đóng.

## 2. Gửi đơn và sức chứa

- Sinh viên chỉ gửi được đơn khi form thực sự đang `OPENING`, thời gian đăng ký còn hiệu lực, hoạt động đã được duyệt và đang `UPCOMING`, và hoạt động còn chỗ.
- Sinh viên phải thuộc đối tượng tham gia của hoạt động theo cấu hình; nếu không cấu hình đối tượng, điều kiện đối tượng không hạn chế sinh viên.
- Mã sinh viên, họ tên, lớp, email, số điện thoại và ngày sinh lấy từ hồ sơ; các thông tin này bắt buộc và không thể sửa trong form đăng ký. Sinh viên cần cập nhật hồ sơ nếu thiếu số điện thoại hoặc ngày sinh.
- Khi đăng ký bằng form, sinh viên phải xác nhận cam kết tham dự trước khi gửi đơn.
- `max_participants` là giới hạn tùy chọn: giá trị rỗng nghĩa là không giới hạn; nếu có giá trị thì phải lớn hơn 0.
- Các đơn `SUBMITTED` (chờ xử lý) và `APPROVED` (đã duyệt) đều giữ một chỗ. Đơn `REJECTED` (đã từ chối) không giữ chỗ.
- Đơn chỉ được tính là gửi thành công sau khi giao dịch lưu đơn và các câu trả lời hoàn tất. `submitted_at` được ghi khi tạo đơn trong giao dịch, không nhất thiết trùng thời điểm commit; nếu giao dịch thất bại, đơn và câu trả lời không được lưu.
- Khi số đơn đang giữ chỗ đạt `max_participants`, hệ thống ngừng nhận đơn mới và không hiển thị form đó trong danh sách đăng ký dành cho sinh viên. Khi đơn bị từ chối, chỗ được giải phóng và sinh viên khác có thể đăng ký nếu form vẫn mở.
- Kiểm tra sức chứa được thực hiện ở backend/database, không chỉ ở giao diện. Các thao tác đăng ký đồng thời được tuần tự hóa trên hoạt động để đơn được ghi nhận trước giữ chỗ trước và không vượt quá giới hạn.
- Không tự động từ chối đơn chờ xử lý khi hoạt động đủ chỗ hoặc khi form đóng. Đóng form chỉ ngăn đơn mới; các đơn hiện có vẫn được giữ nguyên và phải được xét xử lý.
- Không thể giảm sức chứa xuống thấp hơn tổng số đơn `SUBMITTED` và `APPROVED` đang giữ chỗ. Nếu cần giảm thấp hơn, phải xử lý các đơn vượt mức trước; việc giảm giới hạn thông thường không tự ý từ chối hoặc xóa đơn.

## 3. Xét duyệt đơn

- Các trạng thái xét duyệt gồm `SUBMITTED`, `APPROVED` và `REJECTED`. Nhân viên có thể duyệt hoặc từ chối đơn đang `SUBMITTED`; duyệt giữ nguyên chỗ đã có, từ chối giải phóng chỗ.
- Các đơn chờ được sắp xếp theo `submitted_at` tăng dần, sau đó theo ID tăng dần. Backend chỉ cho phép duyệt hoặc từ chối đơn chờ sớm nhất của form; nếu có đơn chờ cũ hơn thì thao tác trên đơn mới hơn bị từ chối, bất kể thứ tự hiển thị.
- Khi staff từ chối đơn, phải nhập lý do tối đa 1.000 ký tự. Lý do được lưu theo đơn và hiển thị trong chi tiết đơn cho sinh viên.
- Staff có thể chọn tối đa 100 đơn đầu hàng đợi để duyệt cùng lúc hoặc từ chối bằng một lý do chung. Thao tác theo lô là nguyên tử: nếu một đơn không còn hợp lệ thì không đơn nào bị cập nhật.
- Hủy duyệt chuyển `APPROVED` về `SUBMITTED` và không giải phóng chỗ. Hủy từ chối chuyển `REJECTED` về `SUBMITTED` và chỉ được phép nếu còn chỗ.
- Khi hủy duyệt hoặc hủy từ chối, đơn trở lại hàng chờ với `pending_since` được đặt lại tại thời điểm khôi phục. Quy tắc ưu tiên theo `submitted_at` ở trên chưa quyết định rõ cách xử lý đơn được khôi phục; xem mục 11.
- Luồng ngoại lệ cho phép ưu tiên hoặc xử lý ngoài thứ tự gửi hiện chưa được triển khai. Nếu nhà trường cần ngoại lệ, cần có quy định riêng cùng thao tác ghi nhận người thực hiện, lý do và thời điểm; không được bỏ qua thứ tự một cách âm thầm.

## 4. Hủy đăng ký

- Sinh viên có thể hủy đăng ký của mình nếu hoạt động ở trạng thái `UPCOMING` và thời điểm bắt đầu còn hơn 24 giờ. Đúng mốc 24 giờ hoặc muộn hơn không được rút/hủy.
- Khi hủy, trạng thái đơn chuyển thành:
  - `WITHDRAWN` (Đã rút đơn): đơn đang `SUBMITTED` hoặc hoạt động là đăng ký nhanh (không cần form). Sinh viên có thể đăng ký lại hoạt động này.
  - `CANCELLED` (Đã hủy): đơn đã `APPROVED` và hoạt động yêu cầu form. Sinh viên không thể đăng ký lại hoạt động này.
- Đơn `REJECTED` không thể hủy.
- Đơn `WITHDRAWN` hoặc `CANCELLED` không thể hủy lần nữa.
- Yêu cầu đã nêu trước đó: đơn `APPROVED` cần tạo đề xuất hủy và chỉ được hủy khi staff duyệt. Yêu cầu này **mâu thuẫn** với quy tắc hủy trực tiếp ở trên và hiện chưa được triển khai; cần thống nhất trước khi áp dụng.

## 5. Đăng ký nhanh (Quick Registration)

- Đối với hoạt động không yêu cầu form đăng ký (`registration_form_required = 0`), hệ thống quản lý một form đăng ký nhanh mặc định. Code hiện tạo form khi hoạt động được duyệt và cũng có thể tạo nếu chưa có khi sinh viên đăng ký.
- Sinh viên không cần điền form, chỉ có thể thêm ghi chú tùy chọn tối đa 2.000 ký tự.
- Đơn đăng ký nhanh được tạo với trạng thái `APPROVED` ngay lập tức, không cần chờ duyệt.
- Nếu đơn đăng ký nhanh trước đó là `WITHDRAWN`, hệ thống kích hoạt lại đơn cũ thay vì tạo đơn mới.
- Nếu đơn đăng ký nhanh trước đó là `CANCELLED`, sinh viên không thể đăng ký lại hoạt động này.

## 6. Đăng ký lại (Re-registration)

- Sinh viên có thể đăng ký lại nếu đơn trước đó ở trạng thái `WITHDRAWN`, form vẫn đang `OPENING` và hoạt động còn chỗ.
- Sinh viên không thể đăng ký lại nếu đơn trước đó ở trạng thái `CANCELLED`, `APPROVED` hoặc `SUBMITTED`.
- Các điều kiện đăng ký lại áp dụng cho cả đăng ký thường và đăng ký nhanh.

## 7. Đồng bộ trạng thái form

- Bộ lập lịch tự động đồng bộ trạng thái form mỗi phút và khi ứng dụng khởi động.
- `DRAFT` → `OPENING`: khi đến thời điểm mở đăng ký.
- `OPENING` → `CLOSED`: khi hết thời điểm đóng đăng ký hoặc khi hoạt động bắt đầu, bị hủy hay không còn đủ điều kiện.
- `CLOSED` có thể được mở lại nếu hoạt động vẫn đủ điều kiện và hạn đăng ký chưa qua.

## 8. Xóa form

- Chỉ có thể xóa form khi không còn đơn đăng ký nào, bao gồm cả đơn `SUBMITTED`, `APPROVED`, `REJECTED`, `WITHDRAWN` hoặc `CANCELLED`.
- Form đăng ký nhanh do hệ thống tự động quản lý không thể xóa thủ công.
- Với form đã có đơn, không tự ý xóa lịch sử. Đề xuất chuyển sang `DRAFT` cần được xác nhận và triển khai riêng nếu hệ thống không hỗ trợ thao tác đó.

## 9. Điều kiện không thể sửa hoạt động

- Không thể sửa hoạt động khi hoạt động ở trạng thái `ONGOING` (đang diễn ra) hoặc `COMPLETED` (đã hoàn thành).
- Không thể thay đổi tính chất hoạt động (`nature`) hoặc yêu cầu form đăng ký (`registration_form_required`) khi đã có sinh viên đăng ký.
- Không thể giảm `max_participants` xuống thấp hơn tổng số đơn `SUBMITTED` và `APPROVED` đang giữ chỗ.

## 10. Trạng thái hoạt động và form

- Chỉ hoạt động đã được duyệt (`APPROVED`) và ở trạng thái `UPCOMING` mới được tạo hoặc mở form đăng ký.
- Khi tạo form, hoạt động phải chưa bắt đầu và chưa bị hủy; có thể đặt thời gian mở trong tương lai. Khi mở form để nhận đăng ký, còn phải nằm trong thời gian đăng ký.
- Khi hoạt động chuyển sang `ONGOING` hoặc bị hủy, form liên quan tự động đóng.

## 11. Khác biệt so với code hiện tại

Các điểm sau là kết quả đọc mã, không phải xác nhận đã kiểm thử thực tế:

1. **Thứ tự duyệt:** Code sắp xếp đơn chờ theo `pending_since` rồi ID. Duyệt/từ chối một đơn hoặc một nhóm ngoài thứ tự vẫn được thực hiện và chỉ có cảnh báo; backend chưa bắt buộc chọn các đơn đầu hàng đợi. Vì `pending_since` được đặt lại khi khôi phục, đơn khôi phục được đưa về cuối hàng chờ, khác với quy tắc tuyệt đối theo `submitted_at` ở mục 3.
2. **Hủy đơn đã duyệt:** Chưa có đề xuất hủy hoặc bước staff duyệt. Sinh viên hủy trực tiếp: đơn thường đã duyệt thành `CANCELLED`; đăng ký nhanh thành `WITHDRAWN` và có thể đăng ký lại. Cần thống nhất việc cấm đăng ký lại có áp dụng cả đăng ký nhanh và đơn chờ đã rút hay không trước khi thay đổi nghiệp vụ.
3. **Thiếu giờ bắt đầu:** Điều kiện trong code vẫn cho rút/hủy khi `start_time` là `NULL`, trong khi quy tắc ở mục 4 yêu cầu xác định được khoảng cách hơn 24 giờ.
4. **Sửa form đã có đơn:** Form thường đã có đơn có luồng gia hạn hạn đóng sau khi hết hạn nếu đáp ứng điều kiện. Form đăng ký nhanh có luồng sửa riêng, kể cả khi đã có đơn. Quy tắc cấm chỉnh sửa tuyệt đối ở mục 1 chưa phản ánh các ngoại lệ này.
5. **Xóa form có đơn đã rút:** Code cho phép xóa form nếu chỉ còn đơn `WITHDRAWN`, đồng thời xóa các đơn đó và câu trả lời liên quan. Việc này khác quy tắc không xóa bất kỳ form nào còn đơn ở mục 8.
6. **Thay đổi tính chất/yêu cầu form:** Code kiểm tra số đơn có trạng thái khác `WITHDRAWN`; nếu chỉ còn đơn đã rút thì điều kiện này không chặn thay đổi. Code cũng kiểm tra số đơn trước khi cập nhật hoạt động, nên chưa khẳng định được quy tắc được bảo đảm trong mọi trường hợp cập nhật và đăng ký đồng thời.
7. **Mở lại và đưa form về nháp:** Không nên hiểu điều kiện đủ về mặt nghiệp vụ là mọi form `CLOSED` đều có thao tác mở lại hoặc mọi form đã có đơn đều có thao tác chuyển `DRAFT`. Code hiện có luồng gia hạn riêng cho form thường đã có đơn và xử lý riêng cho form nhanh; khả năng thao tác phụ thuộc từng luồng và điều kiện thực tế.

Đối chiếu code: `RegistrationFormServiceImpl`, `RegistrationFormDAOImpl`,
`RegistrationFormServlet` và `ExtracurricularActivityServiceImpl` trong
`src/main/java/com/swp391/g1`.
