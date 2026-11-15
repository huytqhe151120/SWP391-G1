# Quy tắc nghiệp vụ: Tuyển Ban tổ chức và tính điểm

Tài liệu này ghi lại quy tắc cho luồng tuyển Ban tổ chức (BTC) và cách tính điểm tham gia của thành viên BTC. BTC vẫn được quản lý trong danh sách điểm danh của hoạt động và được chấm điểm như người tham gia, nhưng có hệ số vai trò riêng.

## 1. Quản lý đợt tuyển

- Staff/Admin được tạo, sửa, mở, đóng và xóa đợt tuyển cho hoạt động.
- Chỉ tạo đợt cho hoạt động đã được duyệt và chưa bắt đầu. Mỗi hoạt động có tối đa một đợt tuyển BTC; đợt tuyển BTC là tùy chọn.
- Đợt có thời gian mở và đóng hồ sơ, vị trí tuyển, chỉ tiêu từng vị trí, câu hỏi bổ sung và tùy chọn bật/tắt phỏng vấn. Thời gian mở và đóng là bắt buộc, thời gian đóng phải sau thời gian mở.
- Đợt tuyển có các trạng thái `DRAFT` (Bản nháp), `OPEN` (Đang nhận hồ sơ) và `CLOSED` (Đã đóng), tự chuyển theo lịch: trước giờ mở là `DRAFT`, trong thời gian nhận hồ sơ là `OPEN`, sau giờ đóng hoặc khi hoạt động bắt đầu/không còn hợp lệ là `CLOSED`. Không có thao tác mở/đóng thủ công; scheduler đồng bộ trạng thái mỗi phút và các luồng tuyển cũng đồng bộ khi được truy cập.
- Tối đa 10 vị trí và 10 câu hỏi. Câu hỏi hỗ trợ tự luận, dropdown và nhiều lựa chọn. Sinh viên chọn tối đa hai nguyện vọng theo thứ tự ưu tiên.
- Đợt BTC nên đóng trước khi đợt đăng ký tham gia mở; đây là lời nhắc, không phải điều kiện bắt buộc.
- Khi đã có hồ sơ, cấu trúc vị trí và câu hỏi bị khóa. Chỉ xóa đợt khi không còn hồ sơ nào ngoài hồ sơ đã rút.

## 2. Nộp và rút hồ sơ

- Sinh viên chỉ nộp khi đợt đang mở, hoạt động chưa bắt đầu và còn ít nhất một vị trí có chỉ tiêu.
- Thông tin hồ sơ sinh viên lấy từ tài khoản/hồ sơ sinh viên, không cho sửa tại form ứng tuyển. Câu hỏi bắt buộc phải được trả lời; có thể gửi ghi chú.
- Mỗi sinh viên chỉ có một hồ sơ cho mỗi đợt. Gửi trùng bị từ chối ở tầng ứng dụng và được bảo vệ bằng ràng buộc duy nhất trong cơ sở dữ liệu. Có thể nộp lại sau khi hồ sơ đã rút nếu đợt vẫn mở.
- Nếu sinh viên đã đăng ký tham gia cùng hoạt động, giao diện cảnh báo rằng trúng tuyển BTC sẽ hủy đăng ký tham gia đó.
- Hồ sơ `PENDING` có thể được sinh viên tự rút. Khi đã được mời phỏng vấn hoặc đã phỏng vấn, sinh viên cần liên hệ staff để xin rút thay vì tự rút.
- Với hồ sơ đã trúng tuyển, sinh viên có thể gửi yêu cầu rút kèm lý do trước mốc 48 giờ so với thời điểm bắt đầu hoạt động. Yêu cầu này báo staff và trả lại chỉ tiêu; sau mốc đó sinh viên cần liên hệ staff.

## 3. Xét tuyển và phỏng vấn

Các trạng thái hồ sơ:

| Trạng thái | Ý nghĩa |
|---|---|
| `PENDING` | Chờ xét |
| `INTERVIEW_INVITED` | Đã mời phỏng vấn |
| `INTERVIEWED` | Đã phỏng vấn |
| `ACCEPTED` | Trúng tuyển |
| `REJECTED` | Không đạt |
| `WITHDRAWN` | Đã rút |

- Staff có thể lọc hồ sơ theo trạng thái/vị trí, ghi chú nội bộ, mời phỏng vấn từng hồ sơ hoặc hàng loạt, loại hồ sơ hoặc quyết định trúng tuyển.
- Nếu đợt có phỏng vấn, chỉ hồ sơ đã phỏng vấn mới được trúng tuyển. Nếu không có phỏng vấn, staff có thể trúng tuyển trực tiếp từ `PENDING`.
- Khi quyết định trúng tuyển, staff chọn vị trí được nhận; vị trí này có thể khác cả hai nguyện vọng. Hệ thống kiểm tra chỉ tiêu tại thời điểm quyết định và khóa vị trí trong giao dịch để hai staff thao tác đồng thời cũng không thể nhận vượt chỉ tiêu.
- Staff đặt thời gian, hình thức trực tiếp/trực tuyến, địa điểm hoặc liên kết, hạn xác nhận và ghi chú phỏng vấn. Hạn xác nhận phải trước giờ phỏng vấn. Có thể đổi hoặc hủy lịch; sinh viên được thông báo khi có thay đổi.
- Hệ thống gửi nhắc xác nhận trước hạn 24 giờ. Quá hạn chưa xác nhận chỉ là nhãn cảnh báo trên hồ sơ, không tự động chuyển hồ sơ thành `REJECTED`; staff chủ động nhắc, đổi lịch hoặc loại.
- Sau giờ phỏng vấn, staff ghi nhận đã phỏng vấn và nhận xét. Sinh viên nhận thông báo khi có kết quả.
- Có thể thu hồi quyết định trước khi hoạt động bắt đầu; khi thu hồi trúng tuyển, chỉ tiêu được trả lại. Việc gỡ thành viên BTC khỏi danh sách điểm danh chỉ thực hiện được nếu chưa có lịch sử điểm danh.

