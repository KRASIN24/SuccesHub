import { HttpInterceptorFn } from '@angular/common/http';

/**
 * BFF auth model: authentication is carried by the HttpOnly session cookie, not
 * a bearer token. We only need to make sure credentials (cookies) are sent with
 * every API request. Angular's built-in XSRF support adds the X-XSRF-TOKEN
 * header automatically for mutating requests.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
