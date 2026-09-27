<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayInfo=false; section>
    <#if section = "header">
        <span class="sh-eyebrow">Session Active</span>
        <span class="sh-title-line">Already signed in</span>
        <span class="sh-title-accent">SuccessHub</span>
    <#elseif section = "form">
    <div id="kc-info-message" class="sh-info">
        <p class="sh-info__text">
            Your Keycloak session is still active. Continue into the app, or sign out to switch / create another account.
        </p>

        <div class="sh-info__actions">
            <#-- Hard-coded SPA escape hatch — do not depend on client.baseUrl or optional url.* helpers. -->
            <a class="sh-info__primary" id="sh-continue-app" href="http://localhost:4200/oauth2/authorization/keycloak">Continue to SuccessHub</a>
            <a class="sh-info__secondary" id="sh-signout-kc"
               href="http://localhost:8080/realms/succeshub-realm/protocol/openid-connect/logout?post_logout_redirect_uri=http%3A%2F%2Flocalhost%3A4200%2F&client_id=succeshub-backend">
                Sign out of Keycloak
            </a>
        </div>

        <p class="sh-info__hint" id="sh-info-hint">
            Continuing into SuccessHub…
        </p>
    </div>
    <script>
      document.body.classList.add("sh-info-page");
      // After registration the OAuth handoff can land here; start BFF login automatically
      // so the user does not need an extra click. Cancel if they choose Sign out.
      (function () {
        var cont = document.getElementById("sh-continue-app");
        var signout = document.getElementById("sh-signout-kc");
        var hint = document.getElementById("sh-info-hint");
        if (!cont) return;
        var timer = window.setTimeout(function () {
          window.location.replace(cont.href);
        }, 900);
        function cancelAuto() {
          window.clearTimeout(timer);
          if (hint) {
            hint.textContent = "Creating a new account requires signing out first, then using Create account on the login page.";
          }
        }
        if (signout) {
          signout.addEventListener("click", cancelAuto);
          signout.addEventListener("pointerdown", cancelAuto);
        }
      })();
      // #region agent log
      try {
        fetch('http://127.0.0.1:7452/ingest/37b8ef57-bd0f-4015-8754-90251ebe3ff8', {
          method: 'POST',
          headers: {'Content-Type': 'application/json', 'X-Debug-Session-Id': '0d3552'},
          body: JSON.stringify({
            sessionId: '0d3552',
            runId: 'post-fix',
            hypothesisId: 'B',
            location: 'info.ftl:beacon',
            message: 'custom-info-ftl-auto-continue',
            data: {
              continueHref: (document.getElementById('sh-continue-app') || {}).href || null,
              hasContinue: !!document.getElementById('sh-continue-app')
            },
            timestamp: Date.now()
          })
        }).catch(function () {});
      } catch (e) {}
      // #endregion
    </script>
    </#if>
</@layout.registrationLayout>
