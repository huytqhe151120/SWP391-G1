# Yêu cầu tối ưu hóa UI/UX cho Sidebar (EAMS System)

## 1. MỤC TIÊU
Cải thiện thiết kế thanh Navigation Sidebar của hệ thống EAMS dựa trên màn hình hiện tại nhằm nâng cao tính thẩm mỹ, độ tương phản thị giác, tính nhất quán về icon và tối ưu không gian hiển thị.

---

## 2. DÃY NGHỆ THUẬT & QUY TẮC THIẾT KẾ (DESIGN RULES)
- **Primary Color:** `#1890FF` (hoặc màu xanh chủ đạo hiện tại của hệ thống)
- **Background Sidebar:** `#FFFFFF` hoặc `#FAFAFA`
- **Text Primary:** `#1F2937`
- **Text Muted / Group Title:** `#8C98A4`
- **Font-family:** Đồng nhất font chữ hiện tại của EAMS (VD: Inter, Roboto, hoặc system-ui).

---

## 3. CÁC HẠNG MỤC CẦN THỰC HIỆN

### 3.1. Cải thiện Typography & Phân cấp thị giác
- [ ] **Group Title (`QUẢN LÝ`, `HỖ TRỢ`):**
  - Giảm `font-size` xuống `11px` - `12px`.
  - Tăng `font-weight` lên `600` (Semi-bold).
  - Đổi màu chữ sang tông xám nhạt (`#8C98A4`).
  - Thêm `letter-spacing: 0.8px` và `text-transform: uppercase`.
  - Thêm `margin-top: 16px` để tạo nhịp nghỉ thị giác rõ ràng giữa các nhóm.

### 3.2. Chuẩn hóa Icon & Trạng thái Hover / Active
- [ ] **Thay thế Icon:**
  - Thay toàn bộ ký tự ASCII/Unicode cũ (`*⌂*`, `*◷*`, `*✉*`, `*✓*`, `*♙*`, `*♧*`, `*▦*`) bằng bộ **Lucide Icons** SVG.
  - Cố định kích thước Icon: `18px x 18px` hoặc `20px x 20px`.
- [ ] **Trạng thái Item:**
  - **Normal:** Màu chữ `#4B5563`, màu icon `#6B7280`.
  - **Hover:** `background-color: #F3F4F6`, đổi màu chữ & icon sang tông đậm hơn nhẹ.
  - **Active (Trạng thái đang chọn - VD: Điểm danh sinh viên):**
    - `background-color: #E6F4FF` (Xanh nhạt).
    - Màu chữ & Icon: `#1890FF` (Primary).
    - Thêm **Indicator Bar**: Vạch xanh đậm 3px ở mép trái (`border-left: 3px solid #1890FF` hoặc `position: absolute`).

### 3.3. Tái cấu trúc Menu & Gom nhóm (Grouping)
- [ ] Nhóm **QUẢN LÝ** hiện có quá nhiều mục (8 mục). Tiến hành gom nhóm dạng Collapsible / Accordion sub-menu nếu cần:
  - **Quản lý Hoạt động:** Hoạt động ngoại khóa, Điểm danh sinh viên, Lời mời hợp tác.
  - **Quản lý Nhân sự:** Sinh viên, Nhân viên, Tài khoản.
  - **Đối tác:** Công ty đối tác, Nhân viên đối tác.
- [ ] Thêm nút **Collapse Sidebar** (Thu gọn về dạng Icon-only) ở góc trên hoặc dưới của Sidebar để giải phóng không gian màn hình chính.

### 3.4. Tối ưu khối Thông tin người dùng (User Profile Box)
- [ ] Cố định khối User Profile nằm ở **đáy Sidebar** (`position: sticky` hoặc Flexbox `margin-top: auto`).
- [ ] Thêm vạch ngăn cách mỏng `border-top: 1px solid #E5E7EB` phía trên User Box.

### 3.5. Bố cục & Scroll Behavior
- [ ] Cố định chiều rộng Sidebar: `240px` - `250px`.
- [ ] Đặt Sidebar chế độ `position: fixed` hoặc `height: 100vh` với `overflow-y: auto` để khi cuộn nội dung bảng bên phải, Sidebar vẫn giữ nguyên vị trí.

---

## 4. KẾT QUẢ MONG MUỐN (DELIVERABLES)
Hãy cập nhật lại file CSS/SCSS và HTML/React Component của Sidebar theo các yêu cầu trên, đảm bảo tính responsive và code sạch sẽ, dễ bảo trì.