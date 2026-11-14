(function () {
    "use strict";

    function create(options) {
        var items = Array.from(options.items);
        var container = options.container;
        var currentPage = 1;
        var pageSize = 10;
        var footer = document.createElement("div");
        footer.className = "activity-pagination";
        var sizeLabel = document.createElement("label");
        sizeLabel.className = "activity-page-size";
        sizeLabel.appendChild(document.createTextNode("Hiển thị "));
        var sizeSelect = document.createElement("select");
        sizeSelect.setAttribute("aria-label", "Số dòng mỗi trang");
        [5, 10, 20, 25].forEach(function (size) {
            var option = document.createElement("option");
            option.value = String(size);
            option.textContent = String(size);
            option.selected = size === pageSize;
            sizeSelect.appendChild(option);
        });
        sizeLabel.appendChild(sizeSelect);
        var summary = document.createElement("span");
        summary.className = "activity-pagination-summary";
        summary.setAttribute("aria-live", "polite");
        var nav = document.createElement("nav");
        nav.className = "activity-pagination-nav";
        nav.setAttribute("aria-label", "Phân trang");
        footer.appendChild(sizeLabel);
        footer.appendChild(summary);
        footer.appendChild(nav);
        container.appendChild(footer);

        function render() {
            var matches = options.matches ? items.filter(options.matches) : items;
            var pageCount = Math.max(1, Math.ceil(matches.length / pageSize));
            currentPage = Math.max(1, Math.min(currentPage, pageCount));
            var start = (currentPage - 1) * pageSize;
            var visible = new Set(matches.slice(start, start + pageSize));
            items.forEach(function (item) { item.hidden = !visible.has(item); });
            footer.hidden = items.length === 0;
            summary.textContent = "Hiển thị " + (matches.length ? start + 1 : 0) + "–" +
                Math.min(start + pageSize, matches.length) + " / " + matches.length;
            nav.replaceChildren();
            if (pageCount > 1) {
                function button(label, page, disabled, current) {
                    var element = document.createElement("button");
                    element.type = "button";
                    element.className = "activity-page-link" + (current ? " is-active" : "");
                    element.textContent = label;
                    element.disabled = disabled;
                    if (current) element.setAttribute("aria-current", "page");
                    element.addEventListener("click", function () {
                        currentPage = page;
                        render();
                    });
                    nav.appendChild(element);
                }
                button("‹", currentPage - 1, currentPage === 1, false);
                var first = Math.max(1, Math.min(currentPage - 2, pageCount - 4));
                var last = Math.min(pageCount, first + 4);
                for (var number = first; number <= last; number++) {
                    button(String(number), number, false, number === currentPage);
                }
                button("›", currentPage + 1, currentPage === pageCount, false);
            }
            if (options.onRender) options.onRender(matches.length);
        }

        sizeSelect.addEventListener("change", function () {
            pageSize = Number(sizeSelect.value);
            currentPage = 1;
            render();
        });
        render();
        return {
            refresh: function (resetPage) {
                if (resetPage) currentPage = 1;
                render();
            }
        };
    }

    window.ListPagination = {create: create};
    document.querySelectorAll("[data-paginated-list]").forEach(function (container) {
        create({container: container, items: container.querySelectorAll("[data-page-item]")});
    });
})();
