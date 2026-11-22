(() => {
    const typeSelect = document.getElementById("filter-type");
    const roleSelect = document.getElementById("filter-role");

    if (!typeSelect || !roleSelect) {
        return;
    }

    // Lưu danh sách đầy đủ để dựng lại khi thay đổi loại tài khoản.
    const allRoles = Array.from(roleSelect.options)
        .filter(option => option.value !== "")
        .map(option => ({
            value: option.value,
            label: option.textContent.trim(),
            type: option.dataset.type
        }));

    function updateRoles() {
        const selectedType = typeSelect.value;
        const previousRole = roleSelect.value;

        const availableRoles = allRoles.filter(role =>
            selectedType === "" || role.type === selectedType
        );

        roleSelect.replaceChildren(new Option("Tất cả", ""));

        // Tránh trùng vai trò khi chọn tất cả loại tài khoản.
        const addedRoles = new Set();

        availableRoles.forEach(role => {
            if (addedRoles.has(role.value)) {
                return;
            }

            addedRoles.add(role.value);
            roleSelect.add(new Option(role.label, role.value));
        });

        // Giữ vai trò cũ nếu vẫn phù hợp, ngược lại về "Tất cả".
        roleSelect.value = addedRoles.has(previousRole)
            ? previousRole
            : "";
    }

    typeSelect.addEventListener("change", updateRoles);

    // Áp dụng cả khi trang vừa tải hoặc sau khi tìm kiếm.
    updateRoles();
})();
