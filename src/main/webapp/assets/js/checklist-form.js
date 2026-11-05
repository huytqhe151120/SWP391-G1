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
    const prerequisiteSelect = document.querySelector("#prerequisiteTaskIds");
    const noPrerequisitesMessage = document.querySelector("[data-no-prerequisite-options]");
    const prerequisiteHelp = document.querySelector("[data-prerequisite-help]");
    if (activitySelect && prerequisiteSelect && noPrerequisitesMessage && prerequisiteHelp) {
        const currentTaskId = document.querySelector('input[name="id"]')?.value || "";
        const updatePrerequisiteOptions = () => {
            let availableCount = 0;
            Array.from(prerequisiteSelect.options).forEach((option) => {
                const allowed = option.dataset.taskActivity === activitySelect.value
                    && option.dataset.taskId !== currentTaskId;
                option.hidden = !allowed;
                option.disabled = !allowed;
                if (!allowed) option.selected = false;
                if (allowed) availableCount += 1;
            });
            noPrerequisitesMessage.hidden = availableCount > 0;
            prerequisiteHelp.hidden = availableCount === 0;
        };

        activitySelect.addEventListener("change", updatePrerequisiteOptions);
        updatePrerequisiteOptions();
    }
})();
