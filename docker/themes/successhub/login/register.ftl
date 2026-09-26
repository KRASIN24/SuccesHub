<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayInfo=false; section>
    <#if section = "header">
        <#-- Branding lives in the enrollment layout; keep header empty. -->
    <#elseif section = "form">
    <div class="sh-enroll" id="sh-enroll">
      <script>document.body.classList.add("sh-register-page");</script>
      <aside class="sh-enroll__brand" aria-label="SuccessHub">
        <p class="sh-enroll__eyebrow">Initiate Protocol</p>
        <h1 class="sh-enroll__title">
          <span class="sh-enroll__title-strong">SuccessHub</span>
          <span class="sh-enroll__title-light">Elite Access</span>
        </h1>
        <p class="sh-enroll__lede">
          Step into the vacuum of luxury. Join the celestial circle where achievement is curated and precision is the only standard.
        </p>
        <div class="sh-enroll__members">
          <div class="sh-enroll__avatars" aria-hidden="true">
            <span class="sh-enroll__avatar sh-enroll__avatar--a"></span>
            <span class="sh-enroll__avatar sh-enroll__avatar--b"></span>
            <span class="sh-enroll__avatar sh-enroll__avatar--count">+4k</span>
          </div>
          <span class="sh-enroll__members-label">Elite Tier Members</span>
        </div>
      </aside>

      <div class="sh-enroll__vessel">
        <form id="kc-register-form"
              class="sh-enroll__form"
              action="${url.registrationAction}"
              method="post"
              novalidate>

          <#-- Keycloak often requires first/last name; keep in sync with username for the user store. -->
          <input type="hidden" id="firstName" name="firstName" value="${(register.formData.firstName!'')}" />
          <input type="hidden" id="lastName" name="lastName" value="${(register.formData.lastName!'')}" />

          <div class="sh-enroll__grid">
            <div class="sh-field<#if messagesPerField.existsError('username')> sh-field--invalid</#if>" data-field="username">
              <label class="sh-field__label" for="username">Username</label>
              <div class="sh-field__control">
                <input class="sh-field__input"
                       type="text"
                       id="username"
                       name="username"
                       autocomplete="username"
                       placeholder="THE_ARCHITECT"
                       value="${(register.formData.username!'')}"
                       aria-invalid="<#if messagesPerField.existsError('username')>true<#else>false</#if>"
                       required
                       minlength="3"
                       maxlength="64"
                       pattern="[A-Za-z0-9._\-]+" />
              </div>
              <p class="sh-field__error" id="error-username" role="alert" aria-live="polite"
                 <#if !messagesPerField.existsError('username')>hidden</#if>>
                <#if messagesPerField.existsError('username')>
                  ${kcSanitize(messagesPerField.getFirstError('username'))?no_esc}
                </#if>
              </p>
            </div>

            <div class="sh-field<#if messagesPerField.existsError('email')> sh-field--invalid</#if>" data-field="email">
              <label class="sh-field__label" for="email">Celestial Email</label>
              <div class="sh-field__control">
                <input class="sh-field__input"
                       type="email"
                       id="email"
                       name="email"
                       autocomplete="email"
                       placeholder="curator@successhub.io"
                       value="${(register.formData.email!'')}"
                       aria-invalid="<#if messagesPerField.existsError('email')>true<#else>false</#if>"
                       required
                       maxlength="254" />
              </div>
              <p class="sh-field__error" id="error-email" role="alert" aria-live="polite"
                 <#if !messagesPerField.existsError('email')>hidden</#if>>
                <#if messagesPerField.existsError('email')>
                  ${kcSanitize(messagesPerField.getFirstError('email'))?no_esc}
                </#if>
              </p>
            </div>

            <div class="sh-field<#if messagesPerField.existsError('password')> sh-field--invalid</#if>" data-field="password">
              <label class="sh-field__label" for="password">Password</label>
              <div class="sh-field__control sh-field__control--icon">
                <input class="sh-field__input"
                       type="password"
                       id="password"
                       name="password"
                       autocomplete="new-password"
                       placeholder="••••••••••••"
                       aria-invalid="<#if messagesPerField.existsError('password')>true<#else>false</#if>"
                       required
                       minlength="8"
                       maxlength="128" />
                <button type="button" class="sh-field__eye" id="toggle-password" aria-label="Show password" tabindex="-1">
                  <svg viewBox="0 0 24 24" width="18" height="14" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true">
                    <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/>
                    <circle cx="12" cy="12" r="2.5"/>
                  </svg>
                </button>
              </div>
              <p class="sh-field__error" id="error-password" role="alert" aria-live="polite"
                 <#if !messagesPerField.existsError('password')>hidden</#if>>
                <#if messagesPerField.existsError('password')>
                  ${kcSanitize(messagesPerField.getFirstError('password'))?no_esc}
                </#if>
              </p>
            </div>

            <div class="sh-field<#if messagesPerField.existsError('password-confirm')> sh-field--invalid</#if>" data-field="password-confirm">
              <label class="sh-field__label" for="password-confirm">Confirm Access</label>
              <div class="sh-field__control">
                <input class="sh-field__input"
                       type="password"
                       id="password-confirm"
                       name="password-confirm"
                       autocomplete="new-password"
                       placeholder="••••••••••••"
                       aria-invalid="<#if messagesPerField.existsError('password-confirm')>true<#else>false</#if>"
                       required
                       minlength="8"
                       maxlength="128" />
              </div>
              <p class="sh-field__error" id="error-password-confirm" role="alert" aria-live="polite"
                 <#if !messagesPerField.existsError('password-confirm')>hidden</#if>>
                <#if messagesPerField.existsError('password-confirm')>
                  ${kcSanitize(messagesPerField.getFirstError('password-confirm'))?no_esc}
                </#if>
              </p>
            </div>
          </div>

          <div class="sh-field sh-field--terms" data-field="terms">
            <label class="sh-terms">
              <input type="checkbox" id="terms" name="terms" value="true" required />
              <span class="sh-terms__box" aria-hidden="true"></span>
              <span class="sh-terms__text">I accept the Celestial Protocol</span>
            </label>
            <p class="sh-field__error" id="error-terms" role="alert" aria-live="polite" hidden></p>
          </div>

          <div class="sh-enroll__actions">
            <button type="submit" class="sh-enroll__submit" id="kc-register" name="register">
              <span>Initialize Access</span>
              <svg class="sh-enroll__submit-icon" viewBox="0 0 12 12" width="10" height="10" aria-hidden="true">
                <path d="M1 6h9M6.5 2.5 10 6l-3.5 3.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </form>

        <footer class="sh-enroll__footer">
          <p class="sh-enroll__footer-hint">Already hold status?</p>
          <#-- Fresh BFF login — loginRestartFlowUrl / loginUrl die when the registration tab session expires. -->
          <a class="sh-enroll__footer-link" href="http://localhost:4200/oauth2/authorization/keycloak">
            Return to Vault
            <svg viewBox="0 0 16 8" width="14" height="7" aria-hidden="true">
              <path d="M1 4h12M10 1l4 3-4 3" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </a>
        </footer>
      </div>
    </div>

    <div class="sh-enroll__status" aria-hidden="true">
      <div class="sh-enroll__status-block">
        <span class="sh-enroll__status-label">System Status</span>
        <span class="sh-enroll__status-value sh-enroll__status-value--gold">Operational</span>
      </div>
      <span class="sh-enroll__status-divider"></span>
      <div class="sh-enroll__status-block">
        <span class="sh-enroll__status-label">Uptime</span>
        <span class="sh-enroll__status-value">99.998%</span>
      </div>
    </div>

    <script src="${url.resourcesPath}/js/register.js"></script>
    </#if>
</@layout.registrationLayout>
