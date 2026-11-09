(() => {
    const filters = document.querySelector("[data-checklist-filters]");
    if (!filters) return;

    const rows = Array.from(document.querySelectorAll("[data-checklist-row]"));
    const emptyRow = document.querySelector("[data-filter-empty]");
    const countLabel = document.querySelector("[data-filter-count]");
    const taskCheckboxes = Array.from(document.querySelectorAll("[data-bulk-task-checkbox]"));
    const selectVisibleCheckbox = document.querySelector("[data-select-visible-tasks]");
    const selectedCount = document.querySelector("[data-bulk-selected-count]");
    const submitBulkStatus = document.querySelector("[data-bulk-status-submit]");
    const normalize = (value) => (value || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[đĐ]/g, "d")
        .toLocaleLowerCase("vi")
        .trim();

    const values = () => Object.fromEntries(
        Array.from(filters.querySelectorAll("[data-filter]"))
            .map((input) => [input.dataset.filter, input.value])
    );

    const updateBulkSelection = () => {
        const visibleCheckboxes = taskCheckboxes.filter((checkbox) => !checkbox.closest("tr").hidden);
        const selectedVisibleCount = visibleCheckboxes.filter((checkbox) => checkbox.checked).length;
        const selectedTotal = taskCheckboxes.filter((checkbox) => checkbox.checked).length;

        if (selectVisibleCheckbox) {
            selectVisibleCheckbox.checked = visibleCheckboxes.length > 0
                && selectedVisibleCount === visibleCheckboxes.length;
            selectVisibleCheckbox.indeterminate = selectedVisibleCount > 0
                && selectedVisibleCount < visibleCheckboxes.length;
        }
        if (selectedCount) {
            selectedCount.textContent = selectedTotal
                ? `Đã chọn ${selectedTotal} công việc`
                : "Chưa chọn công việc";
        }
        if (submitBulkStatus) submitBulkStatus.disabled = selectedTotal === 0;
    };

    const applyFilters = () => {
        const selected = values();
        const globalQuery = normalize(selected.search);
        const staffQuery = normalize(selected.staff);
        const partnerQuery = normalize(selected.partner);
        let visibleCount = 0;

        rows.forEach((row) => {
            const data = row.dataset;
            const text = normalize([
                data.code, data.name, data.description, data.assignees
            ].join(" "));
            const deadline = data.deadline ? data.deadline.slice(0, 10) : "";
            const matches = (!globalQuery || text.includes(globalQuery))
                && (!staffQuery || normalize(data.staff).includes(staffQuery))
                && (!partnerQuery || normalize(data.partner).includes(partnerQuery))
                && (!selected.phase || data.phase === selected.phase)
                && (!selected.status || data.status === selected.status)
                && (!selected["deadline-from"] || (deadline && deadline >= selected["deadline-from"]))
                && (!selected["deadline-to"] || (deadline && deadline <= selected["deadline-to"]));

            row.hidden = !matches;
            if (matches) visibleCount += 1;
        });

        if (emptyRow) emptyRow.hidden = visibleCount > 0;
        if (countLabel) {
            countLabel.textContent = `${visibleCount} / ${rows.length} công việc`;
        }
        updateBulkSelection();
    };

    taskCheckboxes.forEach((checkbox) => checkbox.addEventListener("change", updateBulkSelection));
    selectVisibleCheckbox?.addEventListener("change", () => {
        taskCheckboxes.forEach((checkbox) => {
            if (!checkbox.closest("tr").hidden) checkbox.checked = selectVisibleCheckbox.checked;
        });
        updateBulkSelection();
    });
    filters.addEventListener("input", applyFilters);
    filters.addEventListener("change", applyFilters);
    filters.querySelector("[data-clear-filters]")?.addEventListener("click", () => {
        filters.querySelectorAll("[data-filter]").forEach((input) => {
            input.value = "";
        });
        applyFilters();
    });
})();
