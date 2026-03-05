import http from "k6/http";
import { check, sleep, group } from "k6";

// -------------------------
// Load Configuration
// -------------------------
export const options = {
    scenarios: {
        extreme_load: {
            executor: "ramping-vus",
            startVUs: 20,
            stages: [
                { duration: "1m", target: 50 },   // warm up
                { duration: "2m", target: 100 },   // heavy load
                { duration: "2m", target: 300 },  // stress level
                { duration: "2m", target: 600 },  // extreme load
                { duration: "1m", target: 0 }      // cool down
            ],
            gracefulRampDown: "30s"
        }
    },

    thresholds: {
        http_req_failed: ["rate<0.05"],      // allow <5% failures under extreme load
        http_req_duration: ["p(95)<2000"],   // 95% under 2s
        http_reqs: ["rate>100"]              // at least 100 req/sec throughput
    }
};

// -------------------------
// Base Config
// -------------------------
const BASE_URL = "http://localhost:4004";

const headers = {
    "Content-Type": "application/json"
};

// -------------------------
// Setup (Login once)
// -------------------------
export function setup() {

    const payload = JSON.stringify({
        email: "testuser@test.com",
        password: "password123"
    });

    const res = http.post(
        `${BASE_URL}/auth/login`,
        payload,
        { headers }
    );

    check(res, {
        "login successful": (r) => r.status === 200
    });

    const token = res.json("token");

    return { token };
}

// -------------------------
// Test Scenario
// -------------------------
export default function (data) {

    const authHeaders = {
        headers: {
            ...headers,
            Authorization: `Bearer ${data.token}`
        }
    };

    group("Create Patient", () => {

        const payload = JSON.stringify({
            name: `User-${__VU}-${__ITER}`,
            email: `user_${__VU}_${__ITER}@example.com`,
            address: "Baner, Pune, Maharashtra",
            dateOfBirth: "1992-11-03"
        });

        const res = http.post(
            `${BASE_URL}/api/patients`,
            payload,
            authHeaders
        );

        check(res, {
            "patient created": (r) =>
                r.status === 201 || r.status === 200
        });

        if (res.status !== 201 && res.status !== 200) {
            console.error(`Create failed: ${res.status} ${res.body}`);
        }
    });

    group("Fetch Patients", () => {

        const res = http.get(
            `${BASE_URL}/api/patients?sort=asc&size=2&page=1`,
            authHeaders
        );

        check(res, {
            "patients fetched": (r) => r.status === 200
        });

        if (res.status !== 200) {
            console.error(`Fetch failed: ${res.status}`);
        }
    });

    sleep(1);
}