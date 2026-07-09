/* ============================================================
   ELLITHIUM TEST ARENA — SHARED NAVIGATION & UTILITIES
   _shared.js — included on every page
============================================================ */
'use strict';

/* ── Mark current nav link active ── */
(function () {
  var path = location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-link').forEach(function (a) {
    if (a.getAttribute('href') === path) a.classList.add('active');
  });
})();

/* ── Console logger ── */
var ArenaLog = (function () {
  function log(boxId, type, msg) {
    var box = document.getElementById(boxId);
    if (!box) return;
    var ts  = new Date().toLocaleTimeString();
    var div = document.createElement('div');
    div.innerHTML = '<span class="line-ts">[' + ts + ']</span>' +
                    '<span class="line-' + type + '">' + escapeHtml(msg) + '</span>';
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
  }
  function escapeHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }
  return {
    ok:   function (id, m) { log(id, 'ok',   '✅ ' + m); },
    warn: function (id, m) { log(id, 'warn', '⚠ '  + m); },
    err:  function (id, m) { log(id, 'err',  '✗ '  + m); },
    info: function (id, m) { log(id, 'info', 'ℹ '  + m); },
    raw:  function (id, t, m) { log(id, t, m); },
    clear: function (id)   {
      var b = document.getElementById(id);
      if (b) b.innerHTML = '';
    }
  };
})();

/* ── Status helper ── */
function setStatus(id, type, text) {
  var el = document.getElementById(id);
  if (!el) return;
  el.className = 'status status-' + type;
  el.textContent = text;
  el.style.display = 'flex';
}

/* ── Timestamp ── */
function ts() { return new Date().toISOString(); }

/* ── Random int in [min, max] ── */
function randInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }
