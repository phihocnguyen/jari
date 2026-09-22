import http from 'k6/http';
import { check } from 'k6';
import { getConfig } from './config.js';
import { JSON_HEADERS } from './http.js';

export function login() {
  const { baseUrl, email, password } = getConfig();
  if (!email || !password) {
    return null;
  }

  const res = http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: JSON_HEADERS, tags: { name: 'POST /auth/login' } },
  );

  check(res, {
    'login status 200': (r) => r.status === 200,
    'login returns token': (r) => !!r.json('data.accessToken'),
  });

  return res.json('data.accessToken');
}
