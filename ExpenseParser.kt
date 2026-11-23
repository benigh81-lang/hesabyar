package com.example.kharjyar.data

/** Conservative first-pass SMS parser. It flags likely purchase messages, not every bank event. */
object ExpenseParser {
    private val purchaseWords = listOf("خرید", "پرداخت", "purchase", "pos", "کارتخوان", "درگاه")
    private val transferWords = listOf("انتقال", "واریز", "رسید", "برداشت وجه", "transfer")
    private val amountRegex = Regex("(?:مبلغ|amount|خرید)\\s*[:：]?\\s*([0-9۰-۹٠-٩,٬]+)\\s*(?:ریال|تومان)?", RegexOption.IGNORE_CASE)

    data class Candidate(val amount: Long?, val isLikelyPurchase: Boolean, val needsReview: Boolean = true, val explanation: String)

    fun parse(text: String): Candidate {
        val normalized = normalizeDigits(text).replace("٬", ",")
        val isPurchase = purchaseWords.any { normalized.contains(it, true) }
        val transferOnly = transferWords.any { normalized.contains(it, true) } && !isPurchase
        val amount = amountRegex.find(normalized)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()
        return Candidate(amount, isPurchase && !transferOnly, true,
            when {
                transferOnly -> "احتمال انتقال/واریز؛ به‌عنوان هزینه ثبت نشود مگر تأیید شود"
                isPurchase -> "خرید احتمالی؛ نیازمند تأیید شما"
                else -> "پیام مالی احتمالی؛ بررسی دستی"
            })
    }

    private fun normalizeDigits(s: String): String = s.map { c ->
        when (c) {
            in '۰'..'۹' -> ('0'.code + (c.code - '۰'.code)).toChar()
            in '٠'..'٩' -> ('0'.code + (c.code - '٠'.code)).toChar()
            else -> c
        }
    }.joinToString("")
}
