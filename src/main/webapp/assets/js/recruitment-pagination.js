(function () {
    "use strict";

    document.querySelectorAll("[data-page-container]").forEach(function (container) {
        var items = Array.from(container.querySelectorAll("[data-page-item]"));
        var pagination = container.querySelector("[data-pagination]");
        var summary = pagination.querySelector(".activity-pagination-summary");
        var nav = pagination.querySelector("nav");
        var empty = container.querySelector("[data-filter-empty]");
        var tabs = container.querySelectorAll("[data-filter]");
        var applications = container.hasAttribute("data-applications");
        var size = 10;
        var page = 1;
        var filter = "all";

        if (applications) {
            var requestedPage = new URLSearchParams(window.location.search).get("page");
            if (/^\d+$/.test(requestedPage || "")) {
                page = Math.max(1, Number(requestedPage));
            } else {
                var selectedIndex = items.findIndex(function (item) { return item.classList.contains("selected"); });
                if (selectedIndex >= 0) page = Math.floor(selectedIndex / size) + 1;
            }
        }

        function render() {
            var matches = items.filter(function (item) {
                return filter === "all" || item.dataset.role === filter;
            });
            var pages = Math.max(1, Math.ceil(matches.length / size));
            page = Math.max(1, Math.min(page, pages));
            var start = (page - 1) * size;
            var visible = new Set(matches.slice(start, start + size));
            items.forEach(function (item) { item.hidden = !visible.has(item); });
            if (empty) empty.hidden = matches.length > 0;
            summary.textContent = "Hiển thị " + (matches.length ? start + 1 : 0) + "–"
                + Math.min(start + size, matches.length) + " trong " + matches.length + " kết quả";
            nav.replaceChildren();

            function button(label, target, disabled, current, ariaLabel) {
                var element = document.createElement("button");
                element.type = "button";
                element.className = "activity-page-link" + (current ? " is-active" : "");
                element.textContent = label;
                element.disabled = disabled;
                if (ariaLabel) element.setAttribute("aria-label", ariaLabel);
                if (current) element.setAttribute("aria-current", "page");
                element.addEventListener("click", function () { page = target; render(); });
                nav.appendChild(element);
            }

            if (pages > 1) {
                button("‹", page - 1, page === 1, false, "Trang trước");
                var first = Math.max(1, page - 2);
                var last = Math.min(pages, page + 2);
                if (first > 1) {
                    button("1", 1, false, false);
                    if (first > 2) {
                        var leading = document.createElement("span");
                        leading.className = "activity-page-ellipsis";
                        leading.textContent = "…";
                        nav.appendChild(leading);
                    }
                }
                for (var n = first; n <= last; n++) button(String(n), n, false, n === page);
                if (last < pages) {
                    if (last < pages - 1) {
                        var trailing = document.createElement("span");
                        trailing.className = "activity-page-ellipsis";
                        trailing.textContent = "…";
                        nav.appendChild(trailing);
                    }
                    button(String(pages), pages, false, false);
                }
                button("›", page + 1, page === pages, false, "Trang sau");
            }
            if (applications) {
                document.querySelectorAll(".rc-layout form[method='post']").forEach(function (form) {
                    var input = form.querySelector("input[name='page']");
                    if (!input) {
                        input = document.createElement("input");
                        input.type = "hidden";
                        input.name = "page";
                        form.appendChild(input);
                    }
                    input.value = String(page);
                    if (container.dataset.positionFilter && !form.querySelector("input[name='filterPositionId']")) {
                        var position = document.createElement("input");
                        position.type = "hidden";
                        position.name = "filterPositionId";
                        position.value = container.dataset.positionFilter;
                        form.appendChild(position);
                    }
                });
                items.forEach(function (item) {
                    var link = item.querySelector("a[href*='appId=']");
                    if (link) {
                        var url = new URL(link.href, window.location.href);
                        url.searchParams.set("page", String(page));
                        link.href = url.href;
                    }
                });
            }
            pagination.hidden = false;
        }

        tabs.forEach(function (tab) {
            tab.addEventListener("click", function () {
                tabs.forEach(function (other) { other.classList.toggle("active", other === tab); });
                filter = tab.dataset.filter;
                page = 1;
                render();
            });
        });
        if (applications) {
            var selectionCount = container.querySelector("[data-selection-count]");
            var checkboxes = container.querySelectorAll("input[name='ids']");
            function updateSelectionCount() {
                if (selectionCount) selectionCount.textContent = "Đã chọn: "
                    + Array.from(checkboxes).filter(function (box) { return box.checked; }).length + ".";
            }
            checkboxes.forEach(function (box) {
                box.addEventListener("change", updateSelectionCount);
            });
            updateSelectionCount();
        }
        render();
    });
}());
