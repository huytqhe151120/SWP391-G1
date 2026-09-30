<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kênh Hỏi Đáp Q&A - Sinh Viên</title>

    <!-- Nhúng CSS Bootstrap 5 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Nhúng Bootstrap Icons để có các biểu tượng đẹp mắt -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
</head>
<body class="bg-light">

<div class="container py-5">
    <h2 class="mb-4 text-primary fw-bold text-center">
        <i class="bi bi-chat-square-text"></i> Kênh Hỏi Đáp Q&A - Dành cho Sinh Viên
    </h2>

    <!-- Khung hiển thị thông báo Thành công / Thất bại -->
    <c:if test="${not empty message}">
        <div class="alert alert-success alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-check-circle-fill me-2"></i> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i> ${error}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty databaseError}">
        <div class="alert alert-warning shadow-sm" role="alert">
            <i class="bi bi-database-exclamation me-2"></i> ${databaseError}
        </div>
    </c:if>

    <div class="row">

        <!-- CỘT TRÁI: FORM ĐẶT CÂU HỎI MỚI -->
        <div class="col-lg-5 mb-4">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-header bg-primary text-white fw-bold py-3">
                    <i class="bi bi-send"></i> Đặt câu hỏi mới
                </div>
                <div class="card-body p-4">
                    <form action="${pageContext.request.contextPath}/qa" method="POST">

                        <div class="mb-3">
                            <label for="title" class="form-label fw-semibold">Tiêu đề câu hỏi <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="title" name="title" required placeholder="Nhập tiêu đề ngắn gọn...">
                        </div>

                        <div class="mb-3">
                            <label for="content" class="form-label fw-semibold">Nội dung chi tiết <span class="text-danger">*</span></label>
                            <textarea class="form-control" id="content" name="content" rows="6" required placeholder="Mô tả rõ thắc mắc của bạn..."></textarea>
                        </div>

                        <div class="form-check mb-4">
                            <input class="form-check-input" type="checkbox" id="isAnonymous" name="isAnonymous" value="on">
                            <label class="form-check-label text-muted" for="isAnonymous">
                                Gửi dưới dạng <strong>Ẩn danh</strong>
                            </label>
                        </div>

                        <div class="d-grid">
                            <button type="submit" class="btn btn-primary py-2 fw-bold">
                                <i class="bi bi-cursor-fill"></i> Gửi Câu Hỏi
                            </button>
                        </div>

                    </form>
                </div>
            </div>
        </div>

        <!-- CỘT PHẢI: DANH SÁCH CÂU HỎI ĐÃ ĐƯỢC GIẢI ĐÁP -->
        <div class="col-lg-7">
            <div class="card shadow-sm border-0 h-100">
                <div class="card-header bg-success text-white fw-bold py-3">
                    <i class="bi bi-card-list"></i> Các câu hỏi đã được giải đáp
                </div>
                <div class="card-body p-4 bg-white">

                    <c:choose>
                        <c:when test="${not empty databaseError}">
                            <div class="text-center text-muted py-5">
                                <i class="bi bi-database-exclamation fs-1 d-block mb-3"></i>
                                <p>Danh sách câu hỏi sẽ hiển thị sau khi kết nối được cơ sở dữ liệu.</p>
                            </div>
                        </c:when>
                        <c:when test="${empty questions}">
                            <div class="text-center text-muted py-5">
                                <i class="bi bi-inbox fs-1 d-block mb-3"></i>
                                <p>Hiện chưa có câu hỏi nào được giải đáp và xuất bản.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <!-- Danh sách dạng Accordion (Nhấn vào tiêu đề để xem nội dung) -->
                            <div class="accordion" id="qaAccordion">
                                <c:forEach var="q" items="${questions}" varStatus="status">

                                    <div class="accordion-item mb-3 border rounded shadow-sm">
                                        <h2 class="accordion-header" id="heading${status.index}">
                                            <button class="accordion-button collapsed fw-bold text-primary" type="button" data-bs-toggle="collapse" data-bs-target="#collapse${status.index}" aria-expanded="false" aria-controls="collapse${status.index}">
                                                    <c:out value="${q.title}"/>
                                            </button>
                                        </h2>
                                        <div id="collapse${status.index}" class="accordion-collapse collapse" aria-labelledby="heading${status.index}" data-bs-parent="#qaAccordion">
                                            <div class="accordion-body bg-light">
                                                <p class="mb-3 text-dark" style="white-space: pre-wrap;"><c:out value="${q.content}"/></p>
                                                <hr>
                                                <div class="d-flex justify-content-between text-muted small">
                                                        <span>
                                                            <i class="bi bi-person-circle"></i> Đăng bởi:
                                                            <c:choose>
                                                                <c:when test="${q.anonymous}"><span class="fst-italic text-secondary">Sinh viên ẩn danh</span></c:when>
                                                                <c:otherwise><span class="fw-semibold text-dark">Sinh viên ID: <c:out value="${q.displayStudentId}"/></span></c:otherwise>
                                                            </c:choose>
                                                        </span>
                                                    <span><i class="bi bi-clock"></i> <c:out value="${q.createdAt}"/></span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>

                </div>
            </div>
        </div>

    </div>
</div>

<!-- Nhúng JS Bootstrap 5 (Cần thiết để chạy hiệu ứng thông báo và danh sách xổ xuống) -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>