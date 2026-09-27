<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "header">
        <span class="sh-eyebrow">Account Recovery</span>
        <span class="sh-title-line">Reset your</span>
        <span class="sh-title-accent">access key</span>
    <#elseif section = "form">
    <div id="kc-form">
      <div id="kc-form-wrapper">
        <p class="sh-subtitle">
            <#if realm.duplicateEmailsAllowed>
                ${msg("emailInstructionUsername")}
            <#else>
                ${msg("emailInstruction")}
            </#if>
        </p>

        <form id="kc-reset-password-form" class="${properties.kcFormClass!} sh-form" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!} sh-group">
                <label for="username" class="${properties.kcLabelClass!} sh-label">
                    <span>
                        <#if !realm.loginWithEmailAllowed>
                            ${msg("username")}
                        <#elseif !realm.registrationEmailAsUsername>
                            ${msg("usernameOrEmail")}
                        <#else>
                            ${msg("email")}
                        </#if>
                    </span>
                    <svg class="sh-icon sh-label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>
                </label>

                <input type="text" id="username" name="username" class="${properties.kcInputClass!} sh-input"
                       autofocus value="${(auth.attemptedUsername!'')}"
                       placeholder="Email or Username"
                       aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"/>

                <#if messagesPerField.existsError('username')>
                    <span id="input-error-username" class="${properties.kcInputErrorMessageClass!} sh-error" aria-live="polite">
                        <svg class="sh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>
                        <span>${kcSanitize(messagesPerField.get('username'))?no_esc}</span>
                    </span>
                </#if>
            </div>

            <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                <button class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} sh-submit" type="submit">
                    <span>Send instructions</span>
                    <svg class="sh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12h14"/><path d="m13 6 6 6-6 6"/></svg>
                </button>
            </div>

            <p class="sh-create-account">
                <#-- loginUrl can hit a dead auth session → Keycloak error page; restart flow is safe. -->
                <a href="${url.loginRestartFlowUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a>
            </p>
        </form>
      </div>
    </div>
    </#if>
</@layout.registrationLayout>
