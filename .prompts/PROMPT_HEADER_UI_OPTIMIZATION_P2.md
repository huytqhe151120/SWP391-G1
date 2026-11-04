# Yêu cầu tối ưu hóa UI/UX cho thanh Header (Hệ thống EAMS)

## 1. MỤC TIÊU
Tối ưu hóa thanh Header trên cùng nhằm tăng tính nhận diện thương hiệu, nâng cao trải nghiệm điều hướng (Breadcrumb), hỗ trợ tra cứu nhanh toàn hệ thống và loại bỏ các tiện ích không cần thiết để giữ giao diện tối giản, tập trung vào công việc.

---

## 2. NHẬN XÉT GIAO DIỆN HIỆN TẠI & ĐỀ XUẤT ĐIỀU CHỈNH

### 2.1. Khối Thương hiệu & Logo (Brand Section)
- **Hiện trạng:** Khối Brand/Logo (`EAMS / G1 UNIVERSITY`) có chiều rộng chưa dóng thẳng với lề của Sidebar bên dưới, đường nét còn rối.
- **Đề xuất:**
  - Cố định độ rộng của vùng Brand đúng bằng độ rộng Sidebar (`240px` - `250px`).
  - Sử dụng màu nền đồng nhất (`#1E293B` hoặc giữ `#0F172A`) để tạo cảm giác liên khối liền mạch với Sidebar bên dưới.
  - Tối ưu typography: Tên hệ thống **EAMS** bôi đậm rõ nét, dòng phụ *G1 UNIVERSITY / Quản lý hoạt động ngoại khóa* thu nhỏ với font-size `11px` màu xám nhạt (`#94A3B8`).

### 2.2. Tối ưu Breadcrumb & Navigation Context
- **Hiện trạng:** Đường dẫn Breadcrumb hiện tại (`Trang chủ / Tổng quan`) nằm ở vùng trống bên cạnh ô tìm kiếm nhưng khoảng cách chưa thực sự cân đối.
- **Đề xuất:**
  - Căn trái Breadcrumb sát sau khối Brand Panel.
  - Định dạng chuẩn: `Trang chủ` / **Tổng quan**
    - Trang cấp cao: font-size `13px`, màu xám trung tính (`#64748B`), hover có gạch chân.
    - Trang hiện tại: font-size `13px`, font-weight `600`, màu chữ chính (`#0F172A`).

### 2.3. Tối ưu Ô tìm kiếm toàn hệ thống (Global Search Bar)
- **Hiện trạng:** Ô tìm kiếm có cụm phím tắt `Ctrl + K` bị lặp lại 2 lần (cả trong placeholder và nút bên phải), gây thừa thãi.
- **Đề xuất:**
  - Thu gọn Search Box về giữa Header hoặc căn phải hợp lý.
  - Loại bỏ nút phím tắt bị lặp ở viền phải, chỉ giữ icon kính lúp bên trái + placeholder: `Tìm kiếm hoạt động, sinh viên, tài khoản...`.
  - Phím tắt `Ctrl + K` thiết kế thành 1 badge nhỏ gọn (`<kbd>Ctrl + K</kbd>`) chìm phía bên phải trong khung input.

### 2.4. Đơn giản hóa Action Items (Đã lược bỏ Ngôn ngữ, Hỏi đáp & Dark mode)
- **Thay đổi chính:** Lược bỏ hoàn toàn nút chọn ngôn ngữ (VIE/ENG), icon Hỏi đáp/Help (`?`) và icon chuyển chế độ tối (Dark mode).
- **Cấu trúc góc phải chỉ giữ lại:**
  1. **Icon Thông báo (Notification Bell):**
     - Icon hình chiếc chuông chuẩn SVG (Lucide/Heroicons).
     - Badge đỏ nhỏ hiển thị số thông báo chưa đọc (VD: `3`).
     - Tích hợp hiệu ứng hover nhẹ (`background: #F1F5F9`) và Popover danh sách thông báo khi nhấp vào.
  2. **Vạch ngăn cách đứng (Separator):** Thêm 1 vạch `border-right: 1px solid #E2E8F0` ngăn cách giữa Notification và Avatar người dùng (nếu có ở Header).

---

## 3. CHECKLIST CÔNG VIỆC DÀNH CHO DEVELOPER / AI

- [ ] **Khối Brand / Logo:**
  - Set `width: 240px` (hoặc `250px` tùy theo width Sidebar).
  - Tối ưu font size & màu sắc chữ EAMS.
- [ ] **Breadcrumb:**
  - Căn chỉnh flex layout để Breadcrumb nối tiếp ngay sau khối Logo.
  - Cập nhật màu sắc & font-weight cho item Active.
- [ ] **Global Search:**
  - Xóa bỏ nút `Ctrl + K` bị trùng thừa phía bên phải.
  - Style lại badge `<kbd>` bên trong input.
- [ ] **Góc phải Header (Actions):**
  - Remove nút chọn ngôn ngữ `[VIE]`.
  - Remove icon Hỏi đáp `(?)`.
  - Remove icon Toggle Theme / Dark mode.
  - Giữ lại Icon Notification + Badge đếm số thông báo.

---

## 4. BỘ MÀU QUY CHUẨN HEADER (COLOR PALETTE)
- **Header Background:** `#FFFFFF`
- **Brand Panel Background:** `#0F172A` (hoặc `#1E293B`)
- **Breadcrumb Text Muted:** `#64748B`
- **Breadcrumb Text Active:** `#0F172A`
- **Search Input Background:** `#F8FAFC` (Border: `#E2E8F0`, Focus: `#1890FF`)
- **Notification Badge:** `#EF4444` (Red)