import { check } from 'k6';

export const JSON_HEADERS = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

export function authHeaders(token) {
  const headers = { ...JSON_HEADERS };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

export function checkOk(res, name) {
  return check(res, {
    [`${name} status 2xx`]: (r) => r.status >= 200 && r.status < 300,
  });
}

export function checkJson(res, name) {
  return check(res, {
    [`${name} has data`]: (r) => {
      try {
        const body = r.json();
        return body && body.data !== undefined;
      } catch {
        return false;
      }
    },
  });
}
