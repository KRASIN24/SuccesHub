(function () {
  function readCookie(name) {
    var m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
  }
  var params = new URLSearchParams(window.location.search);
  var theme = params.get('sh_theme') || readCookie('successhub_theme') || 'dark';
  var dark = theme !== 'light';
  document.documentElement.classList.toggle('app-dark', dark);
  if (document.body) {
    document.body.classList.toggle('app-dark', dark);
  }
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
})();
