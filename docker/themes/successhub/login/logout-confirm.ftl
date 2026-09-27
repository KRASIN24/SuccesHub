<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayInfo=false; section>
    <#if section = "header">
        <span class="sh-eyebrow">Session</span>
        <span class="sh-title-line">Signing</span>
        <span class="sh-title-accent">out</span>
    <#elseif section = "form">
        <#-- Skip Keycloak's "Do you want to logout?" prompt — auto-confirm immediately. -->
        <div id="kc-logout-confirm" class="sh-info">
            <p class="sh-info__text">Signing you out of SuccessHub…</p>
            <form id="kc-logout-confirm-form" class="form-actions" action="${url.logoutConfirmAction}" method="POST">
                <input type="hidden" name="session_code" value="${logoutConfirm.code}">
                <noscript>
                    <div class="sh-info__actions">
                        <button class="sh-info__primary" name="confirmLogout" id="kc-logout" type="submit">
                            ${msg("doLogout")}
                        </button>
                    </div>
                </noscript>
            </form>
        </div>
        <script>
          document.body.classList.add("sh-info-page");
          var form = document.getElementById("kc-logout-confirm-form");
          if (form) {
            // Hidden field required by Keycloak when confirming via POST.
            var confirm = document.createElement("input");
            confirm.type = "hidden";
            confirm.name = "confirmLogout";
            confirm.value = "Logout";
            form.appendChild(confirm);
            form.submit();
          }
        </script>
    </#if>
</@layout.registrationLayout>
