# Yêu cầu tối ưu hóa UI/UX cho thanh Header (Hệ thống EAMS)

## 1. MỤC TIÊU
Tối ưu hóa thanh Header trên cùng nhằm tăng tính nhận diện thương hiệu, cải thiện trải nghiệm điều hướng (Breadcrumb), tối ưu các hành động nhanh (Quick Actions) và đảm bảo sự cân bằng thị giác với thanh Sidebar.

---

## 2. ĐỀ XUẤT ĐIỀU CHỈNH GIAO DIỆN & QUY TẮC THIẾT KẾ

### 2.1. Cải thiện Khối Thương hiệu (Brand & Logo Section)
- **Hiện trạng:** Khối Logo (`G1 UNIVERSITY / EAMS`) nằm trên nền xanh navy đậm nhạt không đồng nhất, độ tương phản chữ xám/xanh nhạt chưa tối ưu.
- **Đề xuất:**
  - Đồng nhất màu nền vùng Brand với Sidebar hoặc dùng tông màu thương hiệu cố định (`#1E293B` hoặc `#0F172A`).
  - Tăng kích thước Icon Logo (khoảng `32px - 36px`), chuẩn hóa chữ **EAMS** làm nổi bật hơn tên trường.
  - Cố định chiều rộng của khối Logo bằng đúng chiều rộng của Sidebar (`240px` - `250px`) để tạo đường dóng thẳng đứng hoàn hảo.

### 2.2. Tối ưu Breadcrumb & Tiêu đề trang (Navigation Context)
- **Hiện trạng:** Thanh Breadcrumb (`EAMS / QUẢN LÝ HOẠT ĐỘNG NGOẠI KHÓA`) nằm tách biệt trong một banner xanh phía dưới Header chính, gây tốn diện tích chiều cao màn hình.
- **Đề xuất:**
  - Chuyển đường dẫn Breadcrumb trực tiếp lên thanh Header chính (phần bên phải khối Logo).
  - Định dạng Breadcrumb: `Trang chủ` / `Quản lý Hoạt động` / **Hoạt động ngoại khóa** (dùng font nhỏ `13px`, màu xám `#64748B`, trang hiện tại bôi đậm `#0F172A`).

### 2.3. Bổ sung Ô tìm kiếm toàn hệ thống (Global Search Bar)
- **Đề xuất:** Thêm ô tìm kiếm nhanh ở giữa thanh Header.
  - Thiết kế: Bo góc `8px`, nền xám nhạt (`#F1F5F9`), icon kính lúp bên trái.
  - Placeholder: `Tìm kiếm hoạt động, sinh viên, tài khoản... (Ctrl + K)`.

### 2.4. Tối ưu Tiện ích & Thông báo (Quick Actions)
- **Hiện trạng:** Góc phải chỉ có duy nhất icon cái chuông thông báo.
- **Đề xuất:**
  - **Notification Bell:** Thêm Badge đỏ hiển thị số lượng thông báo chưa đọc (VD: `3`). Khi bấm vào sẽ mở Notification Popover.
  - **Quick Help / FAQ:** Thêm icon `?` (Help Center) bên cạnh chuông thông báo.
  - **Language Selector / Theme Toggle:** Thêm tùy chọn chuyển đổi ngôn ngữ (VIE/ENG) hoặc Dark/Light mode ngắn gọn.

---

## 3. CHECKLIST CÔNG VIỆC DÀNH CHO DEVELOPER / AI

- [ ] **Layout & Flexbox Alignment:**
  - Sử dụng `display: flex; align-items: center; justify-content: space-between;` cho Header.
  - Chiều cao cố định Header: `64px` (sticky / fixed top).
  - Shadow mỏng phía dưới: `box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);`.
- [ ] **Typography & Colors:**
  - Background Header chính: `#FFFFFF` (hoặc Nền tối đồng bộ thương hiệu).
  - Border phân cách: `border-bottom: 1px solid #E2E8F0`.
- [ ] **Interactive Elements:**
  - Thêm hiệu ứng hover cho Notification Icon và Quick Links.
  - Đảm bảo responsive: Thu gọn Search Bar thành Icon trên màn hình tablet/mobile.

---

## 4. BỘ MÀU CHUẨN CHO HEADER (COLOR PALETTE)
- **Header Background:** `#FFFFFF`
- **Brand Panel Background:** `#1E293B` (Hoặc `#1890FF`)
- **Breadcrumb Active:** `#0F172A` (Font Weight 600)
- **Breadcrumb Muted:** `#64748B`
- **Search Background:** `#F8FAFC` (Focus: `#FFFFFF` + Border `#1890FF`)
- **Border Separator:** `#E2E8F0`