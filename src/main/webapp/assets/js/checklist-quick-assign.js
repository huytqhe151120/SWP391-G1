(() => {
    const dialog = document.querySelector("[data-quick-assign-dialog]");
    if (!dialog) return;

    document.querySelector("[data-open-quick-assign]")?.addEventListener("click", () => {
        dialog.showModal();
    });

    dialog.querySelectorAll("[data-close-quick-assign]").forEach((button) => {
        button.addEventListener("click", () => dialog.close());
    });

    dialog.querySelectorAll("[data-select-all]").forEach((selectAll) => {
        const group = selectAll.dataset.selectAll;
        const checkboxes = Array.from(dialog.querySelectorAll(
            `[data-quick-assignee="${group}"]`
        ));

        selectAll.addEventListener("change", () => {
            checkboxes.forEach((checkbox) => {
                checkbox.checked = selectAll.checked;
            });
        });

        checkboxes.forEach((checkbox) => {
            checkbox.addEventListener("change", () => {
                selectAll.checked = checkboxes.length > 0
                    && checkboxes.every((item) => item.checked);
                selectAll.indeterminate = checkboxes.some((item) => item.checked)
                    && !selectAll.checked;
            });
        });
    });

    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) dialog.close();
    });
})();
