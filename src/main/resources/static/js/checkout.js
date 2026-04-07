// 2. 클라이언트 키로 SDK 초기화 -> 브라우저에서 토스페이먼츠가 상점 정보를 확인하기 위함
const clientKey = /*[[${clientKey}]]*/ "";
const tossPayments = TossPayments(clientKey);

// 3. 결제창 띄우기 -> 회원 id로 회원결제를 할 결제창을 띄움
const customerKey = /*[[${customerKey}]]*/ "";

const payment = tossPayments.payment({customerKey});

// 들어갈
const amount = Number(/*[[${amount}]]*/ 0); // 금액
const orderId = /*[[${orderId}]]*/ ""; // 고유 주문 번호
const orderName = /*[[${orderName}]]*/ ""; // 주문명 'A회의실 1시 예약, 비품 2건'
const customerEmail = /*[[${customerEmail}]]*/ ""; // 주문자 이메일
const customerName = /*[[${customerName}]]*/ ""; // 주문자 이름
// 휴대폰 번호도 넣을 수는 있음

// 4. 결제창을 띄우는 메서드 생성
async function requestPayment() {
    try {
        // 5. 결제를 요청하기 전 orderId, amount를 서버에 저장해야함
        // => 악의적으로 금액이 변경되는 것을 확인하는 용도
        // 예시
        const orderData = {
            orderId: orderId,
            amount: amount
        };

        // 결제창 띄우기 //redirect방식
        await payment.requestPayment({
            method: "CARD", // 카드 결제
            amount: {
                currency: "KRW", // 화폐단위
                value: amount,   // 결제 금액
            },
            orderId: orderId,
            orderName: orderName,
            // 성공했을 때의 url
            successUrl: window.location.origin + "/payment/success",
            // 실패했을 때의 url
            failUrl: window.location.origin + "/payment/fail",
            customerEmail: customerEmail,
            customerName: customerName,
            // 카드 결제에 필요한 정보
            card: {
                useEscrow: false,    // 에스크로 적용 여부
                // DEFAULT - 카드/간편결제 통합결제창, DIRECT - 카드/간편결제 자체창
                flowMode: "DEFAULT",
                useCardPoint: false,  // 카드사 포인트 사용 여부의 default값 - False를 하면 체크 해제된 상태로 열림
                useAppCardOnly: false, // 앱카드 단독사용여부 -> true시 카드사의 앱카드만 열림 -국민,농협,우리 등등만 적용 가능
            },

        });
    } catch (e) {
        console.error("결제 요청 실패:", e);
        alert("결제 요청에 실패했습니다. 콘솔을 확인하세요.");
    }
}
