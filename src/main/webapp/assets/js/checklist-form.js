(() => {
    const normalize = (value) => value
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[đĐ]/g, "d")
        .toLocaleLowerCase("vi")
        .trim();

    document.querySelectorAll("[data-assignee-search]").forEach((input) => {
        const group = input.dataset.assigneeSearch;
        const list = document.querySelector(`[data-assignee-list="${group}"]`);
        const emptyMessage = document.querySelector(`[data-no-assignee-results="${group}"]`);
        if (!list || !emptyMessage) return;

        const options = Array.from(list.querySelectorAll("[data-assignee-option]"));
        input.addEventListener("input", () => {
            const query = normalize(input.value);
            let visibleCount = 0;
            options.forEach((option) => {
                const matches = normalize(option.textContent || "").includes(query);
                option.hidden = !matches;
                if (matches) visibleCount += 1;
            });
            emptyMessage.hidden = visibleCount > 0;
        });
    });

    const activitySelect = document.querySelector("#activityId");
    const prerequisiteDialog = document.querySelector("[data-prerequisite-dialog]");
    const prerequisiteOptions = document.querySelector("[data-prerequisite-options]");
    const noPrerequisitesMessage = document.querySelector("[data-no-prerequisite-options]");
    const prerequisiteButton = document.querySelector("[data-open-prerequisites]");
    const prerequisiteSummary = document.querySelector("[data-prerequisite-selection]");
    const prerequisiteSearch = document.querySelector("[data-prerequisite-search]");
    const noPrerequisiteResults = document.querySelector("[data-no-prerequisite-results]");
    const taskStatusSelect = document.querySelector("#status");
    const prerequisiteStatusHelp = document.querySelector("[data-prerequisite-status-help]");
    if (activitySelect && prerequisiteDialog && prerequisiteOptions && noPrerequisitesMessage
        && prerequisiteButton && prerequisiteSummary && prerequisiteSearch && noPrerequisiteResults) {
        const currentTaskId = document.querySelector('input[name="id"]')?.value || "";
        const options = Array.from(prerequisiteOptions.querySelectorAll("[data-prerequisite-option]"));
        const checks = options.map((option) => option.querySelector('input[name="prerequisiteTaskIds"]'));

        const updatePrerequisiteSummary = () => {
            const selected = options
                .filter((option, index) => checks[index].checked && !checks[index].disabled)
                .map((option) => option.querySelector("strong").textContent.trim());
            prerequisiteSummary.textContent = selected.length
                ? `Đã chọn ${selected.length}: ${selected.join(", ")}`
                : "Chưa chọn công việc tiên quyết.";
            const incomplete = checks.some((checkbox) => checkbox.checked
                && !checkbox.disabled
                && checkbox.dataset.taskStatus !== "Đã hoàn thành");
            if (taskStatusSelect && prerequisiteStatusHelp) {
                taskStatusSelect.querySelectorAll("[data-advanced-status]").forEach((option) => {
                    option.disabled = incomplete;
                });
                prerequisiteStatusHelp.hidden = !incomplete;
                if (incomplete && taskStatusSelect.value !== "Chưa hoàn thành") {
                    taskStatusSelect.value = "Chưa hoàn thành";
                }
            }
        };

        const updatePrerequisiteSearch = () => {
            const query = normalize(prerequisiteSearch.value);
            let visibleCount = 0;
            options.forEach((option, index) => {
                if (checks[index].disabled) {
                    option.hidden = true;
                    return;
                }
                const matches = normalize(option.textContent || "").includes(query);
                option.hidden = !matches;
                if (matches) visibleCount += 1;
            });
            noPrerequisiteResults.hidden = visibleCount > 0
                || options.every((option, index) => checks[index].disabled);
        };

        const updatePrerequisiteOptions = () => {
            let availableCount = 0;
            options.forEach((option, index) => {
                const checkbox = checks[index];
                const allowed = checkbox.dataset.taskActivity === activitySelect.value
                    && checkbox.dataset.taskId !== currentTaskId;
                checkbox.disabled = !allowed;
                if (!allowed) checkbox.checked = false;
                if (allowed) availableCount += 1;
            });
            noPrerequisitesMessage.hidden = availableCount > 0;
            prerequisiteButton.disabled = availableCount === 0;
            if (availableCount === 0) prerequisiteDialog.close();
            updatePrerequisiteSummary();
            updatePrerequisiteSearch();
        };

        prerequisiteButton.addEventListener("click", () => prerequisiteDialog.showModal());
        prerequisiteDialog.querySelectorAll("[data-close-prerequisites]").forEach((button) => {
            button.addEventListener("click", () => prerequisiteDialog.close());
        });
        prerequisiteDialog.addEventListener("click", (event) => {
            if (event.target === prerequisiteDialog) prerequisiteDialog.close();
        });
        checks.forEach((checkbox) => {
            checkbox.addEventListener("change", updatePrerequisiteSummary);
        });
        prerequisiteSearch.addEventListener("input", updatePrerequisiteSearch);
        activitySelect.addEventListener("change", updatePrerequisiteOptions);
        updatePrerequisiteOptions();
    }
})();
