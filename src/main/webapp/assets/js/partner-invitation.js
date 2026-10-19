(function () {
    var modal = document.getElementById("invitation-message-modal");
    document.querySelectorAll(".invitation-message-view").forEach(function (button) {
        button.addEventListener("click", function () {
            document.getElementById("invitation-message-modal-title").textContent = button.dataset.title;
            document.getElementById("invitation-message-modal-body").textContent = button.dataset.message;
            modal.showModal();
        });
    });
    document.getElementById("invitation-message-modal-close").addEventListener("click", function () {
        modal.close();
    });
    modal.addEventListener("click", function (e) {
        if (e.target === modal) {
            modal.close();
        }
    });
    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });
})();