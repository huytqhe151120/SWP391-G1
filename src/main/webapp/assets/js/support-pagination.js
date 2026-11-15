(function () {
    'use strict';

    function button(text) {
        const element = document.createElement('button');
        element.type = 'button';
        element.className = 'support-page-button';
        element.textContent = text;
        return element;
    }

    document.querySelectorAll('[data-support-pages]').forEach(function (list) {
        const items = Array.from(list.querySelectorAll(list.dataset.supportItem));
        const size = Number(list.dataset.supportPages);
        if (items.length <= size) return;

        const selected = list.dataset.supportSelected
            ? items.findIndex(function (item) { return item.matches(list.dataset.supportSelected); })
            : -1;
        let page = selected < 0 ? 0 : Math.floor(selected / size);
        const lastPage = Math.ceil(items.length / size) - 1;
        const nav = document.createElement('nav');
        nav.className = 'support-pagination';
        nav.setAttribute('aria-label', 'Phân trang ' + list.dataset.supportLabel);
        const summary = document.createElement('span');
        summary.setAttribute('aria-live', 'polite');
        const previous = button('Trước');
        const next = button('Tiếp');
        nav.append(summary, previous, next);
        list.insertAdjacentElement('afterend', nav);

        function render() {
            const start = page * size;
            items.forEach(function (item, index) {
                item.hidden = index < start || index >= start + size;
            });
            summary.textContent = 'Hiển thị ' + (start + 1) + '–' + Math.min(start + size, items.length)
                + ' trong ' + items.length + ' ' + list.dataset.supportLabel
                + ' · Trang ' + (page + 1) + '/' + (lastPage + 1);
            previous.disabled = page === 0;
            next.disabled = page === lastPage;
        }
        previous.addEventListener('click', function () { if (page > 0) { page--; render(); } });
        next.addEventListener('click', function () { if (page < lastPage) { page++; render(); } });
        render();
    });

    document.querySelectorAll('[data-support-chat]').forEach(function (area) {
        const messages = Array.from(area.querySelectorAll(area.dataset.supportMessage));
        const size = Number(area.dataset.supportChat);
        let first = Math.max(0, messages.length - size);
        if (first > 0) {
            const older = button('');
            older.classList.add('support-load-older');
            area.prepend(older);
            function render() {
                messages.forEach(function (message, index) { message.hidden = index < first; });
                older.hidden = first === 0;
                older.textContent = 'Xem tin nhắn cũ hơn (' + first + ' tin)';
            }
            older.addEventListener('click', function () {
                const oldHeight = area.scrollHeight;
                const oldTop = area.scrollTop;
                first = Math.max(0, first - size);
                render();
                area.scrollTop = oldTop + area.scrollHeight - oldHeight;
            });
            render();
        }
        area.scrollTop = area.scrollHeight;
    });
}());
