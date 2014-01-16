// Scrolling.
var SCROLL = 800;
var dir = 1;
var pos = 0;
var max = document.body.scrollHeight - window.innerHeight;
var scroll =
    function() {
        if (dir == 1) {
            if (pos == max) {
                dir = -1;
                pos -= SCROLL;
            } else if (pos + SCROLL > max) {
                pos = max;
            } else {
                pos += SCROLL;
            }
        } else if (pos == 0) {
            dir = 1;
            pos += SCROLL;
        } else if (pos - SCROLL < 0) {
            pos = 0;
        } else {
            pos -= SCROLL;
        }
        window.scrollTo(0, pos);
        setTimeout(scroll, 100);
    }
setTimeout(scroll, 100);
