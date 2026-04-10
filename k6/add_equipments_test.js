
import http from 'k6/http';
import { Counter } from 'k6/metrics';

export let successCount = new Counter('success');
export let stockOutCount = new Counter('stock_out');
export let errorCount = new Counter('errors');

export let options = {
    vus: 4,
    iterations: 4
};

const RESERVATION_IDS = [658, 659, 660, 661];
const EQUIPMENT_ID = 6;
const BASE_URL = 'http://localhost:8080';

// 각 VU가 직접 로그인
function login() {
    // 1. 로그인 페이지에서 CSRF 토큰 가져오기
    const loginPageRes = http.get(`${BASE_URL}/users/login`);
    const csrfMatch = loginPageRes.body.match(/name="_csrf"[^>]*value="([^"]+)"/);
    const csrfToken = csrfMatch ? csrfMatch[1] : null;

    // 2. 로그인 (k6가 자동으로 쿠키 관리)
    const loginRes = http.post(`${BASE_URL}/users/login`, {
        email: 'system@test.com',
        password: '1234',
        _csrf: csrfToken
    }, {
        redirects: 5
    });

    return loginRes.status === 200 || loginRes.status === 302;
}

export default function () {
    // 각 VU가 로그인
    const loggedIn = login();

    if (!loggedIn) {
        console.log(`❌ VU${__VU} 로그인 실패`);
        errorCount.add(1);
        return;
    }

    const reservationId = RESERVATION_IDS[__VU - 1];
    const url = `${BASE_URL}/api/v1/reservations/${reservationId}/equipments`;

    // API 요청 (쿠키는 k6가 자동 전송)
    const res = http.post(url, JSON.stringify({
        equipments: [{ equipmentId: EQUIPMENT_ID, quantity: 5 }]
    }), {
        headers: { 'Content-Type': 'application/json' }
    });

    console.log(`VU${__VU} 예약${reservationId}: ${res.status} - ${res.body ? res.body.substring(0, 200) : ''}`);

    if (res.status === 200 || res.status === 201) {
        successCount.add(1);
        console.log(`✅ VU${__VU} 성공`);
    } else if (res.body && res.body.includes('EQUIPMENT_002')) {
        stockOutCount.add(1);
        console.log(`⚠️ VU${__VU} 재고부족`);
    } else {
        errorCount.add(1);
        console.log(`❌ VU${__VU} 실패`);
    }
}

export function handleSummary(data) {
    const success = data.metrics.success ? data.metrics.success.values.count : 0;
    const stockOut = data.metrics.stock_out ? data.metrics.stock_out.values.count : 0;
    const errors = data.metrics.errors ? data.metrics.errors.values.count : 0;

    console.log('\n========================================');
    console.log('           테스트 결과');
    console.log('========================================');
    console.log(`✅ 성공: ${success}`);
    console.log(`⚠️ 재고부족: ${stockOut}`);
    console.log(`❌ 에러: ${errors}`);
    console.log('========================================\n');

    return {};
}