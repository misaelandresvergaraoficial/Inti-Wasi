import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const EMAIL = __ENV.K6_EMAIL;
const PASSWORD = __ENV.K6_PASSWORD;

export const options = {
  scenarios: {
    historial: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '5s', target: 5 },
        { duration: '10s', target: 5 },
        { duration: '5s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
  },
};

export function setup() {
  const response = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ correo: EMAIL, contrasena: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(response, { 'login ok': (r) => r.status === 200 });
  return { token: response.json('token') };
}

export default function (data) {
  const response = http.get(
    `${BASE}/api/movimientos?page=0&size=20&sort=fechaEmision,desc`,
    { headers: { Authorization: `Bearer ${data.token}` } },
  );
  check(response, {
    'estado 200': (r) => r.status === 200,
    'pagina con contenido': (r) => Array.isArray(r.json('content')),
  });
  sleep(1);
}