## 4. Quy tắc vai trò trong một hoạt động

- Một sinh viên chỉ giữ một vai trò trong cùng hoạt động: `PARTICIPANT` (người tham gia) hoặc `ORGANIZER` (BTC).
- Khi trúng tuyển BTC, hệ thống hủy đơn đăng ký tham gia cùng hoạt động nếu đơn đang ở trạng thái đã nộp/được duyệt, gỡ dòng tham gia chưa điểm danh nếu có, rồi thêm sinh viên vào danh sách điểm danh với vai trò `ORGANIZER`.
- Người đã trúng tuyển BTC không thể đăng ký tham gia thêm cùng hoạt động. Người không đạt hoặc rút hồ sơ BTC vẫn có thể đăng ký tham gia nếu đợt đăng ký tham gia còn mở và họ đáp ứng điều kiện.
- Khi vai trò hoặc nguồn đăng ký thay đổi, điểm được đồng bộ lại theo vai trò và nguồn đăng ký hiện tại; không giữ hệ số của vai trò trước.

## 5. Quy tắc tính điểm BTC

### 5.1. Điểm chuyên cần

Điểm từng buổi:

| Trạng thái điểm danh | Điểm |
|---|---:|
| Có mặt (`ATTENDED`) | +5 |
| Đi muộn (`IN_LATE`) | +3 |
| Về sớm (`OUT_EARLY`) | +3 |
| Vắng (`ABSENT`) | -5 |

Điểm chuyên cần của hoạt động là trung bình cộng điểm các buổi đã cấu hình. Điểm đánh giá tham gia được cộng vào điểm chuyên cần để tạo `participation_score` đối với người đã tham gia ít nhất một buổi.

Nếu hoạt động yêu cầu báo cáo/tổng kết và có điểm báo cáo:

`average_score = (participation_score + summary_score) / 2`

Nếu không yêu cầu hoặc chưa có điểm báo cáo:

`average_score = participation_score`

### 5.2. Hệ số và điểm cuối

Đối với người có mặt ít nhất một buổi:

`final_point = ROUND(average_score × org_level_coefficient × early_reg_coefficient × role_coefficient, 2)`

- `org_level_coefficient`: hệ số cấp tổ chức của hoạt động; mặc định `1.0` nếu hoạt động không có hệ số.
- `early_reg_coefficient`: `1.1` cho người tham gia tự đăng ký (`registration_status = REGISTERED`); `1.0` cho các nguồn khác.
- `role_coefficient`: `1.5` nếu vai trò hiện tại là BTC (`participation_role = ORGANIZER`); `1.0` nếu là người tham gia.

**Hệ số theo vai trò:**

| Vai trò hiện tại | Hệ số đăng ký sớm | Hệ số vai trò | Tổng hệ số riêng của vai trò/đăng ký |
|---|---:|---:|---:|
| Người tham gia đăng ký thường/được ghi danh | 1.0 | 1.0 | 1.0 |
| Người tham gia tự đăng ký (`REGISTERED`) | 1.1 | 1.0 | 1.1 |
| Ban tổ chức (`STAFF_ENROLLED`, `ORGANIZER`) | 1.0 | 1.5 | 1.5 |

BTC chỉ nhận hệ số vai trò `1.5`, **không cộng thêm hệ số đăng ký sớm `1.1`**. Khi sinh viên được nhận vào BTC, đăng ký tham gia bị hủy và nguồn tham gia của BTC là `STAFF_ENROLLED`; vì vậy hệ số đăng ký sớm trở về `1.0`. Người tham gia nhận `1.1` chỉ khi trạng thái nguồn đăng ký hiện tại là `REGISTERED`.

Nếu trạng thái tổng hợp là vắng (`ABSENT`), điểm cuối bằng điểm chuyên cần (`attendance_score`) và không nhân hệ số cấp tổ chức, đăng ký sớm hay vai trò. Trạng thái chưa điểm danh chưa tạo điểm cuối.

**Ví dụ** với `average_score = 8`, hệ số cấp hoạt động `1.2`:

- Người tham gia đăng ký sớm: `8 × 1.2 × 1.1 × 1.0 = 10.56`.
- BTC: `8 × 1.2 × 1.0 × 1.5 = 14.40`.

## 6. Các thành phần triển khai chính

- `RecruitmentRoundDAOImpl` và `RecruitmentServiceImpl`: điều kiện, cấu hình và vòng đời đợt tuyển.
- `RecruitmentApplicationDAOImpl` và `RecruitmentServiceImpl`: hồ sơ, phỏng vấn, chỉ tiêu, quyết định và đồng bộ vai trò BTC.
- `ActivityParticipantDAOImpl`: điểm danh, hệ số và đồng bộ điểm hoạt động.
- `RecruitmentSchema`: các ràng buộc và cập nhật schema liên quan tuyển BTC.
