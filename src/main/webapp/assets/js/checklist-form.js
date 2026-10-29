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
})();
