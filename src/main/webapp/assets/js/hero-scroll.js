// Hiệu ứng cuộn cho hero + vùng danh sách, dùng chung cho các trang có class "hero-scroll".
// - Chữ trong hero mờ dần + blur dần, hoàn tất trước khi hero khép hẳn.
// - Hero co dần chiều cao rồi biến mất khi cuộn xuống.
// - Mép trên vùng cuộn fade theo vị trí cuộn (seamless với header).
(function () {
    var TEXT_FADE_END = 0.7; // chữ tan hết tại mốc tiến trình này (trước khi hero khép về 0)
    var MAX_BLUR = 4;        // px
    var MAX_FADE = 8;        // px fade mép trên tối đa

    document.querySelectorAll(".hero-scroll").forEach(function (root) {
        var scroller = root.querySelector(".activity-app-main");
        var hero = root.querySelector(".activity-hero");
        if (!scroller || !hero) {
            return;
        }
        var contents = hero.children;
        var naturalHeight = 0;
        var distance = 1;
        var lastHeight = -1;
        var lastOpacity = -1;
        var lastFade = -1;
        var ticking = false;

        function measure() {
            hero.style.maxHeight = "";
            naturalHeight = hero.offsetHeight;
            distance = Math.max(naturalHeight, 1);
        }

        function render() {
            ticking = false;
            var progress = Math.min(1, Math.max(0, scroller.scrollTop / distance));

            // Fade mép trên theo vị trí cuộn: ở đầu = 0, cuộn tới đâu mờ tới đó (tối đa MAX_FADE).
            var fade = Math.min(MAX_FADE, Math.round(scroller.scrollTop));
            if (fade !== lastFade) {
                scroller.style.setProperty("--scroll-fade", fade + "px");
                lastFade = fade;
            }

            var height = Math.round(naturalHeight * (1 - progress));
            if (height !== lastHeight) {
                hero.style.maxHeight = height + "px";
                hero.style.pointerEvents = progress >= 1 ? "none" : "";
                lastHeight = height;
            }

            // Chữ: mờ dần + blur dần, hoàn tất ở TEXT_FADE_END (trước khi hero khép hẳn).
            var textProgress = Math.min(1, progress / TEXT_FADE_END);
            var opacity = Math.round((1 - textProgress) * 100) / 100;
            if (opacity !== lastOpacity) {
                var blur = (textProgress * MAX_BLUR).toFixed(2);
                var filter = textProgress > 0 ? "blur(" + blur + "px)" : "";
                for (var i = 0; i < contents.length; i++) {
                    contents[i].style.opacity = opacity;
                    contents[i].style.filter = filter;
                }
                lastOpacity = opacity;
            }
        }

        function onScroll() {
            if (!ticking) {
                ticking = true;
                requestAnimationFrame(render);
            }
        }

        measure();
        render();
        scroller.addEventListener("scroll", onScroll, { passive: true });
        window.addEventListener("resize", function () {
            lastHeight = -1;
            lastOpacity = -1;
            lastFade = -1;
            measure();
            render();
        });
    });
})();
