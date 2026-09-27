(function () {
  function readCookie(name) {
    var m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
  }

  function writeCookie(name, value) {
    document.cookie =
      name +
      '=' +
      encodeURIComponent(value) +
      '; path=/; SameSite=Lax; max-age=31536000';
  }

  function shortLocale(raw) {
    if (!raw) return null;
    var v = String(raw).toLowerCase();
    if (v.indexOf('pl') === 0) return 'pl';
    if (v.indexOf('en') === 0) return 'en';
    return null;
  }

  function paramFromReferrer(name) {
    try {
      if (!document.referrer) return null;
      return new URL(document.referrer).searchParams.get(name);
    } catch (e) {
      return null;
    }
  }

  var params = new URLSearchParams(window.location.search);

  // Theme: query wins, then auth-URL referrer (Keycloak drops custom params on
  // login-actions), then cookie. Persist so later login-actions keep light/dark.
  var themeParam = params.get('sh_theme') || paramFromReferrer('sh_theme');
  var theme = themeParam || readCookie('successhub_theme') || 'dark';
  if (theme !== 'light' && theme !== 'dark') {
    theme = 'dark';
  }
  writeCookie('successhub_theme', theme);
  var dark = theme !== 'light';
  document.documentElement.classList.toggle('app-dark', dark);
  if (document.body) {
    document.body.classList.toggle('app-dark', dark);
  }
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light';

  // Locale: seed Keycloak's own cookie so FreeMarker messages stay pl/en
  // after the auth URL is rewritten to /login-actions/*. If we learn locale
  // from the auth referrer and the cookie was missing/wrong, reload once so
  // FreeMarker re-renders with the correct bundle.
  var loc =
    shortLocale(params.get('ui_locales')) ||
    shortLocale(params.get('kc_locale')) ||
    shortLocale(paramFromReferrer('ui_locales')) ||
    shortLocale(paramFromReferrer('kc_locale')) ||
    shortLocale(readCookie('successhub_locale')) ||
    shortLocale(readCookie('KEYCLOAK_LOCALE'));
  if (loc) {
    var prev = shortLocale(readCookie('KEYCLOAK_LOCALE'));
    writeCookie('successhub_locale', loc === 'pl' ? 'pl-PL' : 'en-US');
    writeCookie('KEYCLOAK_LOCALE', loc);
    if (prev !== loc && !sessionStorage.getItem('sh_locale_reloaded')) {
      sessionStorage.setItem('sh_locale_reloaded', '1');
      window.location.reload();
      return;
    }
  }
})();
