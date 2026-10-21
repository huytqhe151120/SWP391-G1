(function () {
    var API = (window.APP_CTX || '') + '/api/notifications';

    function esc(s) {
        var d = document.createElement('div');
        d.textContent = s == null ? '' : s;
        return d.innerHTML;
    }
    function fmt(iso) {
        var d = new Date(iso);
        return isNaN(d) ? '' : d.toLocaleString('vi-VN');
    }
    function get(params) {
        return fetch(API + '?' + new URLSearchParams(params), {credentials: 'same-origin'})
            .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); });
    }
    function post(params) {
        return fetch(API, {
            method: 'POST', credentials: 'same-origin',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: new URLSearchParams(params)
        }).then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); });
    }
    function safeHref(link) {
        return link && link.charAt(0) === '/' && link.charAt(1) !== '/' ? (window.APP_CTX || '') + link : null;
    }
    function itemHtml(n) {
        return '<a class="notif-item' + (n.read ? '' : ' unread') + '" href="#" data-id="' + n.id +
            '" data-link="' + esc(safeHref(n.link) || '') + '">' +
            '<strong>' + esc(n.title) + '</strong><span>' + esc(n.content) + '</span>' +
            '<small>' + fmt(n.createdAt) + ' · ' + (n.read ? 'Đã đọc' : 'Chưa đọc') + '</small></a>';
    }
    function setBadge(n) {
        var b = document.getElementById('notifBadge');
        if (!b) return;
        b.textContent = n > 99 ? '99+' : n;
        b.hidden = n <= 0;
    }
    // Đánh dấu đã đọc rồi mới chuyển trang tới chi tiết yêu cầu
    function bindItems(container, onChange) {
        container.addEventListener('click', function (e) {
            var a = e.target.closest('.notif-item');
            if (!a) return;
            e.preventDefault();
            var link = a.dataset.link;
            post({action: 'read', id: a.dataset.id}).catch(function () {}).then(function () {
                if (link) { window.location.href = link; } else { onChange(); }
            });
        });
    }

    // ---- Chuông trên navbar ----
    var bell = document.getElementById('notifBell');
    if (bell) {
        var panel = document.getElementById('notifPanel'),
            list = document.getElementById('notifDropdownList');

        function refreshBadge() {
            get({action: 'count'}).then(function (d) { setBadge(d.unread); }).catch(function () {});
        }
        function loadDropdown() {
            get({action: 'list', size: 8}).then(function (d) {
                setBadge(d.unread);
                list.innerHTML = d.items.length ? d.items.map(itemHtml).join('')
                    : '<p class="notif-empty">Không có thông báo.</p>';
            }).catch(function () { list.innerHTML = '<p class="notif-empty">Không tải được thông báo.</p>'; });
        }
        bell.addEventListener('click', function (e) {
            e.stopPropagation();
            panel.hidden = !panel.hidden;
            bell.setAttribute('aria-expanded', String(!panel.hidden));
            if (!panel.hidden) loadDropdown();
        });
        document.addEventListener('click', function (e) {
            if (!panel.hidden && !panel.contains(e.target)) { panel.hidden = true; bell.setAttribute('aria-expanded', 'false'); }
        });
        document.getElementById('notifReadAll').addEventListener('click', function () {
            post({action: 'readAll'}).then(function () { loadDropdown(); if (window.reloadNotifPage) window.reloadNotifPage(); });
        });
        bindItems(list, loadDropdown);
        refreshBadge();
        setInterval(refreshBadge, 30000);
    }

    // ---- Trang danh sách ----
    var page = document.getElementById('notifList');
    if (page) {
        var state = {filter: 'all', page: 1}, pager = document.getElementById('notifPager');
        function load() {
            get({action: 'list', filter: state.filter, page: state.page, size: 10}).then(function (d) {
                state.page = d.page;
                setBadge(d.unread);
                page.innerHTML = d.items.length ? d.items.map(itemHtml).join('')
                    : '<p class="notif-empty">Không có thông báo.</p>';
                pager.innerHTML = '<button ' + (d.page <= 1 ? 'disabled' : '') + ' data-p="' + (d.page - 1) + '">‹ Trước</button>' +
                    '<span>Trang ' + d.page + '/' + d.pages + ' (' + d.total + ')</span>' +
                    '<button ' + (d.page >= d.pages ? 'disabled' : '') + ' data-p="' + (d.page + 1) + '">Sau ›</button>';
            }).catch(function () { page.innerHTML = '<p class="notif-empty">Không tải được thông báo.</p>'; });
        }
        window.reloadNotifPage = load;
        pager.addEventListener('click', function (e) {
            var b = e.target.closest('button[data-p]');
            if (b && !b.disabled) { state.page = +b.dataset.p; load(); }
        });
        document.getElementById('notifFilter').addEventListener('click', function (e) {
            var b = e.target.closest('button[data-filter]');
            if (!b) return;
            state.filter = b.dataset.filter; state.page = 1;
            this.querySelectorAll('button').forEach(function (x) { x.classList.toggle('active', x === b); });
            load();
        });
        document.getElementById('notifPageReadAll').addEventListener('click', function () {
            post({action: 'readAll'}).then(load);
        });
        bindItems(page, load);
        load();
    }
})();