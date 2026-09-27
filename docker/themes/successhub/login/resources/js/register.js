/**
 * Instant client-side validation for SuccessHub Keycloak registration.
 * - Live username/email availability via BFF
 * - Password draft restore after Keycloak clears fields on error
 * - Submit disabled while any field shows an error
 */
(function () {
  var form = document.getElementById("kc-register-form");
  if (!form) return;

  var AVAIL_URL = "http://localhost:8082/public/registration/availability";
  var DRAFT_KEY = "sh-reg-draft-v1";

  var username = document.getElementById("username");
  var email = document.getElementById("email");
  var password = document.getElementById("password");
  var confirm = document.getElementById("password-confirm");
  var terms = document.getElementById("terms");
  var firstName = document.getElementById("firstName");
  var lastName = document.getElementById("lastName");
  var eye = document.getElementById("toggle-password");
  var submitBtn = document.getElementById("kc-register");

  var usernameTaken = false;
  var emailTaken = false;
  var usernameCheckSeq = 0;
  var emailCheckSeq = 0;
  var usernameTimer = null;
  var emailTimer = null;

  function setError(field, message) {
    var wrap = form.querySelector('[data-field="' + field + '"]');
    var err = document.getElementById("error-" + field);
    var input = wrap && wrap.querySelector("input:not([type=hidden]):not([type=checkbox])");
    if (field === "terms") {
      input = terms;
    }
    if (!err) return;
    if (message) {
      err.textContent = message;
      err.hidden = false;
      if (wrap) wrap.classList.add("sh-field--invalid");
      if (input) input.setAttribute("aria-invalid", "true");
    } else {
      err.textContent = "";
      err.hidden = true;
      if (wrap) wrap.classList.remove("sh-field--invalid");
      if (input) input.setAttribute("aria-invalid", "false");
    }
    updateSubmitState();
  }

  function hasVisibleErrors() {
    return !!form.querySelector(".sh-field--invalid");
  }

  function updateSubmitState() {
    if (!submitBtn) return;
    var disable = hasVisibleErrors();
    submitBtn.disabled = disable;
    submitBtn.setAttribute("aria-disabled", disable ? "true" : "false");
  }

  function syncNames() {
    if (!username || !firstName || !lastName) return;
    var value = (username.value || "").trim();
    if (!value) {
      firstName.value = "";
      lastName.value = "";
      return;
    }
    firstName.value = value;
    lastName.value = "Member";
  }

  function saveDraft() {
    try {
      sessionStorage.setItem(
        DRAFT_KEY,
        JSON.stringify({
          username: username ? username.value : "",
          email: email ? email.value : "",
          password: password ? password.value : "",
          confirm: confirm ? confirm.value : "",
          terms: terms ? !!terms.checked : false,
          savedAt: Date.now()
        })
      );
    } catch (e) {}
  }

  function restoreDraft() {
    try {
      var raw = sessionStorage.getItem(DRAFT_KEY);
      if (!raw) return;
      var draft = JSON.parse(raw);
      if (!draft || !draft.savedAt || Date.now() - draft.savedAt > 30 * 60 * 1000) {
        sessionStorage.removeItem(DRAFT_KEY);
        return;
      }
      // Keycloak clears passwords on validation errors — put them back.
      if (password && !password.value && draft.password) {
        password.value = draft.password;
      }
      if (confirm && !confirm.value && draft.confirm) {
        confirm.value = draft.confirm;
      }
      if (terms && draft.terms) {
        terms.checked = true;
      }
    } catch (e) {}
  }

  function clearDraft() {
    try {
      sessionStorage.removeItem(DRAFT_KEY);
    } catch (e) {}
  }

  function validateUsernameLocal() {
    var v = (username.value || "").trim();
    usernameTaken = false;
    if (!v) return setError("username", "Username is required"), false;
    if (v.length < 3) return setError("username", "Username must be at least 3 characters"), false;
    if (!/^[A-Za-z0-9._\-]+$/.test(v)) {
      return setError("username", "Use letters, numbers, dots, underscores, or hyphens"), false;
    }
    setError("username", "");
    return true;
  }

  function validateEmailLocal() {
    var v = (email.value || "").trim();
    emailTaken = false;
    if (!v) return setError("email", "Email is required"), false;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)) {
      return setError("email", "Enter a valid email address"), false;
    }
    setError("email", "");
    return true;
  }

  function validatePassword() {
    var v = password.value || "";
    if (!v) return setError("password", "Password is required"), false;
    if (v.length < 8) return setError("password", "Password must be at least 8 characters"), false;
    setError("password", "");
    return true;
  }

  function validateConfirm() {
    var v = confirm.value || "";
    if (!v) return setError("password-confirm", "Confirm your password"), false;
    if (v !== password.value) return setError("password-confirm", "Passwords do not match"), false;
    setError("password-confirm", "");
    return true;
  }

  function validateTerms() {
    if (!terms.checked) return setError("terms", "You must accept the Celestial Protocol"), false;
    setError("terms", "");
    return true;
  }

  function checkUsernameAvailability() {
    var v = (username.value || "").trim();
    if (!validateUsernameLocal()) return;
    var seq = ++usernameCheckSeq;
    fetch(AVAIL_URL + "?username=" + encodeURIComponent(v), {
      method: "GET",
      credentials: "omit",
      headers: { Accept: "application/json" }
    })
      .then(function (r) {
        return r.ok ? r.json() : Promise.reject();
      })
      .then(function (data) {
        if (seq !== usernameCheckSeq) return;
        if ((username.value || "").trim() !== v) return;
        if (data.usernameAvailable === false) {
          usernameTaken = true;
          setError("username", "Username is already taken");
        } else {
          usernameTaken = false;
          if (!form.querySelector('[data-field="username"].sh-field--invalid')) {
            setError("username", "");
          } else if (!usernameTaken) {
            setError("username", "");
          }
        }
      })
      .catch(function () {
        /* availability is best-effort; Keycloak still validates on submit */
      });
  }

  function checkEmailAvailability() {
    var v = (email.value || "").trim();
    if (!validateEmailLocal()) return;
    var seq = ++emailCheckSeq;
    fetch(AVAIL_URL + "?email=" + encodeURIComponent(v), {
      method: "GET",
      credentials: "omit",
      headers: { Accept: "application/json" }
    })
      .then(function (r) {
        return r.ok ? r.json() : Promise.reject();
      })
      .then(function (data) {
        if (seq !== emailCheckSeq) return;
        if ((email.value || "").trim() !== v) return;
        if (data.emailAvailable === false) {
          emailTaken = true;
          setError("email", "Email is already registered");
        } else {
          emailTaken = false;
          setError("email", "");
        }
      })
      .catch(function () {});
  }

  function scheduleUsernameCheck() {
    if (!validateUsernameLocal()) return;
    clearTimeout(usernameTimer);
    usernameTimer = setTimeout(checkUsernameAvailability, 400);
  }

  function scheduleEmailCheck() {
    if (!validateEmailLocal()) return;
    clearTimeout(emailTimer);
    emailTimer = setTimeout(checkEmailAvailability, 400);
  }

  function validateAll() {
    var ok = true;
    ok = validateUsernameLocal() && ok;
    ok = validateEmailLocal() && ok;
    ok = validatePassword() && ok;
    ok = validateConfirm() && ok;
    ok = validateTerms() && ok;
    if (usernameTaken) {
      setError("username", "Username is already taken");
      ok = false;
    }
    if (emailTaken) {
      setError("email", "Email is already registered");
      ok = false;
    }
    syncNames();
    updateSubmitState();
    return ok;
  }

  username.addEventListener("input", function () {
    usernameTaken = false;
    validateUsernameLocal();
    syncNames();
    scheduleUsernameCheck();
    saveDraft();
  });
  username.addEventListener("blur", function () {
    scheduleUsernameCheck();
  });

  email.addEventListener("input", function () {
    emailTaken = false;
    validateEmailLocal();
    scheduleEmailCheck();
    saveDraft();
  });
  email.addEventListener("blur", function () {
    scheduleEmailCheck();
  });

  password.addEventListener("input", function () {
    validatePassword();
    if (confirm.value) validateConfirm();
    saveDraft();
  });
  confirm.addEventListener("input", function () {
    validateConfirm();
    saveDraft();
  });
  terms.addEventListener("change", function () {
    validateTerms();
    saveDraft();
  });

  form.addEventListener("submit", function (e) {
    saveDraft();
    if (!validateAll() || hasVisibleErrors()) {
      e.preventDefault();
      updateSubmitState();
      var firstInvalid = form.querySelector(".sh-field--invalid .sh-field__input, .sh-field--invalid input");
      if (firstInvalid) firstInvalid.focus();
      return false;
    }
    // Keep draft until next page proves success; clear on successful app entry separately.
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.dataset.submitting = "1";
    }
    return true;
  });

  if (eye && password) {
    eye.addEventListener("click", function () {
      var show = password.type === "password";
      password.type = show ? "text" : "password";
      eye.setAttribute("aria-label", show ? "Hide password" : "Show password");
    });
  }

  document.body.classList.add("sh-register-page");
  restoreDraft();
  syncNames();

  // Re-validate restored / server-rendered state so the button starts disabled when needed.
  if ((username.value || "").trim()) {
    validateUsernameLocal();
    scheduleUsernameCheck();
  }
  if ((email.value || "").trim()) {
    validateEmailLocal();
    scheduleEmailCheck();
  }
  if (password.value) validatePassword();
  if (confirm.value) validateConfirm();
  if (form.querySelector(".sh-field--invalid")) {
    updateSubmitState();
  } else {
    updateSubmitState();
  }

  // If Keycloak painted server errors, keep submit disabled until the user fixes them.
  if (hasVisibleErrors()) {
    updateSubmitState();
  }
})();
