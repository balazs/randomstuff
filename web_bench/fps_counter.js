var frameTimeStamps = new Array();
var WINDOW = 20;
var SAMPLES_PER_AVG_RECOUNT = 5;
var minFps = Infinity;
var maxFps = -Infinity;
var samples = new Array();
var oldSum = 0;
var numSamples = 0;

var tick =
    function()
    {
        var round2 = function(x)
        {
            var xF = Math.floor(x);
            return xF + (Math.floor(((x - xF) * 1e2)) / 1e2);
        }
        var emitValue = function(id, value) { document.getElementById(id).innerHTML = round2(value); }

        frameTimeStamps.push(Date.now());

        // Sanity check
        if (frameTimeStamps.length > WINDOW + 1) {
            document.getElementById('fps_counter').innerHTML = "error";
            return;
        }

        if (frameTimeStamps.length == WINDOW + 1) {
            var elapsed = frameTimeStamps[frameTimeStamps.length - 1] - frameTimeStamps[0];
            var fps = 1000 * ((frameTimeStamps.length - 1) / elapsed);
            samples.push(fps);
            ++numSamples;
            emitValue('fps_counter', fps);
            frameTimeStamps.splice(0, WINDOW / 4); // Remove some old values.
        }

        if (samples.length == SAMPLES_PER_AVG_RECOUNT) {
            var sum = oldSum;
            for (var i = 0; i < samples.length; ++i)
                sum += samples[i];
            oldSum = sum;
            var avg = sum / numSamples;
            samples.length = 0;
            emitValue('avg_fps_counter', avg);
        }

        requestAnimationFrame(tick);
    }

requestAnimationFrame(tick);
