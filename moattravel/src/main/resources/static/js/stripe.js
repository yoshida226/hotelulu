const stripe = Stripe('pk_test_51RCXMoQrrhKzyniq6Mr6r6ABOeb3cFj88M4cmYBxRs9Qmemkz85f2mbKZmOpf2ZW0OXpRlMyRG3b8GTLB3I5LVDT00gLLlyU5K');
const paymentButton = document.querySelector('#paymentButton');

paymentButton.addEventListener('click', () => {
	stripe.redirectToCheckout({
		sessionId: sessionId
	})
});