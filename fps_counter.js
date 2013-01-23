var frameTimeStamps = new Array();
var MAX_FRAMES = 20;
var KEEP_FRAMES = 10;

var tick = function() {
    frameTimeStamps.push(Date.now());
    if (frameTimeStamps.length == MAX_FRAMES) {
        var elapsed = frameTimeStamps[frameTimeStamps.length - 1] - frameTimeStamps[0];
        var fps = (1000 / elapsed) * frameTimeStamps.length;
        document.getElementById('fps_counter').innerHTML = fps;
        // Remove oldest.
        frameTimeStamps.splice(0, MAX_FRAMES - KEEP_FRAMES);
    }
    requestAnimationFrame(tick);
};
requestAnimationFrame(tick);
